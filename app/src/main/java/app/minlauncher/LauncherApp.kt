package app.minlauncher

import android.app.Application
import app.minlauncher.data.AppSettingsStore
import app.minlauncher.data.LauncherRepository
import app.minlauncher.helper.RealAppListProvider

/**
 * Single shared repository for the launcher screens. MainActivity and the
 * screen ViewModels (HomeViewModel, DrawerViewModel, SettingsViewModel) all
 * delegate to [launcherRepository], so an app list loaded through one screen
 * reaches the others. TestMainActivity and JVM tests replace the repository
 * before any ViewModel is created.
 *
 * The repository is NOT created in a property initializer: the Application
 * constructor runs before attachBaseContext, so Context use must be deferred
 * to onCreate. Hence lateinit + onCreate, not a field initializer.
 *
 * Only the main process creates the repository and warms up the DataStore:
 * the accessibility service's `:serviceProcess` must never instantiate the
 * DataStore (one instance per file per process) — it writes lockModeOn
 * directly through SharedPreferences instead.
 */
class LauncherApp : Application() {
    lateinit var launcherRepository: LauncherRepository

    /** App-scoped DataStore owner; per-Application so Robolectric gets one per test. */
    val appSettings by lazy { AppSettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        // The accessibility service's :serviceProcess must never instantiate
        // the DataStore (one instance per file per process) or the repository.
        // Robolectric's getProcessName() may differ; treat any process name
        // without ':' as the main process (production main == packageName).
        val processName = getProcessName()
        if (processName == null || !processName.contains(':')) {
            appSettings.warmUp()
            launcherRepository = LauncherRepository(this, RealAppListProvider)
        }
    }
}
