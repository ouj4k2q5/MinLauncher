package app.minlauncher.testing

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import app.minlauncher.LauncherApp
import app.minlauncher.R
import app.minlauncher.data.LauncherRepository
import app.minlauncher.data.Prefs
import app.minlauncher.helper.AppListProvider
import app.minlauncher.helper.RealAppListProvider

/**
 * Debug-only host activity for JVM UI tests. It mirrors just enough of
 * [app.minlauncher.MainActivity] for the real fragments to run: the real
 * navigation graph and the launcher's app-scoped [LauncherRepository] whose
 * [app.minlauncher.helper.AppListProvider] is swappable from tests through
 * [testAppListProvider]. The repository is replaced before any ViewModel is
 * created, so every fragment observes the test's fake world.
 *
 * The extra repository priming calls cover the hidden-apps drawer, which in
 * production is fed by SettingsFragment.
 */
class TestMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val launcherRepository = LauncherRepository(application, testAppListProvider ?: RealAppListProvider)
        (application as LauncherApp).launcherRepository = launcherRepository
        AppCompatDelegate.setDefaultNightMode(Prefs(this).appTheme)
        super.onCreate(savedInstanceState)
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, NavHostFragment.create(R.navigation.nav_graph))
                .commitNow()
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
