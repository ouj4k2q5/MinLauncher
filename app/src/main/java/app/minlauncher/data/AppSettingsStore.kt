package app.minlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
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
 */
class AppSettingsStore(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    dataStoreFileName: String = DATASTORE_FILE,
) {
    companion object {
        /** Must equal Prefs' PREFS_FILENAME so SharedPreferencesMigration reads the old file. */
        const val LEGACY_PREFS_NAME = "app.minlauncher"

        private const val DATASTORE_FILE = "app.minlauncher"
    }

    val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            migrations = listOf(SharedPreferencesMigration(context, LEGACY_PREFS_NAME)),
            produceFile = { context.filesDir.resolve("datastore/$dataStoreFileName.preferences_pb") },
        )

    @Volatile
    var snapshot: Preferences = emptyPreferences()
        private set

    init {
        scope.launch { dataStore.data.collect { snapshot = it } }
    }

    /**
     * Blocks the calling thread once to run migrations and seed [snapshot].
     * Call exactly once from Application.onCreate; every later access is
     * non-blocking via [snapshot]. Triggers the one-time SharedPreferences
     * migration (default MIGRATE_ALL_KEYS: every key incl. dynamic
     * package-name keys, types boolean/int/long/float/string/stringSet).
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
        snapshot = snapshot.toMutablePreferences().apply { set(key, value) }.toPreferences()
        scope.launch { dataStore.edit { it[key] = value } }
    }
}
