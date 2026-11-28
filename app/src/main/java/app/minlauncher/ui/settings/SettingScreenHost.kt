package app.minlauncher.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minlauncher.SettingsViewModel

/**
 * The settings screen's host: adapts the [SettingsViewModel] state and the
 * resolved colors/callbacks into [SettingsScreen]. The app theme comes from the
 * single [app.minlauncher.ui.AppRoot] wrapper (Stage 2), so this host no longer
 * wraps its content in [app.minlauncher.ui.theme.MinLauncherTheme].
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
