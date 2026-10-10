package app.minlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class AppSettingsStoreMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val boolKey = booleanPreferencesKey("FIRST_OPEN")
    private val intKey = intPreferencesKey("HOME_APPS_NUM")
    private val longKey = longPreferencesKey("FIRST_OPEN_TIME")
    private val floatKey = floatPreferencesKey("TEXT_SIZE_SCALE")
    private val stringKey = stringPreferencesKey("APP_NAME_1")
    private val stringSetKey = stringSetPreferencesKey("HIDDEN_APPS")
    private val dynamicKey = stringPreferencesKey("com.example.app")

    // The real LauncherApp warms up its own AppSettingsStore on the default
    // file in onCreate, so a test store must use its own file name to avoid
    // "multiple DataStores active for the same file".
    private fun newStore() = AppSettingsStore(context, dataStoreFileName = "migration-test")

    private fun seedLegacyPrefs() {
        context
            .getSharedPreferences(AppSettingsStore.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("FIRST_OPEN", false)
            .putInt("HOME_APPS_NUM", 3)
            .putLong("FIRST_OPEN_TIME", 42L)
            .putFloat("TEXT_SIZE_SCALE", 1.25f)
            .putString("APP_NAME_1", "Phone")
            .putStringSet("HIDDEN_APPS", setOf("com.hidden.app", "com.also.hidden"))
            .putString("com.example.app", "My Renamed App")
            .putBoolean(LockModePreservingMigration.LOCK_MODE, true)
            .commit()
    }

    @Test
    fun `full migration brings every legacy SharedPreferences value into DataStore`() {
        seedLegacyPrefs()
        val store = newStore()
        store.warmUp()

        assertEquals(false, store[boolKey])
        assertEquals(3, store[intKey])
        assertEquals(42L, store[longKey])
        assertEquals(1.25f, store[floatKey])
        assertEquals("Phone", store[stringKey])
        assertEquals(setOf("com.hidden.app", "com.also.hidden"), store[stringSetKey])
        assertEquals("My Renamed App", store[dynamicKey])
    }

    @Test
    fun `migrated keys are removed from the legacy SharedPreferences file`() {
        seedLegacyPrefs()
        val store = newStore()
        store.warmUp()

        val legacy = context.getSharedPreferences(AppSettingsStore.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        // Everything except LOCK_MODE is migrated away; LOCK_MODE survives for
        // the :serviceProcess write path and is NOT imported into the DataStore.
        assertEquals(setOf(LockModePreservingMigration.LOCK_MODE), legacy.all.keys)
        assertNull(store[booleanPreferencesKey(LockModePreservingMigration.LOCK_MODE)])
    }

    @Test
    fun `legacy keys survive migrate and are only removed by cleanUp`() {
        seedLegacyPrefs()
        // Drive the DataMigration lifecycle directly: this is the crash-window
        // contract that warmUp exercises only end-to-end.
        val migration = LockModePreservingMigration(context)
        val legacy = context.getSharedPreferences(AppSettingsStore.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

        assertTrue(runBlocking { migration.shouldMigrate(emptyPreferences()) })

        // migrate() imports values but must NOT delete them yet: if the
        // process died before the DataStore durably committed the result, the
        // next run has to be able to migrate again from the still-populated
        // legacy file instead of losing the values entirely.
        runBlocking { migration.migrate(emptyPreferences()) }
        val expectedLegacyKeys =
            listOf(
                "FIRST_OPEN",
                "HOME_APPS_NUM",
                "HIDDEN_APPS",
                "com.example.app",
                LockModePreservingMigration.LOCK_MODE,
            )
        assertTrue(legacy.all.keys.containsAll(expectedLegacyKeys))

        // cleanUp() runs after the DataStore commit: now the legacy keys go
        // away (LOCK_MODE survives) and a re-run finds nothing to migrate.
        runBlocking { migration.cleanUp() }
        assertEquals(setOf(LockModePreservingMigration.LOCK_MODE), legacy.all.keys)
        runBlocking { assertTrue(!migration.shouldMigrate(emptyPreferences())) }
    }

    @Test
    fun `never-set keys read as null when no legacy file exists`() {
        // Robolectric gives each test method a fresh files dir, so there is no
        // legacy file; clear explicitly anyway to keep the intent obvious.
        context
            .getSharedPreferences(AppSettingsStore.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val store = newStore()
        store.warmUp()

        assertNull(store[boolKey])
        assertNull(store[stringSetKey])
        assertNull(store[dynamicKey])
    }

    @Test
    fun `set updates the snapshot immediately and persists asynchronously`() {
        val store = newStore()
        store.warmUp()

        store.set(stringKey, "Camera")

        // Optimistic snapshot is readable synchronously.
        assertEquals("Camera", store[stringKey])

        // The async edit lands on the DataStore file; poll the authoritative
        // DataStore read until it reflects the write (a single data.first()
        // right after set() can race the in-flight edit, and the optimistic
        // snapshot is not proof of persistence).
        runBlocking {
            withTimeout(5_000) {
                while (store.dataStore.data.first()[stringKey] != "Camera") {
                    delay(10)
                }
            }
            // The durable read now agrees with the optimistic snapshot.
            assertEquals("Camera", store.dataStore.data.first()[stringKey])
        }
    }

    @Test
    fun `stringSet values are unmodifiable on read`() {
        seedLegacyPrefs()
        val store = newStore()
        store.warmUp()

        val read = store[stringSetKey]
        assertEquals(setOf("com.hidden.app", "com.also.hidden"), read)

        // DataStore hands back an unmodifiable view so callers cannot corrupt
        // its in-memory state; document that by asserting the mutation throws.
        assertThrows(UnsupportedOperationException::class.java) {
            (read as MutableSet<String>).add("com.extra.app")
        }
    }

    @Test
    fun `corrupted DataStore file degrades to defaults instead of crashing`() {
        // 0x0f bytes are an invalid protobuf wire type (field 1, wire type 7),
        // so parsing fails and the store detects corruption.
        val file = context.filesDir.resolve("datastore/migration-test.preferences_pb")
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(8) { 0x0f })

        val store = newStore()
        store.warmUp() // must not throw

        // The corruption handler replaced the file with empty preferences.
        assertNull(store[boolKey])

        // The store stays usable: writes still go through and persist.
        store.set(boolKey, false)
        runBlocking {
            withTimeout(5_000) {
                while (store.dataStore.data.first()[boolKey] != false) {
                    delay(10)
                }
            }
        }
    }
}
