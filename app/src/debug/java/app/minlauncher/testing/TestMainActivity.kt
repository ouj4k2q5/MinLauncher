package app.minlauncher.testing

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.NavHostFragment
import app.minlauncher.MainViewModel
import app.minlauncher.R
import app.minlauncher.data.Prefs
import app.minlauncher.helper.AppListProvider
import app.minlauncher.helper.RealAppListProvider

/**
 * Debug-only host activity for JVM UI tests. It mirrors just enough of
 * [app.minlauncher.MainActivity] for the real fragments to run: the real
 * navigation graph and a [MainViewModel] whose [app.minlauncher.helper.AppListProvider]
 * is swappable from tests through [testAppListProvider].
 *
 * The extra [MainViewModel.getHiddenApps] call covers the hidden-apps drawer
 * which in production is fed by SettingsFragment.
 */
class TestMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(Prefs(this).appTheme)
        super.onCreate(savedInstanceState)
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, NavHostFragment.create(R.navigation.nav_graph))
                .commitNow()
        }
        ViewModelProvider(this)[MainViewModel::class.java].apply {
            getAppList()
            getHiddenApps()
        }
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() =
            viewModelFactory {
                initializer {
                    MainViewModel(
                        this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!,
                        testAppListProvider ?: RealAppListProvider,
                    )
                }
            }

    companion object {
        /**
         * Set by tests before launching this activity; falls back to the real
         * provider so nothing special happens outside UI tests.
         */
        var testAppListProvider: AppListProvider? = null
    }
}
