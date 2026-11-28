package app.minlauncher.ui.settings

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Picker currently expanded under a settings row. At most one is open at a time. */
enum class SettingsPicker {
    NONE,
    APPS_NUM,
    DATE_TIME,
    ALIGNMENT,
    APP_THEME,
    TEXT_SIZE,
}

/**
 * Theme attributes resolved once by SettingsEntry from the activity theme (so the
 * day/night variant is the one the activity is currently using). Compose must
 * not hardcode the palette or extend MinLauncherTheme's M3 color scheme with it.
 */
@Suppress("LongParameterList") // a palette bag; splitting it would only move the count
@Immutable
data class SettingsColors(
    val primaryColor: Int,
    val shadeDark: Int,
    val colorTrans50: Int,
    val inverse: Int,
    val inverseTrans80: Int,
    val inverseTrans90: Int,
    val dialogShade: Int,
    val textShadow: Int,
)

/** Everything the settings screen renders. Derived in SettingsEntry, not imperative populate calls. */
@Suppress("LongParameterList") // one flat snapshot of the screen's inputs
@Immutable
data class SettingsUiState(
    val homeAppsNum: Int,
    val dateTimeVisibility: Int,
    val homeAlignment: Int,
    val homeBottomAlignment: Boolean,
    val statusBarOn: Boolean,
    val screenTimeOn: Boolean,
    val isDefaultLauncher: Boolean,
    val autoShowKeyboard: Boolean,
    val solidWallpaper: Boolean,
    val showThemeRow: Boolean,
    val appTheme: Int,
    val boldFont: Boolean,
    val textSizeLabel: String,
    val swipeLeftAppName: String,
    val swipeRightAppName: String,
    val swipeLeftEnabled: Boolean,
    val swipeRightEnabled: Boolean,
    val lockModeOn: Boolean,
    val accessibilityServiceEnabled: Boolean,
    val accessibilityOverlayVisible: Boolean,
)

/**
 * Side effects live in SettingsEntry: recreations, dialogs, navigation, toasts,
 * status-bar insets, the lock-mode SharedPreferences write and app-info intents.
 */
@Suppress("LongParameterList") // a callback bag; each entry is one distinct screen side effect
@Immutable
data class SettingsCallbacks(
    val onOpenHiddenApps: () -> Unit,
    val onOpenAppInfo: () -> Unit,
    val onRequestResetLauncher: () -> Unit,
    val onToggleScreenTime: () -> Unit,
    val onSelectHomeAppsNum: (Int) -> Unit,
    val onSelectDateTime: (Int) -> Unit,
    val onSelectAlignment: (Int) -> Unit,
    val onToggleBottomAlignment: () -> Unit,
    val onLongPressAlignment: () -> Unit,
    val onToggleStatusBar: () -> Unit,
    val onToggleAutoShowKeyboard: () -> Unit,
    val onToggleSolidWallpaper: () -> Unit,
    val onLongPressSolidWallpaper: () -> Unit,
    val onSelectTheme: (Int) -> Unit,
    val onToggleBoldFont: () -> Unit,
    val onAdjustTextSize: (Float) -> Unit,
    val onFinalizeTextSize: () -> Unit,
    val onSwipeAppClick: (Boolean) -> Unit,
    val onSwipeAppLongPress: (Boolean) -> Unit,
    val onToggleLockMode: () -> Unit,
    val onLongPressLockMode: () -> Unit,
    val onOpenAccessibilitySettings: () -> Unit,
    val onCloseAccessibility: () -> Unit,
)

/** Label/value pair of one settings row, plus the test tag attached to its click target. */
@Immutable
internal data class SettingRowContent(
    val label: String,
    val value: String,
    val tag: String,
)

/** A row whose value is replaced by an inline picker while [target] is open. */
@Immutable
internal data class PickerRowSpec(
    val content: SettingRowContent,
    val target: SettingsPicker,
    val onLongClick: (() -> Unit)? = null,
)

/** Optional row decorations: a dimmed value for disabled swipe targets and a long-press action. */
@Immutable
internal data class ActionRowStyle(
    val valueColor: Color? = null,
    val onLongClick: (() -> Unit)? = null,
)

/**
 * Single-open-picker semantics: opening one closes the others, and leaving the
 * text-size picker finalizes the pending preview. Mirrors the old onClick().
 */
@Immutable
internal data class SettingsPickerController(
    val current: SettingsPicker,
    val open: (SettingsPicker) -> Unit,
    val close: () -> Unit,
)

/** Portrait hides the "System" theme option until the row is long-pressed; landscape always shows it. */
@Immutable
internal data class SystemThemeOption(
    val revealed: Boolean,
    val onReveal: () -> Unit,
)

@Immutable
internal data class SettingsLayout(
    val isLandscape: Boolean,
    val systemThemeOption: SystemThemeOption,
)
