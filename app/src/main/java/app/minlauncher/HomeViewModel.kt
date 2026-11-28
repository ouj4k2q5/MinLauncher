package app.minlauncher

import android.app.Application
import android.view.Gravity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.data.LauncherRepository
import app.minlauncher.ui.home.HomeSlotState

/**
 * Screen adapter for the home screen; all shared state and selection logic live
 * in [LauncherRepository], which is app-scoped so the home and drawer screens
 * observe the same data.
 *
 * The home screen is the launcher's permanent screen, so its renderable state
 * lives here rather than in the Fragment. The properties are Compose state so
 * the host composable recomposes when a collector writes them.
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
    val toggleDateTime get() = repository.toggleDateTime
    val screenTimeValue get() = repository.screenTimeValue

    // The repository-backed alignment *settings*; distinct from the renderable
    // state below, which is what the screen draws.
    val homeAppAlignmentFlow get() = repository.homeAppAlignment
    val homeBottomAlignmentFlow get() = repository.homeBottomAlignment

    // The renderable home screen. Initial values match the old Fragment fields;
    // the Fragment's collectors populate them.
    var slots by mutableStateOf<List<HomeSlotState>>(emptyList())
    var homeAppAlignment by mutableIntStateOf(Gravity.START)
    var homeBottomAlignment by mutableStateOf(false)
    var dateTimeVisibility by mutableIntStateOf(Constants.DateTime.ON)
    var showStatusBar by mutableStateOf(false)
    var showFirstRunTips by mutableStateOf(false)
    var showSetDefaultLauncher by mutableStateOf(false)
    var screenTimeText by mutableStateOf<String?>(null)

    fun isMinLauncherDefault() = repository.isMinLauncherDefault()

    fun selectedApp(
        appModel: AppModel,
        flag: Int,
    ) = repository.selectedApp(appModel, flag)

    fun getAppList(includeHiddenApps: Boolean = false) = repository.getAppList(includeHiddenApps)

    fun requestResetLauncher() = repository.requestResetLauncher()

    fun getTodaysScreenTime() = repository.getTodaysScreenTime()
}
