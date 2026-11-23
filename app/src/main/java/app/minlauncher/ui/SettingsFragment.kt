package app.minlauncher.ui

import android.content.Intent
import android.content.res.TypedArray
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import app.minlauncher.BuildConfig
import app.minlauncher.LauncherApp
import app.minlauncher.R
import app.minlauncher.SettingsViewModel
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.helper.appUsagePermissionGranted
import app.minlauncher.helper.isAccessServiceEnabled
import app.minlauncher.helper.isEinkDisplay
import app.minlauncher.helper.isTablet
import app.minlauncher.helper.openAppInfo
import app.minlauncher.helper.showToast
import app.minlauncher.ui.settings.SettingsCallbacks
import app.minlauncher.ui.settings.SettingsColors
import app.minlauncher.ui.settings.SettingsScreen
import app.minlauncher.ui.settings.SettingsUiState
import app.minlauncher.ui.theme.MinLauncherTheme
import kotlin.math.roundToInt

// One small method per settings action (the same layout as the old onClick
// switch, split by side effect); the count is inherent to binding a dense
// settings screen to the Fragment, not a sign of a class doing too many things.
@Suppress("TooManyFunctions")
class SettingsFragment : BaseFragment() {
    private lateinit var prefs: Prefs

    // Explicit factory: under Robolectric the default factory's Application
    // fallback can be stale across tests, so read the repository from the
    // current activity's app. In production the activity's app is the one
    // and only LauncherApp instance.
    private val viewModel: SettingsViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application
                SettingsViewModel(app, (app as LauncherApp).launcherRepository)
            }
        }
    }

    // Compose state hoisted here: the pending text-size preview, the theme
    // (only changes through Activity.recreate), and the values that have no
    // repository mirror (auto-show keyboard, lock mode, swipe app labels).
    private var pendingTextSizeScale by mutableFloatStateOf(-1f)
    private var appTheme by mutableIntStateOf(AppCompatDelegate.MODE_NIGHT_YES)
    private var autoShowKeyboard by mutableStateOf(true)
    private var usagePermissionGranted by mutableStateOf(false)
    private var accessibilityOverlayVisible by mutableStateOf(false)
    private var accessibilityServiceEnabled by mutableStateOf(false)
    private var lockModeOn by mutableStateOf(false)
    private var swipeLeftAppName by mutableStateOf("")
    private var swipeRightAppName by mutableStateOf("")
    private var einkDisplay by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        prefs = Prefs(requireContext())
        appTheme = prefs.appTheme
        autoShowKeyboard = prefs.autoShowKeyboard
        pendingTextSizeScale = -1f
        accessibilityOverlayVisible = false
        usagePermissionGranted = requireContext().appUsagePermissionGranted()
        einkDisplay = requireContext().isEinkDisplay()
        populateLockSettings()
        refreshSwipeApps()

        val colors = resolveSettingsColors()
        val callbacks = createCallbacks()

        return ComposeView(requireContext()).apply {
            id = R.id.mainActivityLayout
            setBackgroundColor(colors.shadeDark)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { SettingsScreenHost(colors, callbacks) }
        }
    }

    @Composable
    private fun SettingsScreenHost(
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
        val displayedTextSize = if (pendingTextSizeScale > 0f) pendingTextSizeScale else textSizeScale

        MinLauncherTheme(appTheme = appTheme) {
            SettingsScreen(
                state =
                    SettingsUiState(
                        homeAppsNum = homeAppsNum,
                        dateTimeVisibility = dateTimeVisibility,
                        homeAlignment = homeAlignment,
                        homeBottomAlignment = homeBottomAlignment,
                        statusBarOn = statusBarOn,
                        screenTimeOn = usagePermissionGranted && screenTimeEnabled,
                        isDefaultLauncher = isDefaultLauncher == true,
                        autoShowKeyboard = autoShowKeyboard,
                        solidWallpaper = solidWallpaper,
                        showThemeRow = !einkDisplay,
                        appTheme = appTheme,
                        boldFont = boldFont,
                        textSizeLabel = "%.1f".format(displayedTextSize),
                        swipeLeftAppName = swipeLeftAppName,
                        swipeRightAppName = swipeRightAppName,
                        swipeLeftEnabled = swipeLeftEnabled,
                        swipeRightEnabled = swipeRightEnabled,
                        lockModeOn = lockModeOn,
                        accessibilityServiceEnabled = accessibilityServiceEnabled,
                        accessibilityOverlayVisible = accessibilityOverlayVisible,
                    ),
                colors = colors,
                callbacks = callbacks,
            )
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.isMinLauncherDefault()
        applyStatusBarInsets(prefs.showStatusBar)
        if (prefs.firstSettingsOpen) {
            viewModel.postDialog(Constants.Dialog.ABOUT)
            prefs.firstSettingsOpen = false
        }
        collectOnStart(viewModel.updateSwipeApps) {
            refreshSwipeApps()
        }
    }

    private fun createCallbacks(): SettingsCallbacks =
        SettingsCallbacks(
            onOpenHiddenApps = { showHiddenApps() },
            onOpenAppInfo = {
                openAppInfo(requireContext(), Process.myUserHandle(), BuildConfig.APPLICATION_ID)
            },
            onRequestResetLauncher = { viewModel.requestResetLauncher() },
            onToggleScreenTime = { toggleScreenTime() },
            onSelectHomeAppsNum = { updateHomeAppsNum(it) },
            onSelectDateTime = { toggleDateTime(it) },
            onSelectAlignment = { viewModel.updateHomeAlignment(it) },
            onToggleBottomAlignment = { updateHomeBottomAlignment() },
            onLongPressAlignment = { handleAlignmentLongClick() },
            onToggleStatusBar = { toggleStatusBar() },
            onToggleAutoShowKeyboard = { toggleKeyboardText() },
            onToggleSolidWallpaper = { toggleSolidWallpaper() },
            onLongPressSolidWallpaper = { removeWallpaper() },
            onSelectTheme = { updateTheme(it) },
            onToggleBoldFont = { toggleBoldFont() },
            onAdjustTextSize = { adjustTextSizePreview(it) },
            onFinalizeTextSize = { applyTextSizeScale() },
            onSwipeAppClick = { isLeft ->
                showAppListIfEnabled(
                    if (isLeft) Constants.FLAG_SET_SWIPE_LEFT_APP else Constants.FLAG_SET_SWIPE_RIGHT_APP,
                )
            },
            onSwipeAppLongPress = { isLeft ->
                if (isLeft) toggleSwipeLeft() else toggleSwipeRight()
            },
            onToggleLockMode = { toggleLockMode() },
            onLongPressLockMode = { openAccessibilitySettings() },
            onOpenAccessibilitySettings = { openAccessibilityService() },
            onCloseAccessibility = { accessibilityOverlayVisible = false },
        )

    private fun handleAlignmentLongClick() {
        prefs.appLabelAlignment = prefs.homeAlignment
        findNavController().navigate(R.id.action_settingsFragment_to_appListFragment)
        requireContext().showToast(getString(R.string.alignment_changed))
    }

    private fun toggleSwipeLeft() {
        prefs.swipeLeftEnabled = !prefs.swipeLeftEnabled
        requireContext().showToast(
            getString(
                if (prefs.swipeLeftEnabled) R.string.swipe_left_app_enabled else R.string.swipe_left_app_disabled,
            ),
        )
    }

    private fun toggleSwipeRight() {
        prefs.swipeRightEnabled = !prefs.swipeRightEnabled
        requireContext().showToast(
            getString(
                if (prefs.swipeRightEnabled) R.string.swipe_right_app_enabled else R.string.swipe_right_app_disabled,
            ),
        )
    }

    private fun toggleStatusBar() {
        prefs.showStatusBar = !prefs.showStatusBar
        applyStatusBarInsets(prefs.showStatusBar)
    }

    private fun applyStatusBarInsets(show: Boolean) {
        if (show) {
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        } else {
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }

    private fun toggleDateTime(selected: Int) {
        prefs.dateTimeVisibility = selected
        viewModel.toggleDateTime()
    }

    private fun showHiddenApps() {
        if (prefs.hiddenApps.isEmpty()) {
            requireContext().showToast(getString(R.string.no_hidden_apps))
            return
        }
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            Bundle().apply { putInt(Constants.Key.FLAG, Constants.FLAG_HIDDEN_APPS) },
        )
    }

    private fun openAccessibilityService() {
        accessibilityOverlayVisible = false
        populateLockSettings()
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    /** Long-press on the lock row: jump straight to the system accessibility settings. */
    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun toggleLockMode() {
        accessibilityServiceEnabled = isAccessServiceEnabled(requireContext())
        if (!prefs.lockModeOn && !accessibilityServiceEnabled) {
            accessibilityOverlayVisible = true
            return
        }
        prefs.lockModeOn = !prefs.lockModeOn
        populateLockSettings()
    }

    /**
     * Long press on the wallpaper row: force the theme to one the solid wallpaper suits
     * and switch the toggle off. The activity is recreated so the forced theme
     * (and the transparent background) apply immediately, not at the next
     * unrelated recreation.
     */
    private fun removeWallpaper() {
        prefs.appTheme =
            if (einkDisplay) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }
        prefs.solidWallpaper = false
        requireActivity().recreate()
    }

    private fun toggleSolidWallpaper() {
        prefs.solidWallpaper = !prefs.solidWallpaper
    }

    private fun updateHomeAppsNum(num: Int) {
        prefs.homeAppsNum = num
        viewModel.refreshHome(true)
    }

    private fun adjustTextSizePreview(delta: Float) {
        val maxScale = if (isTablet(requireContext())) 2.0f else 1.5f
        val current = if (pendingTextSizeScale > 0) pendingTextSizeScale else prefs.textSizeScale
        val newScale = ((current + delta) * 10f).roundToInt() / 10f
        val clamped = newScale.coerceIn(0.5f, maxScale)
        if (clamped == current) return
        pendingTextSizeScale = clamped
    }

    private fun applyTextSizeScale() {
        if (pendingTextSizeScale < 0 || prefs.textSizeScale == pendingTextSizeScale) {
            pendingTextSizeScale = -1f
            return
        }
        prefs.textSizeScale = pendingTextSizeScale
        pendingTextSizeScale = -1f
        val activity = activity ?: return
        if (activity.isChangingConfigurations.not()) {
            activity.recreate()
        }
    }

    private fun toggleKeyboardText() {
        if (prefs.autoShowKeyboard && prefs.keyboardMessageShown.not()) {
            viewModel.postDialog(Constants.Dialog.KEYBOARD)
            prefs.keyboardMessageShown = true
        } else {
            prefs.autoShowKeyboard = !prefs.autoShowKeyboard
            autoShowKeyboard = prefs.autoShowKeyboard
        }
    }

    private fun updateTheme(theme: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == theme) return
        prefs.appTheme = theme
        appTheme = theme
        requireActivity().recreate()
    }

    private fun toggleBoldFont() {
        prefs.boldFont = !prefs.boldFont
        requireActivity().recreate()
    }

    private fun toggleScreenTime() {
        usagePermissionGranted = requireContext().appUsagePermissionGranted()
        if (!usagePermissionGranted) {
            viewModel.postDialog(Constants.Dialog.DIGITAL_WELLBEING)
            return
        }
        prefs.screenTimeEnabled = !prefs.screenTimeEnabled
    }

    private fun updateHomeBottomAlignment() {
        if (viewModel.isMinLauncherDefault.value != true) {
            requireContext().showToast(getString(R.string.please_set_minlauncher_as_default_first), Toast.LENGTH_LONG)
            return
        }
        prefs.homeBottomAlignment = !prefs.homeBottomAlignment
        viewModel.updateHomeAlignment(prefs.homeAlignment)
    }

    private fun populateLockSettings() {
        accessibilityServiceEnabled = isAccessServiceEnabled(requireContext())
        lockModeOn = prefs.lockModeOn && accessibilityServiceEnabled
    }

    private fun refreshSwipeApps() {
        swipeLeftAppName = prefs.appNameSwipeLeft.ifEmpty { getString(R.string.swipe_left_app_default) }
        swipeRightAppName = prefs.appNameSwipeRight.ifEmpty { getString(R.string.swipe_right_app_default) }
    }

    private fun showAppListIfEnabled(flag: Int) {
        if ((flag == Constants.FLAG_SET_SWIPE_LEFT_APP) and !prefs.swipeLeftEnabled) {
            requireContext().showToast(getString(R.string.long_press_to_enable))
            return
        }
        if ((flag == Constants.FLAG_SET_SWIPE_RIGHT_APP) and !prefs.swipeRightEnabled) {
            requireContext().showToast(getString(R.string.long_press_to_enable))
            return
        }
        viewModel.getAppList(true)
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            Bundle().apply { putInt(Constants.Key.FLAG, flag) },
        )
    }

    private fun resolveSettingsColors(): SettingsColors {
        // Resolve once, from the activity theme, so the day/night variant is the
        // one the (about to be displayed) screen uses. Theme switches recreate.
        val typedArray = requireActivity().obtainStyledAttributes(R.styleable.SettingsColors)
        return try {
            SettingsColors(
                primaryColor = typedArray.requireColor(R.styleable.SettingsColors_primaryColor, "primaryColor"),
                shadeDark =
                    typedArray.requireColor(R.styleable.SettingsColors_primaryShadeDarkColor, "primaryShadeDarkColor"),
                colorTrans50 =
                    typedArray.requireColor(R.styleable.SettingsColors_primaryColorTrans50, "primaryColorTrans50"),
                inverse =
                    typedArray.requireColor(R.styleable.SettingsColors_primaryInverseColor, "primaryInverseColor"),
                inverseTrans80 =
                    typedArray.requireColor(
                        R.styleable.SettingsColors_primaryColorInverseTrans80,
                        "primaryColorInverseTrans80",
                    ),
                inverseTrans90 =
                    typedArray.requireColor(
                        R.styleable.SettingsColors_primaryColorInverseTrans90,
                        "primaryColorInverseTrans90",
                    ),
                dialogShade =
                    typedArray.requireColor(R.styleable.SettingsColors_dialogShadeColor, "dialogShadeColor"),
                textShadow =
                    typedArray.requireColor(
                        R.styleable.SettingsColors_primaryTextShadowColor,
                        "primaryTextShadowColor",
                    ),
            )
        } finally {
            typedArray.recycle()
        }
    }
}

/**
 * Fail loudly (like getColorFromAttr) instead of silently painting transparent
 * when a theme attribute the settings palette depends on is missing.
 */
private fun TypedArray.requireColor(
    index: Int,
    name: String,
): Int {
    val value = TypedValue()
    check(getValue(index, value)) { "Failed to resolve theme attribute $name" }
    check(value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) {
        "Theme attribute $name is not a color (type=${value.type})"
    }
    return value.data
}
