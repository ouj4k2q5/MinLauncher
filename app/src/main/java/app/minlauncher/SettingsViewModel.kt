package app.minlauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import app.minlauncher.data.LauncherRepository

/**
 * Screen adapter for the settings screen; all shared state and selection logic
 * live in [LauncherRepository], which is app-scoped so the settings, home and
 * drawer screens observe the same data.
 *
 * Constructed only through the explicit factory in SettingsFragment, which
 * reads the current activity's application (under Robolectric the default
 * factory's Application fallback can be stale across tests).
 */
class SettingsViewModel(
    application: Application,
    private val repository: LauncherRepository,
) : AndroidViewModel(application) {
    val isMinLauncherDefault get() = repository.isMinLauncherDefault
    val homeAppAlignment get() = repository.homeAppAlignment
    val updateSwipeApps get() = repository.updateSwipeApps
    val resetLauncherLiveData get() = repository.resetLauncherLiveData

    fun isMinLauncherDefault() = repository.isMinLauncherDefault()

    fun updateHomeAlignment(gravity: Int) = repository.updateHomeAlignment(gravity)

    fun toggleDateTime() = repository.toggleDateTime()

    fun getHiddenApps() = repository.getHiddenApps()

    fun refreshHome(appCountUpdated: Boolean) = repository.refreshHome(appCountUpdated)

    fun getAppList(includeHiddenApps: Boolean = false) = repository.getAppList(includeHiddenApps)

    fun postDialog(dialog: String) = repository.showDialog.postValue(dialog)
}
