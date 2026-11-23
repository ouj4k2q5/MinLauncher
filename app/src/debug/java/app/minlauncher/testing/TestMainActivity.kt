package app.minlauncher.testing

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import app.minlauncher.LauncherApp
import app.minlauncher.R
import app.minlauncher.data.LauncherRepository
import app.minlauncher.data.Prefs
import app.minlauncher.helper.AppListProvider
import app.minlauncher.helper.RealAppListProvider
import app.minlauncher.helper.applySolidWallpaper
import kotlinx.coroutines.launch

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
        val prefs = Prefs(this)
        AppCompatDelegate.setDefaultNightMode(prefs.appTheme)
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply { id = R.id.mainActivityLayout }
        val navHostContainer = FrameLayout(this).apply { id = R.id.nav_host_fragment }
        root.addView(navHostContainer, fullSizeLayoutParams())
        setContentView(root)
        root.applySolidWallpaper(prefs.solidWallpaper)
        if (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.nav_host_fragment, NavHostFragment.create(R.navigation.nav_graph))
                .commitNow()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launcherRepository.solidWallpaper.collect {
                    root.applySolidWallpaper(it)
                }
            }
        }
        launcherRepository.getAppList()
        launcherRepository.getHiddenApps()
    }

    private fun fullSizeLayoutParams() =
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

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
