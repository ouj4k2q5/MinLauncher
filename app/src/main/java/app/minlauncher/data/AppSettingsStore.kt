package app.minlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

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
    }

    val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            migrations = listOf(LockModePreservingMigration(context)),
            produceFile = { context.filesDir.resolve("datastore/$dataStoreFileName.preferences_pb") },
        )

    // Guards [snapshot] against the optimistic-write/committed-state race:
    // set()/remove() update the snapshot and count the in-flight edit
    // atomically, and the collector replaces the snapshot only when the count
    // is zero (no edit still pending, so the committed state is complete).
    private val snapshotLock = Any()
    private val inFlightEdits = AtomicInteger(0)

    @Volatile
    var snapshot: Preferences = emptyPreferences()
        private set

    init {
        scope.launch {
            dataStore.data.collect { committed ->
                synchronized(snapshotLock) {
                    if (inFlightEdits.get() == 0) snapshot = committed
                }
            }
        }
    }

    /**
     * Blocks the calling thread once to run migrations and seed [snapshot].
     * Call exactly once from Application.onCreate; every later access is
     * non-blocking via [snapshot]. Triggers the one-time
     * [LockModePreservingMigration] (every legacy key except LOCK_MODE, which
     * stays on SharedPreferences for the :serviceProcess write path).
     */
    fun warmUp() {
        runBlocking { snapshot = dataStore.data.first() }
    }

    /** Synchronous read of the cached snapshot. */
    operator fun <T> get(key: Preferences.Key<T>): T? = snapshot[key]

    /**
     * Optimistic synchronous cache update + asynchronous persistent edit.
     * Single-key edits only; the collector re-syncs the snapshot from the
     * authoritative file afterwards.
     */
    fun <T> set(
        key: Preferences.Key<T>,
        value: T,
    ) {
        synchronized(snapshotLock) {
            snapshot = snapshot.toMutablePreferences().apply { set(key, value) }.toPreferences()
            inFlightEdits.incrementAndGet()
        }
        scope.launch {
            try {
                dataStore.edit { it[key] = value }
            } finally {
                inFlightEdits.decrementAndGet()
            }
        }
    }

    /**
     * Optimistic synchronous cache removal + asynchronous persistent edit.
     * DataStore has no null values, so removing a key is the way a nullable
     * preference is cleared (mirrors the old putString(key, null) behavior).
     */
    fun <T> remove(key: Preferences.Key<T>) {
        synchronized(snapshotLock) {
            snapshot = snapshot.toMutablePreferences().apply { remove(key) }.toPreferences()
            inFlightEdits.incrementAndGet()
        }
        scope.launch {
            try {
                dataStore.edit { it.remove(key) }
            } finally {
                inFlightEdits.decrementAndGet()
            }
        }
    }
}
