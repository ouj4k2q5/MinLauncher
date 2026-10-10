package app.minlauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import app.minlauncher.data.AppModel
import app.minlauncher.data.LauncherRepository

/**
 * Screen adapter for the home screen; all shared state and selection logic live
 * in [LauncherRepository], which is app-scoped so the home and drawer screens
 * observe the same data.
 *
 * Constructed only through the explicit factory in HomeFragment, which reads
 * the current activity's application (under Robolectric the default factory's
 * Application fallback can be stale across tests).
 */
class HomeViewModel(
    application: Application,
    private val repository: LauncherRepository,
) : AndroidViewModel(application) {
    val refreshHome get() = repository.refreshHome
    val isMinLauncherDefault get() = repository.isMinLauncherDefault
    val homeAppAlignment get() = repository.homeAppAlignment
    val toggleDateTime get() = repository.toggleDateTime
    val screenTimeValue get() = repository.screenTimeValue

    fun isMinLauncherDefault() = repository.isMinLauncherDefault()

    fun selectedApp(
        appModel: AppModel,
        flag: Int,
    ) = repository.selectedApp(appModel, flag)

    fun getAppList(includeHiddenApps: Boolean = false) = repository.getAppList(includeHiddenApps)

    fun requestResetLauncher() = repository.requestResetLauncher()

    fun getTodaysScreenTime() = repository.getTodaysScreenTime()
}
