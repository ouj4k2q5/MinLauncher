// One small function per home action/observer (the same layout as the old
// Fragment's callbacks, split by side effect); the count is inherent to binding
// the home screen, not a sign of a file doing too many things.
@file:Suppress("TooManyFunctions")

package app.minlauncher.ui.home

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import android.view.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavBackStack
import app.minlauncher.HomeViewModel
import app.minlauncher.R
import app.minlauncher.data.Constants
import app.minlauncher.data.LauncherRepository
import app.minlauncher.data.Prefs
import app.minlauncher.helper.LockRequest
import app.minlauncher.helper.appUsagePermissionGranted
import app.minlauncher.helper.expandNotificationDrawer
import app.minlauncher.helper.openCalendar
import app.minlauncher.helper.openClock
import app.minlauncher.helper.showToast
import app.minlauncher.ui.nav.DrawerKey
import app.minlauncher.ui.nav.HomeKey
import app.minlauncher.ui.nav.MinLauncherNavKey
import app.minlauncher.ui.nav.SettingsKey

private const val TAG = "HomeEntry"

/**
 * The home screen's Navigation 3 entry. Rebuilds the fragment's runtime: an
 * entry-scoped [HomeViewModel] (cleared only when the entry leaves the stack,
 * so the permanent home screen's state survives drawer/settings visits), the
 * six observers as collectors, and the onResume work twice — once per
 * composition (returning from the drawer disposes and recreates the entry) and
 * once from the ACTIVITY lifecycle, because the entry-scoped
 * `LocalLifecycleOwner`'s identity is not guaranteed.
 *
 * The composition-time initialization below is deliberately synchronous: it
 * mutates ViewModel state before [HomeScreenHost] reads it, so the first drawn
 * frame already has the slots. SideEffect/LaunchedEffect run after composition
 * and would let the first frame draw empty (the decor-view screenshot capture
 * does not wait for an effect), so the remember-returns-Unit pattern is
 * intentional here rather than the misuse RememberReturnType warns about.
 */
@Suppress("RememberReturnType")
@Composable
internal fun HomeEntry(
    repository: LauncherRepository,
    backStack: NavBackStack<MinLauncherNavKey>,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val application = context.applicationContext as Application
    val prefs = remember(context) { Prefs(context) }
    val viewModel: HomeViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer { HomeViewModel(application, repository) }
                },
        )
    val colors = remember(activity) { activity.resolveHomeColors() }
    val view = LocalView.current

    // Runs once per entry composition. Deliberately synchronous: the old
    // fragment populated the ViewModel in onResume, before the first draw, and
    // the screenshot tests capture the decor view without waiting for an
    // effect. Returning from the drawer recreates the entry, so this runs
    // again just like the effect would.
    remember(viewModel) {
        initializeHome(context, activity, prefs, viewModel)
    }
    initHomeObservers(context, prefs, viewModel)
    // The entry-scoped LocalLifecycleOwner is not relied upon; the activity's
    // lifecycle is the documented resume source, gated on home being on top.
    DisposableEffect(activity, backStack) {
        val activityLifecycle = (activity as LifecycleOwner).lifecycle
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && backStack.lastOrNull() is HomeKey) {
                    populateHomeScreen(context, prefs, viewModel)
                    viewModel.isMinLauncherDefault()
                    applyHomeStatusBar(activity, prefs)
                }
            }
        activityLifecycle.addObserver(observer)
        onDispose { activityLifecycle.removeObserver(observer) }
    }

    val callbacks =
        remember(viewModel, prefs, backStack, view) {
            createHomeCallbacks(context, prefs, viewModel, backStack, view)
        }
    HomeScreenHost(viewModel, colors, callbacks)
}

private fun initializeHome(
    context: Context,
    activity: Activity,
    prefs: Prefs,
    viewModel: HomeViewModel,
) {
    viewModel.showFirstRunTips = prefs.firstSettingsOpen
    setHomeAlignment(viewModel)
    populateHomeScreen(context, prefs, viewModel)
    viewModel.isMinLauncherDefault()
    applyHomeStatusBar(activity, prefs)
}

