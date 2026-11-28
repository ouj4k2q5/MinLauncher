package app.minlauncher.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minlauncher.SettingsViewModel
import app.minlauncher.ui.theme.MinLauncherTheme

/**
 * The settings screen's host: adapts the [SettingsViewModel] state and the
 * resolved colors/callbacks into [SettingsScreen]. Moved out of the Fragment
 * (Stage 1 of the fragment removal) so the UI no longer depends on the
 * fragment's view state; the fragment still owns the scoped view model factory,
 * its observers, the permission flows and navigation until Stage 2.
 */
@Composable
internal fun SettingsScreenHost(
    viewModel: SettingsViewModel,
    colors: SettingsColors,
    callbacks: SettingsCallbacks,
) {
    val homeAppsNum by viewModel.homeAppsNum.collectAsStateWithLifecycle()
    val dateTimeVisibility by viewModel.dateTimeVisibility.collectAsStateWithLifecycle()
    val homeAlignment by viewModel.homeAppAlignment.collectAsStateWithLifecycle()
    val homeBottomAlignment by viewModel.homeBottomAlignment.collectAsStateWithLifecycle()
    val statusBarOn by viewModel.showStatusBar.collectAsStateWithLifecycle()
    val screenTimeEnabled by viewModel.screenTimeEnabled.collectAsStateWithLifecycle()
    val textSizeScale by viewModel.textSizeScale.collectAsStateWithLifecycle()
    val boldFont by viewModel.boldFont.collectAsStateWithLifecycle()
    val solidWallpaper by viewModel.solidWallpaper.collectAsStateWithLifecycle()
    val swipeLeftEnabled by viewModel.swipeLeftEnabled.collectAsStateWithLifecycle()
    val swipeRightEnabled by viewModel.swipeRightEnabled.collectAsStateWithLifecycle()
    val isDefaultLauncher by viewModel.isMinLauncherDefault.collectAsStateWithLifecycle()
    val displayedTextSize = if (viewModel.pendingTextSizeScale > 0f) viewModel.pendingTextSizeScale else textSizeScale

    MinLauncherTheme(appTheme = viewModel.appTheme) {
        SettingsScreen(
            state =
                SettingsUiState(
                    homeAppsNum = homeAppsNum,
                    dateTimeVisibility = dateTimeVisibility,
                    homeAlignment = homeAlignment,
                    homeBottomAlignment = homeBottomAlignment,
                    statusBarOn = statusBarOn,
                    screenTimeOn = viewModel.usagePermissionGranted && screenTimeEnabled,
                    isDefaultLauncher = isDefaultLauncher == true,
                    autoShowKeyboard = viewModel.autoShowKeyboard,
                    solidWallpaper = solidWallpaper,
                    showThemeRow = !viewModel.einkDisplay,
                    appTheme = viewModel.appTheme,
                    boldFont = boldFont,
                    textSizeLabel = "%.1f".format(displayedTextSize),
                    swipeLeftAppName = viewModel.swipeLeftAppName,
                    swipeRightAppName = viewModel.swipeRightAppName,
                    swipeLeftEnabled = swipeLeftEnabled,
                    swipeRightEnabled = swipeRightEnabled,
                    lockModeOn = viewModel.lockModeOn,
                    accessibilityServiceEnabled = viewModel.accessibilityServiceEnabled,
                    accessibilityOverlayVisible = viewModel.accessibilityOverlayVisible,
                ),
            colors = colors,
            callbacks = callbacks,
        )
    }
}
