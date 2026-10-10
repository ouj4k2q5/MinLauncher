package app.minlauncher

import android.app.Application
import app.minlauncher.data.LauncherRepository
import app.minlauncher.helper.RealAppListProvider

/**
 * Single shared repository for the launcher screens. MainViewModel and
 * DrawerViewModel both delegate to [launcherRepository], so an app list loaded
 * through one screen reaches the others. TestMainActivity and JVM tests replace
 * the repository before any ViewModel is created.
 *
 * The repository is NOT created in a property initializer: the Application
 * constructor runs before attachBaseContext, so Context use must be deferred
 * to onCreate. Hence lateinit + onCreate, not a field initializer.
 */
class LauncherApp : Application() {
    lateinit var launcherRepository: LauncherRepository

    override fun onCreate() {
        super.onCreate()
        launcherRepository = LauncherRepository(this, RealAppListProvider)
    }
}
