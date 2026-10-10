package app.minlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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
            .commit()
    }

    @Test
    fun `full migration brings every legacy SharedPreferences value into DataStore`() {
        seedLegacyPrefs()
        val store = AppSettingsStore(context)
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
        val store = AppSettingsStore(context)
        store.warmUp()

        val legacy = context.getSharedPreferences(AppSettingsStore.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        assertTrue("migrated keys should be cleaned up from SharedPreferences", legacy.all.isEmpty())
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
        val store = AppSettingsStore(context)
        store.warmUp()

        assertNull(store[boolKey])
        assertNull(store[stringSetKey])
        assertNull(store[dynamicKey])
    }

    @Test
    fun `set updates the snapshot immediately and persists asynchronously`() {
        val store = AppSettingsStore(context)
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
        val store = AppSettingsStore(context)
        store.warmUp()

        val read = store[stringSetKey]
        assertEquals(setOf("com.hidden.app", "com.also.hidden"), read)

        // DataStore hands back an unmodifiable view so callers cannot corrupt
        // its in-memory state; document that by asserting the mutation throws.
        assertThrows(UnsupportedOperationException::class.java) {
            (read as MutableSet<String>).add("com.extra.app")
        }
    }
}
