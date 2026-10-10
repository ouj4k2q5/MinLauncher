package app.minlauncher

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import app.minlauncher.data.AppModel
import app.minlauncher.data.LauncherRepository
import app.minlauncher.helper.showToast

/**
 * Screen adapter for the app drawer; all shared state and selection logic live
 * in [LauncherRepository], which is app-scoped so the drawer and the home
 * screens observe the same data.
 *
 * Constructed only through the explicit factory in AppDrawerFragment, which
 * reads the current app's repository (under Robolectric the default factory's
 * Application fallback can be stale across tests).
 */
class DrawerViewModel(
    application: Application,
    private val repository: LauncherRepository,
) : AndroidViewModel(application) {
    private val appContext by lazy { application.applicationContext }

    val appList get() = repository.appList
    val hiddenApps get() = repository.hiddenApps
    val privateSpaceApps get() = repository.privateSpaceApps
    val privateSpaceLocked get() = repository.privateSpaceLocked
    val privateSpaceAvailable get() = repository.privateSpaceAvailable
    val showDialog get() = repository.showDialog

    fun selectedApp(
        appModel: AppModel,
        flag: Int,
    ) = repository.selectedApp(appModel, flag)

    fun getAppList(includeHiddenApps: Boolean = false) = repository.getAppList(includeHiddenApps)

    fun getHiddenApps() = repository.getHiddenApps()

    fun getPrivateSpaceAppList() = repository.getPrivateSpaceAppList()

    fun togglePrivateSpaceLock() = repository.togglePrivateSpaceLock()

    fun openPrivateSpaceSettings() {
        try {
            val intent = Intent("android.settings.PRIVATE_SPACE_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
            } catch (_: Exception) {
                appContext.showToast(appContext.getString(R.string.unable_to_open_app))
            }
        }
    }
}
