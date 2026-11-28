package app.minlauncher

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import app.minlauncher.data.LauncherRepository

/**
 * Screen adapter for the settings screen; all shared state and selection logic
 * live in [LauncherRepository], which is app-scoped so the settings, home and
 * drawer screens observe the same data.
 *
 * Settings is not torn down by opening the drawer on top of it, so its
 * renderable state lives here rather than in the Fragment. The properties are
 * Compose state so the host composable recomposes when a collector writes them.
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
    val homeBottomAlignment get() = repository.homeBottomAlignment
    val dateTimeVisibility get() = repository.dateTimeVisibility
    val showStatusBar get() = repository.showStatusBar
    val homeAppsNum get() = repository.homeAppsNum
    val screenTimeEnabled get() = repository.screenTimeEnabled
    val textSizeScale get() = repository.textSizeScale
    val boldFont get() = repository.boldFont
    val solidWallpaper get() = repository.solidWallpaper
    val swipeLeftEnabled get() = repository.swipeLeftEnabled
    val swipeRightEnabled get() = repository.swipeRightEnabled
    val updateSwipeApps get() = repository.updateSwipeApps

    // The renderable settings screen. Initial values match the old Fragment
    // fields; the Fragment's view setup and collectors populate them.
    var pendingTextSizeScale by mutableFloatStateOf(-1f)
    var appTheme by mutableIntStateOf(AppCompatDelegate.MODE_NIGHT_YES)
    var autoShowKeyboard by mutableStateOf(true)
    var usagePermissionGranted by mutableStateOf(false)
    var accessibilityOverlayVisible by mutableStateOf(false)
    var accessibilityServiceEnabled by mutableStateOf(false)
    var lockModeOn by mutableStateOf(false)
    var swipeLeftAppName by mutableStateOf("")
    var swipeRightAppName by mutableStateOf("")
    var einkDisplay by mutableStateOf(false)

    fun isMinLauncherDefault() = repository.isMinLauncherDefault()

    fun updateHomeAlignment(gravity: Int) = repository.updateHomeAlignment(gravity)

    fun toggleDateTime() = repository.toggleDateTime()

    fun getHiddenApps() = repository.getHiddenApps()

    fun refreshHome(appCountUpdated: Boolean) = repository.refreshHome(appCountUpdated)

    fun getAppList(includeHiddenApps: Boolean = false) = repository.getAppList(includeHiddenApps)

    fun postDialog(dialog: String) = repository.postDialog(dialog)

    fun requestResetLauncher() = repository.requestResetLauncher()
}
