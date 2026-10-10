package app.minlauncher.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import app.minlauncher.LauncherApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class PrefsDataStoreFacadeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Test-specific DataStore file name so this store never collides with the
    // default-file store the (real) Application will own after Stage 3.
    private fun newStore() = AppSettingsStore(context, dataStoreFileName = "prefs-facade-test")

    private fun seedLegacyPrefs() {
        context
            .getSharedPreferences(AppSettingsStore.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("FIRST_OPEN", false)
            .putInt("HOME_APPS_NUM", 3)
            .putFloat("TEXT_SIZE_SCALE", 1.25f)
            .putString("APP_NAME_1", "Phone")
            .putStringSet("HIDDEN_APPS", setOf("a", "b"))
            .putInt("APP_THEME", 2)
            .putBoolean("LOCK_MODE", true)
            .commit()
    }

    @Test
    fun `Prefs reads values migrated from legacy SharedPreferences through the injected DataStore`() {
        seedLegacyPrefs()
        val store = newStore()
        store.warmUp()
        val prefs = Prefs(context, store)

        assertEquals(false, prefs.firstOpen)
        assertEquals(3, prefs.homeAppsNum)
        assertEquals(1.25f, prefs.textSizeScale)
        assertEquals("Phone", prefs.appName1)
        assertEquals(setOf("a", "b"), prefs.hiddenApps)
        assertEquals(2, prefs.appTheme)
        assertEquals(true, prefs.lockModeOn)
    }

    @Test
    fun `Prefs getters read the injected DataStore, not SharedPreferences`() {
        val store = newStore()
        val prefs = Prefs(context, store)

        // Seed the DataStore directly, not the legacy SharedPreferences file.
        store.set(stringPreferencesKey("APP_NAME_1"), "FromDataStore")
        store.set(intPreferencesKey("HOME_APPS_NUM"), 7)
        store.set(booleanPreferencesKey("FIRST_OPEN"), false)

        assertEquals("FromDataStore", prefs.appName1)
        assertEquals(7, prefs.homeAppsNum)
        assertEquals(false, prefs.firstOpen)
    }

    @Test
    fun `Prefs setters persist into the injected DataStore`() {
        val store = newStore()
        // Warm up first: without it, the store's one-time migration could run
        // after the SharedPreferences write below and migrate the value into
        // the DataStore, making this test falsely green today.
        store.warmUp()
        val prefs = Prefs(context, store)

        prefs.appName1 = "Camera"

        // Poll the authoritative DataStore read; a single first() can race
        // the in-flight edit (see AppSettingsStoreMigrationTest).
        runBlocking {
            withTimeout(5_000) {
                while (store.dataStore.data.first()[stringPreferencesKey("APP_NAME_1")] != "Camera") {
                    delay(10)
                }
            }
        }
    }

    @Test
    fun `lockModeOn writes stay in SharedPreferences`() {
        val store = newStore()
        // Warm up first so the one-time migration runs before this test's own
        // SharedPreferences write, mirroring production where the Application
        // warms up before any Prefs access. (LOCK_MODE itself can never
        // migrate away: migrate() skips it and cleanUp() only removes keys
        // that were actually imported.)
        store.warmUp()
        val prefs = Prefs(context, store)

        prefs.lockModeOn = true

        val legacy = context.getSharedPreferences(AppSettingsStore.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        assertTrue("LOCK_MODE must stay in SharedPreferences", legacy.getBoolean("LOCK_MODE", false))
    }

    @Test
    fun `defaults are unchanged when nothing is set`() {
        val store = newStore()
        val prefs = Prefs(context, store)

        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, prefs.appTheme)
        assertEquals(4, prefs.homeAppsNum)
        assertEquals(true, prefs.firstOpen)
        assertEquals(0L, prefs.firstOpenTime)
        assertEquals(1.0f, prefs.textSizeScale)
        assertEquals("", prefs.appName1)
        assertTrue(prefs.hiddenApps.isEmpty())
        assertEquals(false, prefs.lockModeOn)
    }

    @Test
    fun `appActivityClassName null removes the key from the DataStore`() {
        val store = newStore()
        store.warmUp()
        val prefs = Prefs(context, store)

        prefs.appActivityClassName1 = "com.example/.MainActivity"
        prefs.appActivityClassName1 = null

        // Getter falls back to "" once the key is gone.
        assertEquals("", prefs.appActivityClassName1)

        // The durable state drops the key too (DataStore has no null values,
        // so clearing means removing).
        runBlocking {
            withTimeout(5_000) {
                val activityKey = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_1")
                while (store.dataStore.data
                        .first()
                        .contains(activityKey)
                ) {
                    delay(10)
                }
            }
        }
    }

    @Test
    fun `dynamic rename labels round-trip through the facade`() {
        val store = newStore()
        store.warmUp()
        val prefs = Prefs(context, store)

        prefs.setAppRenameLabel("com.example.app", "My Renamed App")
        assertEquals("My Renamed App", prefs.getAppRenameLabel("com.example.app"))
        assertEquals("", prefs.getAppRenameLabel("com.other.app"))
    }

    @Test
    fun `Prefs without an injected store resolves the Application's AppSettingsStore`() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val appStore = (app as LauncherApp).appSettings

        appStore.set(stringPreferencesKey("APP_NAME_1"), "FromAppStore")

        // Same store instance: the facade immediately sees the write.
        assertEquals("FromAppStore", Prefs(app).appName1)
    }
}
