package app.minlauncher.data

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

/**
 * App-scoped owner of the Preferences DataStore that backs [Prefs].
 *
 * Deliberately owned by the Application instance (not a static delegate and
 * not a top-level `preferencesDataStore` property): Robolectric creates a
 * fresh Application per test but reuses classloaders, so any static singleton
 * would go stale across tests. Holding the store on the Application gives
 * production one instance per process and each test its own instance with
 * its own files dir.
 *
 * Synchronous access pattern: [snapshot] is a @Volatile cache of the latest
 * Preferences, seeded by [warmUp] (called once from Application.onCreate,
 * before any Activity's attachBaseContext) and kept fresh by a background
 * collector. Writes update the snapshot optimistically and persist
 * asynchronously via edit.
 *
 * The collector only lets the committed DataStore state replace [snapshot]
 * when no optimistic edit is still in flight: DataStore commits edits one at
 * a time, so mid-burst emissions lag the optimistic snapshot and must not
 * clobber newer local writes.
 *
 * A DataStore failure must never crash the launcher (it would crash-loop on
 * every start, and this app is the home screen): a corrupted file is replaced
 * with empty preferences via [ReplaceFileCorruptionHandler], [warmUp]
 * degrades to defaults instead of throwing inside Application.onCreate, and
 * the collector and async edits log and recover instead of dying.
 */
class AppSettingsStore(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    dataStoreFileName: String = DATASTORE_FILE,
) {
    companion object {
        /** Must equal Prefs' PREFS_FILENAME so LockModePreservingMigration reads the old file. */
        const val LEGACY_PREFS_NAME = "app.minlauncher"

        private const val DATASTORE_FILE = "app.minlauncher"
        private const val TAG = "AppSettingsStore"
        private const val COLLECT_RETRY_DELAY_MS = 1_000L
    }

    val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = listOf(LockModePreservingMigration(context)),
            produceFile = { context.filesDir.resolve("datastore/$dataStoreFileName.preferences_pb") },
        )

    // Guards [snapshot] against the optimistic-write/committed-state race:
    // set()/remove() update the snapshot and count the in-flight edit
    // atomically, and the collector replaces the snapshot only when the count
    // is zero (no edit still pending, so the committed state is complete).
    private val snapshotLock = Any()
    private val inFlightEdits = AtomicInteger(0)

    // Test seam: how many committed emissions arrived while an optimistic
    // edit was still in flight and were therefore withheld from [snapshot].
    // Pinned by AppSettingsStoreConcurrencyTest — if this stays zero across a
    // same-key write burst, the suppression guard has stopped working.
    @VisibleForTesting
    internal val suppressedEmissions = AtomicInteger(0)

    // FIFO queue drained by a single consumer coroutine: edits reach the
    // DataStore (which serializes edits by arrival order) in the same order
    // their optimistic snapshot updates were applied under [snapshotLock], so
    // a same-key double write can never persist the older value last. A plain
    // scope.launch per write would not guarantee this — dispatch order on a
    // thread pool is not FIFO.
    private val editQueue = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    @Volatile
    var snapshot: Preferences = emptyPreferences()
        private set

    init {
        scope.launch { drainEditQueue() }
        scope.launch {
            while (true) {
                try {
                    dataStore.data.collect { committed ->
                        synchronized(snapshotLock) {
                            if (inFlightEdits.get() == 0) {
                                snapshot = committed
                            } else {
                                suppressedEmissions.incrementAndGet()
                            }
                        }
                    }
                    return@launch
                } catch (e: IOException) {
                    // Without the retry loop the collector would die on the
                    // first read failure and freeze the snapshot forever.
                    Log.e(TAG, "Failed to read committed settings; retrying", e)
                    delay(COLLECT_RETRY_DELAY_MS)
                }
            }
        }
    }

    /**
     * Applies queued edits strictly FIFO (see [editQueue]). Any single failing
     * op is logged and skipped: the consumer must survive it, or the queue
     * would stop draining, [inFlightEdits] would never reach zero, and the
     * collector would be suppressed forever.
     */
    @Suppress("TooGenericExceptionCaught") // deliberate blast-radius containment for the queue consumer
    private suspend fun drainEditQueue() {
        for (pendingEdit in editQueue) {
            try {
                pendingEdit()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Persistence failed: the optimistic snapshot may diverge
                // from disk until the next successful write, but the
                // launcher must not crash over a settings write.
                Log.e(TAG, "Failed to persist settings change", e)
            }
        }
    }

    /**
     * Blocks the calling thread once to run migrations and seed [snapshot].
     * Call exactly once from Application.onCreate; every later access is
     * non-blocking via [snapshot]. Triggers the one-time
     * [LockModePreservingMigration] (every legacy key except LOCK_MODE, which
     * stays on SharedPreferences for the :serviceProcess write path).
     *
     * Never throws: an unreadable settings file degrades to empty preferences
     * (logged) so the launcher keeps starting instead of crash-looping.
     */
    fun warmUp() {
        snapshot =
            try {
                runBlocking { dataStore.data.first() }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to read settings at startup; using defaults", e)
                emptyPreferences()
            }
    }

    /** Synchronous read of the cached snapshot. */
    operator fun <T> get(key: Preferences.Key<T>): T? = snapshot[key]

    /**
     * Optimistic synchronous cache update + asynchronous persistent edit.
     * Single-key edits only; the collector re-syncs the snapshot from the
     * authoritative file afterwards. The edit is enqueued while holding
     * [snapshotLock], so the durable write order always matches the snapshot
     * update order.
     */
    fun <T> set(
        key: Preferences.Key<T>,
        value: T,
    ) {
        synchronized(snapshotLock) {
            snapshot = snapshot.toMutablePreferences().apply { set(key, value) }.toPreferences()
            inFlightEdits.incrementAndGet()
            editQueue.trySend {
                try {
                    dataStore.edit { it[key] = value }
                } finally {
                    inFlightEdits.decrementAndGet()
                }
            }
        }
    }

    /**
     * Optimistic synchronous cache removal + asynchronous persistent edit.
     * DataStore has no null values, so removing a key is the way a nullable
     * preference is cleared (mirrors the old putString(key, null) behavior).
     * The edit is enqueued while holding [snapshotLock]; see [set].
     */
    fun <T> remove(key: Preferences.Key<T>) {
        synchronized(snapshotLock) {
            snapshot = snapshot.toMutablePreferences().apply { remove(key) }.toPreferences()
            inFlightEdits.incrementAndGet()
            editQueue.trySend {
                try {
                    dataStore.edit { it.remove(key) }
                } finally {
                    inFlightEdits.decrementAndGet()
                }
            }
        }
    }
}
