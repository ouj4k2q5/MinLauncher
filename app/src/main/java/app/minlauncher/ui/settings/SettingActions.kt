// One small function per settings action (the same layout as the old onClick
// switch, split by side effect); the count is inherent to binding a dense
// settings screen, not a sign of a file doing too many things.
//
// These are the settings screen's non-navigation actions, moved out of the
// old fragment (Stage 2 of the fragment removal) so the settings entry
// composable can run the same actions without a fragment. They are the
// fragment's originals with its `requireContext()`/`requireActivity()`/
// `activity`/`getString()` calls and its `prefs`/`viewModel` fields turned into
// explicit parameters; the bodies are otherwise unchanged.
@file:Suppress("TooManyFunctions")

package app.minlauncher.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import app.minlauncher.BuildConfig
import app.minlauncher.R
import app.minlauncher.SettingsViewModel
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.helper.appUsagePermissionGranted
import app.minlauncher.helper.isAccessServiceEnabled
import app.minlauncher.helper.isTablet
import app.minlauncher.helper.showToast
import kotlin.math.roundToInt
import app.minlauncher.helper.openAppInfo as openAppInfoHelper

/**
 * The settings screen's actions continue below; see the file header for the
 * extraction rationale.
 */

internal fun toggleScreenTime(
    context: Context,
    prefs: Prefs,
    viewModel: SettingsViewModel,
) {
    viewModel.usagePermissionGranted = context.appUsagePermissionGranted()
    if (!viewModel.usagePermissionGranted) {
        viewModel.postDialog(Constants.Dialog.DIGITAL_WELLBEING)
        return
    }
    prefs.screenTimeEnabled = !prefs.screenTimeEnabled
}

internal fun updateHomeAppsNum(
    prefs: Prefs,
    viewModel: SettingsViewModel,
    num: Int,
) {
    prefs.homeAppsNum = num
    viewModel.refreshHome(true)
}

internal fun toggleDateTime(
    prefs: Prefs,
    viewModel: SettingsViewModel,
    selected: Int,
) {
    prefs.dateTimeVisibility = selected
    viewModel.toggleDateTime()
}

internal fun updateHomeBottomAlignment(
    context: Context,
    prefs: Prefs,
    viewModel: SettingsViewModel,
) {
    if (viewModel.isMinLauncherDefault.value != true) {
        context.showToast(context.getString(R.string.please_set_minlauncher_as_default_first), Toast.LENGTH_LONG)
        return
    }
    prefs.homeBottomAlignment = !prefs.homeBottomAlignment
    viewModel.updateHomeAlignment(prefs.homeAlignment)
}

internal fun toggleStatusBar(
    activity: Activity,
    prefs: Prefs,
) {
    prefs.showStatusBar = !prefs.showStatusBar
    applyStatusBarInsets(activity, prefs.showStatusBar)
}

internal fun applyStatusBarInsets(
    activity: Activity,
    show: Boolean,
) {
    if (show) {
        activity.window.insetsController?.show(WindowInsets.Type.statusBars())
    } else {
        activity.window.insetsController?.hide(WindowInsets.Type.statusBars())
    }
}

internal fun toggleKeyboardText(
    prefs: Prefs,
    viewModel: SettingsViewModel,
) {
    if (prefs.autoShowKeyboard && prefs.keyboardMessageShown.not()) {
        viewModel.postDialog(Constants.Dialog.KEYBOARD)
        prefs.keyboardMessageShown = true
    } else {
        prefs.autoShowKeyboard = !prefs.autoShowKeyboard
        viewModel.autoShowKeyboard = prefs.autoShowKeyboard
    }
}

internal fun toggleSolidWallpaper(prefs: Prefs) {
    prefs.solidWallpaper = !prefs.solidWallpaper
}

/**
 * Long press on the wallpaper row: force the theme to one the solid wallpaper suits
 * and switch the toggle off. The activity is recreated so the forced theme
 * (and the transparent background) apply immediately, not at the next
 * unrelated recreation.
 */