@Composable
private fun initHomeObservers(
    context: Context,
    prefs: Prefs,
    viewModel: HomeViewModel,
) {
    LaunchedEffect(viewModel) {
        viewModel.refreshHome.collect {
            populateHomeScreen(context, prefs, viewModel)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.isMinLauncherDefault.collect { isDefault ->
            if (isDefault == false) {
                prefs.homeBottomAlignment = false
                setHomeAlignment(viewModel)
            }
            if (viewModel.showFirstRunTips) return@collect
            viewModel.showSetDefaultLauncher = isDefault == false && prefs.hideSetDefaultLauncher.not()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.homeAppAlignmentFlow.collect { setHomeAlignment(viewModel, it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.homeBottomAlignmentFlow.collect { setHomeAlignment(viewModel) }
    }
    LaunchedEffect(viewModel) {
        viewModel.toggleDateTime.collect { populateDateTime(prefs, viewModel) }
    }
    LaunchedEffect(viewModel) {
        viewModel.screenTimeValue.collect { value -> value?.let { viewModel.screenTimeText = it } }
    }
}

private fun setHomeAlignment(
    viewModel: HomeViewModel,
    horizontalGravity: Int = viewModel.homeAppAlignmentFlow.value,
) {
    viewModel.homeAppAlignment = horizontalGravity
    viewModel.homeBottomAlignment = viewModel.homeBottomAlignmentFlow.value
}

private fun populateDateTime(
    prefs: Prefs,
    viewModel: HomeViewModel,
) {
    viewModel.dateTimeVisibility = prefs.dateTimeVisibility
    // Gates the battery suffix on the date line; re-read on every populate.
    viewModel.showStatusBar = prefs.showStatusBar
}

private fun populateScreenTime(
    context: Context,
    prefs: Prefs,
    viewModel: HomeViewModel,
) {
    if (context.appUsagePermissionGranted().not() || prefs.screenTimeEnabled.not()) {
        viewModel.screenTimeText = null
        return
    }
    viewModel.getTodaysScreenTime()
}

private fun populateHomeScreen(
    context: Context,
    prefs: Prefs,
    viewModel: HomeViewModel,
) {
    populateDateTime(prefs, viewModel)
    populateScreenTime(context, prefs, viewModel)
    viewModel.slots =
        (1..prefs.homeAppsNum).map { location ->
            HomeSlotState(
                index = location,
                label = verifiedHomeAppLabel(context, prefs, location),
            )
        }
}

private fun applyHomeStatusBar(
    activity: Activity,
    prefs: Prefs,
) {
    if (prefs.showStatusBar) {
        activity.window.insetsController?.show(WindowInsets.Type.statusBars())
    } else {
        activity.window.insetsController?.hide(WindowInsets.Type.statusBars())
    }
}

private fun showAppList(
    viewModel: HomeViewModel,
    backStack: NavBackStack<MinLauncherNavKey>,
    flag: Int,
    rename: Boolean = false,
    includeHiddenApps: Boolean = false,
) {
    viewModel.getAppList(includeHiddenApps)
    backStack.add(DrawerKey(flag, rename))
}

private fun createHomeCallbacks(
    context: Context,
    prefs: Prefs,
    viewModel: HomeViewModel,
    backStack: NavBackStack<MinLauncherNavKey>,
    view: View,
): HomeCallbacks =
    HomeCallbacks(
        onSwipeLeft = { openSwipeLeftApp(context, prefs, viewModel) },
        onSwipeRight = { openSwipeRightApp(context, prefs, viewModel) },
        onSwipeUp = { showAppList(viewModel, backStack, Constants.FLAG_LAUNCH_APP) },
        onSwipeDown = { expandNotificationDrawer(context) },
        onBackgroundLongPress = { backStack.add(SettingsKey) },
        onBackgroundDoubleTap = {
            if (prefs.lockModeOn) {
                // Preserve the system click feedback the old lock tap played.
                view.playSoundEffect(SoundEffectConstants.CLICK)
                LockRequest.send(context)
            }
        },
        onSlotTap = { location -> homeAppClicked(context, prefs, viewModel, location) },
        // The slot number doubles as Constants.FLAG_SET_HOME_APP_n (1..8).
        onSlotLongPress = { location ->
            showAppList(
                viewModel,
                backStack,
                location,
                rename = prefs.getAppName(location).isNotEmpty(),
                includeHiddenApps = true,
            )
        },
        onClockTap = { openClock(context) },
        onDateTap = { openCalendar(context) },
        onScreenTimeTap = { openScreenTimeDigitalWellbeing(context) },
        onSetDefaultLauncherTap = { viewModel.requestResetLauncher() },
        onSetDefaultLauncherLongPress = {
            prefs.hideSetDefaultLauncher = true
            viewModel.showSetDefaultLauncher = false
            if (viewModel.isMinLauncherDefault.value != true) {
                context.showToast(R.string.set_as_default_launcher)
                backStack.add(SettingsKey)
            }
        },
    )

// The Digital Wellbeing intents can fail with anything the platform chooses
// (ActivityNotFound, Security, component resolution); both fallbacks must run.
@Suppress("TooGenericExceptionCaught")
private fun openScreenTimeDigitalWellbeing(context: Context) {
    val intent = Intent()
    try {
        intent.setClassName(
            Constants.DIGITAL_WELLBEING_PACKAGE_NAME,
            Constants.DIGITAL_WELLBEING_ACTIVITY,
        )
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open Digital Wellbeing app", e)
        try {
            intent.setClassName(
                Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME,
                Constants.DIGITAL_WELLBEING_SAMSUNG_ACTIVITY,
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Samsung Digital Wellbeing app", e)
        }
    }
}
