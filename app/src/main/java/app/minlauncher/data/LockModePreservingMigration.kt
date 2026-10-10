package app.minlauncher.data

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * One-time migration of the legacy "app.minlauncher" SharedPreferences into
 * the Preferences DataStore.
 *
 * Unlike the stock [androidx.datastore.preferences.SharedPreferencesMigration],
 * this deliberately keeps [LOCK_MODE] in the SharedPreferences file: the
 * accessibility service runs in `:serviceProcess`, which must not instantiate
 * the DataStore (one DataStore instance per file per process), so it reads and
 * writes lockModeOn directly through SharedPreferences.
 *
 * Removal happens in [cleanUp], after DataStore has durably committed the
 * migrated values: a crash between [migrate] and [cleanUp] leaves both the
 * legacy file and the DataStore populated, so the next run's [shouldMigrate]
 * is still true and the migration re-runs idempotently. Only the keys
 * [migrate] actually imported are removed, so LOCK_MODE and unsupported
 * values survive in the legacy file.
 */
class LockModePreservingMigration(
    context: Context,
) : DataMigration<Preferences> {
    companion object {
        /** Kept on SharedPreferences: MyAccessibilityService writes it cross-process. */
        const val LOCK_MODE = "LOCK_MODE"
    }

    private val sharedPrefs =
        context.getSharedPreferences(AppSettingsStore.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

    // Keys imported into the DataStore by the last [migrate] run; cleanUp()
    // removes exactly these from the legacy file.
    private var migratedKeys: List<String> = emptyList()

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = sharedPrefs.all.keys.any { it != LOCK_MODE }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val migrated = currentData.toMutablePreferences()
        val keysToRemove = mutableListOf<String>()
        sharedPrefs.all.forEach { (key, value) ->
            if (key == LOCK_MODE) return@forEach
            when (value) {
                is Boolean -> migrated[booleanPreferencesKey(key)] = value
                is Int -> migrated[intPreferencesKey(key)] = value
                is Long -> migrated[longPreferencesKey(key)] = value
                is Float -> migrated[floatPreferencesKey(key)] = value
                is String -> migrated[stringPreferencesKey(key)] = value
                is Set<*> ->
                    if (value.all { it is String }) {
                        migrated[stringSetPreferencesKey(key)] = value as Set<String>
                    } else {
                        return@forEach
                    }
                else -> return@forEach
            }
            keysToRemove.add(key)
        }
        migratedKeys = keysToRemove
        return migrated.toPreferences()
    }

    override suspend fun cleanUp() {
        // Runs only after DataStore durably committed the values migrate()
        // returned, so removing the legacy keys here is loss-free: if the
        // process died before this point, both copies are still populated
        // and the next run re-migrates. See class KDoc.
        if (migratedKeys.isNotEmpty()) {
            sharedPrefs.edit(commit = true) { migratedKeys.forEach { remove(it) } }
            migratedKeys = emptyList()
        }
    }
}