internal fun removeWallpaper(
    activity: Activity,
    prefs: Prefs,
    viewModel: SettingsViewModel,
) {
    prefs.appTheme =
        if (viewModel.einkDisplay) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
    prefs.solidWallpaper = false
    activity.recreate()
}

internal fun updateTheme(
    activity: Activity,
    prefs: Prefs,
    viewModel: SettingsViewModel,
    theme: Int,
) {
    if (AppCompatDelegate.getDefaultNightMode() == theme) return
    prefs.appTheme = theme
    viewModel.appTheme = theme
    activity.recreate()
}

internal fun toggleBoldFont(
    activity: Activity,
    prefs: Prefs,
) {
    prefs.boldFont = !prefs.boldFont
    activity.recreate()
}

internal fun adjustTextSizePreview(
    context: Context,
    prefs: Prefs,
    viewModel: SettingsViewModel,
    delta: Float,
) {
    val maxScale = if (isTablet(context)) 2.0f else 1.5f
    val current = if (viewModel.pendingTextSizeScale > 0) viewModel.pendingTextSizeScale else prefs.textSizeScale
    val newScale = ((current + delta) * 10f).roundToInt() / 10f
    val clamped = newScale.coerceIn(0.5f, maxScale)
    if (clamped == current) return
    viewModel.pendingTextSizeScale = clamped
}

// The nullable activity mirrors the original's `activity ?: return`: a late
// callback with no host activity skips the recreation but still commits the
// scale and clears the pending preview.
internal fun applyTextSizeScale(
    activity: Activity?,
    prefs: Prefs,
    viewModel: SettingsViewModel,
) {
    if (viewModel.pendingTextSizeScale < 0 || prefs.textSizeScale == viewModel.pendingTextSizeScale) {
        viewModel.pendingTextSizeScale = -1f
        return
    }
    prefs.textSizeScale = viewModel.pendingTextSizeScale
    viewModel.pendingTextSizeScale = -1f
    if (activity == null) return
    if (activity.isChangingConfigurations.not()) {
        activity.recreate()
    }
}

internal fun toggleLockMode(
    context: Context,
    prefs: Prefs,
    viewModel: SettingsViewModel,
) {
    viewModel.accessibilityServiceEnabled = isAccessServiceEnabled(context)
    if (!prefs.lockModeOn && !viewModel.accessibilityServiceEnabled) {
        viewModel.accessibilityOverlayVisible = true
        return
    }
    prefs.lockModeOn = !prefs.lockModeOn
    populateLockSettings(context, prefs, viewModel)
}

internal fun openAccessibilityService(
    activity: Activity,
    prefs: Prefs,
    viewModel: SettingsViewModel,
) {
    viewModel.accessibilityOverlayVisible = false
    populateLockSettings(activity, prefs, viewModel)
    activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}

/** Long-press on the lock row: jump straight to the system accessibility settings. */
internal fun openAccessibilitySettings(activity: Activity) {
    activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}

internal fun openAppInfo(context: Context) {
    openAppInfoHelper(context, Process.myUserHandle(), BuildConfig.APPLICATION_ID)
}

internal fun populateLockSettings(
    context: Context,
    prefs: Prefs,
    viewModel: SettingsViewModel,
) {
    viewModel.accessibilityServiceEnabled = isAccessServiceEnabled(context)
    viewModel.lockModeOn = prefs.lockModeOn && viewModel.accessibilityServiceEnabled
}

internal fun refreshSwipeApps(
    context: Context,
    prefs: Prefs,
    viewModel: SettingsViewModel,
) {
    viewModel.swipeLeftAppName = prefs.appNameSwipeLeft.ifEmpty { context.getString(R.string.swipe_left_app_default) }
    viewModel.swipeRightAppName =
        prefs.appNameSwipeRight.ifEmpty { context.getString(R.string.swipe_right_app_default) }
}
