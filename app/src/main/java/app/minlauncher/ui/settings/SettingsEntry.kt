package app.minlauncher.ui.settings

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavBackStack
import app.minlauncher.R
import app.minlauncher.SettingsViewModel
import app.minlauncher.data.Constants
import app.minlauncher.data.LauncherRepository
import app.minlauncher.data.Prefs
import app.minlauncher.helper.appUsagePermissionGranted
import app.minlauncher.helper.isEinkDisplay
import app.minlauncher.helper.showToast
import app.minlauncher.ui.nav.DrawerKey
import app.minlauncher.ui.nav.MinLauncherNavKey

/**
 * The settings screen's Navigation 3 entry. Rebuilds the fragment's setup: the
 * entry-scoped [SettingsViewModel] (retained while settings stays on the stack,
 * which is what keeps the text-size preview across a drawer visit), the
 * onCreateView initialization and onViewCreated work as effects, and the
 * navigation callbacks as back-stack lambdas.
 *
 * The host is wrapped in the old `ComposeView`'s [resolveSettingsColors]
 * `shadeDark` background: [SettingsScreen] has no background of its own, and
 * the old screen painted this at the View level.
 */
@Composable
internal fun SettingsEntry(
    repository: LauncherRepository,
    backStack: NavBackStack<MinLauncherNavKey>,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val application = context.applicationContext as Application
    val prefs = remember(context) { Prefs(context) }
    val viewModel: SettingsViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer { SettingsViewModel(application, repository) }
                },
        )
    val colors = remember(activity) { activity.resolveSettingsColors() }

    LaunchedEffect(viewModel) {
        initializeSettings(context, activity, prefs, viewModel)
        viewModel.updateSwipeApps.collect {
            refreshSwipeApps(context, prefs, viewModel)
        }
    }

    val callbacks =
        remember(prefs, viewModel, backStack) {
            createSettingsCallbacks(context, activity, prefs, viewModel, backStack)
        }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(colors.shadeDark)),
    ) {
        SettingsScreenHost(viewModel, colors, callbacks)
    }
}

private fun initializeSettings(
    context: Context,
    activity: Activity,
    prefs: Prefs,
    viewModel: SettingsViewModel,
) {
    viewModel.appTheme = prefs.appTheme
    viewModel.autoShowKeyboard = prefs.autoShowKeyboard
    viewModel.accessibilityOverlayVisible = false
    viewModel.usagePermissionGranted = context.appUsagePermissionGranted()
    viewModel.einkDisplay = context.isEinkDisplay()
    populateLockSettings(context, prefs, viewModel)
    refreshSwipeApps(context, prefs, viewModel)
    viewModel.isMinLauncherDefault()
    applyStatusBarInsets(activity, prefs.showStatusBar)
    if (prefs.firstSettingsOpen) {
        viewModel.postDialog(Constants.Dialog.ABOUT)
        prefs.firstSettingsOpen = false
    }
}

private fun createSettingsCallbacks(
    context: Context,
    activity: Activity,
    prefs: Prefs,
    viewModel: SettingsViewModel,
    backStack: NavBackStack<MinLauncherNavKey>,
): SettingsCallbacks =
    SettingsCallbacks(
        onOpenHiddenApps = { showHiddenApps(context, prefs, viewModel, backStack) },
        onOpenAppInfo = { openAppInfo(context) },
        onRequestResetLauncher = { viewModel.requestResetLauncher() },
        onToggleScreenTime = { toggleScreenTime(context, prefs, viewModel) },
        onSelectHomeAppsNum = { updateHomeAppsNum(prefs, viewModel, it) },
        onSelectDateTime = { toggleDateTime(prefs, viewModel, it) },
        onSelectAlignment = { viewModel.updateHomeAlignment(it) },
        onToggleBottomAlignment = { updateHomeBottomAlignment(context, prefs, viewModel) },
        onLongPressAlignment = { handleAlignmentLongClick(context, prefs, backStack) },
        onToggleStatusBar = { toggleStatusBar(activity, prefs) },
        onToggleAutoShowKeyboard = { toggleKeyboardText(prefs, viewModel) },
        onToggleSolidWallpaper = { toggleSolidWallpaper(prefs) },
        onLongPressSolidWallpaper = { removeWallpaper(activity, prefs, viewModel) },
        onSelectTheme = { updateTheme(activity, prefs, viewModel, it) },
        onToggleBoldFont = { toggleBoldFont(activity, prefs) },
        onAdjustTextSize = { adjustTextSizePreview(context, prefs, viewModel, it) },
        // Keep the nullable-activity contract: a late callback with no host
        // activity still commits the scale and clears the pending preview.
        onFinalizeTextSize = { applyTextSizeScale(activity, prefs, viewModel) },
        onSwipeAppClick = { isLeft ->
            showAppListIfEnabled(
                context,
                prefs,
                viewModel,
                backStack,
                if (isLeft) Constants.FLAG_SET_SWIPE_LEFT_APP else Constants.FLAG_SET_SWIPE_RIGHT_APP,
            )
        },
        onSwipeAppLongPress = { isLeft -> toggleSwipeApp(context, prefs, isLeft) },
        onToggleLockMode = { toggleLockMode(context, prefs, viewModel) },
        onLongPressLockMode = { openAccessibilitySettings(activity) },
        onOpenAccessibilitySettings = { openAccessibilityService(activity, prefs, viewModel) },
        onCloseAccessibility = { viewModel.accessibilityOverlayVisible = false },
    )

private fun showHiddenApps(
    context: Context,
    prefs: Prefs,
    viewModel: SettingsViewModel,
    backStack: NavBackStack<MinLauncherNavKey>,
) {
    if (prefs.hiddenApps.isEmpty()) {
        context.showToast(R.string.no_hidden_apps)
        return
    }
    viewModel.getHiddenApps()
    backStack.add(DrawerKey(Constants.FLAG_HIDDEN_APPS, false))
}

private fun showAppListIfEnabled(
    context: Context,
    prefs: Prefs,
    viewModel: SettingsViewModel,
    backStack: NavBackStack<MinLauncherNavKey>,
    flag: Int,
) {
    if ((flag == Constants.FLAG_SET_SWIPE_LEFT_APP) and !prefs.swipeLeftEnabled) {
        context.showToast(R.string.long_press_to_enable)
        return
    }
    if ((flag == Constants.FLAG_SET_SWIPE_RIGHT_APP) and !prefs.swipeRightEnabled) {
        context.showToast(R.string.long_press_to_enable)
        return
    }
    viewModel.getAppList(true)
    backStack.add(DrawerKey(flag, false))
}

private fun handleAlignmentLongClick(
    context: Context,
    prefs: Prefs,
    backStack: NavBackStack<MinLauncherNavKey>,
) {
    prefs.appLabelAlignment = prefs.homeAlignment
    backStack.add(DrawerKey(Constants.FLAG_LAUNCH_APP, false))
    context.showToast(R.string.alignment_changed)
}

private fun toggleSwipeApp(
    context: Context,
    prefs: Prefs,
    isLeft: Boolean,
) {
    if (isLeft) {
        prefs.swipeLeftEnabled = !prefs.swipeLeftEnabled
        context.showToast(
            if (prefs.swipeLeftEnabled) R.string.swipe_left_app_enabled else R.string.swipe_left_app_disabled,
        )
    } else {
        prefs.swipeRightEnabled = !prefs.swipeRightEnabled
        context.showToast(
            if (prefs.swipeRightEnabled) R.string.swipe_right_app_enabled else R.string.swipe_right_app_disabled,
        )
    }
}
