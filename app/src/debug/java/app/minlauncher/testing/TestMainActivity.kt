package app.minlauncher.testing

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import app.minlauncher.LauncherApp
import app.minlauncher.data.LauncherRepository
import app.minlauncher.data.Prefs
import app.minlauncher.helper.AppListProvider
import app.minlauncher.helper.RealAppListProvider
import app.minlauncher.ui.AppRoot
import app.minlauncher.ui.AppRootHandle

/**
 * Debug-only host activity for JVM UI tests. It mirrors just enough of
 * [app.minlauncher.MainActivity] for the real screens to run: the Navigation 3
 * [AppRoot] and the launcher's app-scoped [LauncherRepository] whose
 * [app.minlauncher.helper.AppListProvider] is swappable from tests through
 * [testAppListProvider]. The repository is replaced before any ViewModel is
 * created, so every entry observes the test's fake world.
 *
 * [handle] is the published back-stack handle; tests navigate by mutating it
 * (see UiTestHelpers) until the on-device gesture tests take over.
 */
class TestMainActivity : AppCompatActivity() {
    var handle: AppRootHandle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val launcherRepository = LauncherRepository(application, testAppListProvider ?: RealAppListProvider)
        (application as LauncherApp).launcherRepository = launcherRepository
        val prefs = Prefs(this)
        AppCompatDelegate.setDefaultNightMode(prefs.appTheme)
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot(
                repository = launcherRepository,
                appTheme = prefs.appTheme,
                onHandleReady = { handle = it },
            )
        }
        launcherRepository.getAppList()
        launcherRepository.getHiddenApps()
    }

    companion object {
        /**
         * Set by tests before launching this activity; falls back to the real
         * provider so nothing special happens outside UI tests. UI tests clear
         * it in an @After because Robolectric reuses the sandbox classloader,
         * so the static would otherwise leak into later tests.
         */
        var testAppListProvider: AppListProvider? = null
    }
}
