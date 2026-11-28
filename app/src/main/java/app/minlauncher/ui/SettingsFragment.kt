package app.minlauncher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import app.minlauncher.LauncherApp
import app.minlauncher.R
import app.minlauncher.SettingsViewModel
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.helper.appUsagePermissionGranted
import app.minlauncher.helper.isEinkDisplay
import app.minlauncher.helper.showToast
import app.minlauncher.ui.settings.SettingsCallbacks
import app.minlauncher.ui.settings.SettingsScreenHost
import app.minlauncher.ui.settings.adjustTextSizePreview
import app.minlauncher.ui.settings.applyStatusBarInsets
import app.minlauncher.ui.settings.applyTextSizeScale
import app.minlauncher.ui.settings.openAccessibilityService
import app.minlauncher.ui.settings.openAccessibilitySettings
import app.minlauncher.ui.settings.openAppInfo
import app.minlauncher.ui.settings.populateLockSettings
import app.minlauncher.ui.settings.refreshSwipeApps
import app.minlauncher.ui.settings.removeWallpaper
import app.minlauncher.ui.settings.resolveSettingsColors
import app.minlauncher.ui.settings.toggleBoldFont
import app.minlauncher.ui.settings.toggleDateTime
import app.minlauncher.ui.settings.toggleKeyboardText
import app.minlauncher.ui.settings.toggleLockMode
import app.minlauncher.ui.settings.toggleScreenTime
import app.minlauncher.ui.settings.toggleSolidWallpaper
import app.minlauncher.ui.settings.toggleStatusBar
import app.minlauncher.ui.settings.updateHomeAppsNum
import app.minlauncher.ui.settings.updateHomeBottomAlignment
import app.minlauncher.ui.settings.updateTheme

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

    // Compose state lives in the view model: the pending text-size preview, the
    // theme (only changes through Activity.recreate), and the values that have
    // no repository mirror (auto-show keyboard, lock mode, swipe app labels).

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        prefs = Prefs(requireContext())
        viewModel.appTheme = prefs.appTheme
        viewModel.autoShowKeyboard = prefs.autoShowKeyboard
        viewModel.accessibilityOverlayVisible = false
        viewModel.usagePermissionGranted = requireContext().appUsagePermissionGranted()
        viewModel.einkDisplay = requireContext().isEinkDisplay()
        populateLockSettings(requireContext(), prefs, viewModel)
        refreshSwipeApps(requireContext(), prefs, viewModel)

        val colors = requireActivity().resolveSettingsColors()
        val callbacks = createCallbacks()

        return ComposeView(requireContext()).apply {
            id = R.id.mainActivityLayout
            setBackgroundColor(colors.shadeDark)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { SettingsScreenHost(viewModel, colors, callbacks) }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.isMinLauncherDefault()
        applyStatusBarInsets(requireActivity(), prefs.showStatusBar)
        if (prefs.firstSettingsOpen) {
            viewModel.postDialog(Constants.Dialog.ABOUT)
            prefs.firstSettingsOpen = false
        }
        collectOnStart(viewModel.updateSwipeApps) {
            refreshSwipeApps(requireContext(), prefs, viewModel)
        }
    }

    private fun createCallbacks(): SettingsCallbacks =
        SettingsCallbacks(
            onOpenHiddenApps = { showHiddenApps() },
            onOpenAppInfo = { openAppInfo(requireContext()) },
            onRequestResetLauncher = { viewModel.requestResetLauncher() },
            onToggleScreenTime = { toggleScreenTime(requireContext(), prefs, viewModel) },
            onSelectHomeAppsNum = { updateHomeAppsNum(prefs, viewModel, it) },
            onSelectDateTime = { toggleDateTime(prefs, viewModel, it) },
            onSelectAlignment = { viewModel.updateHomeAlignment(it) },
            onToggleBottomAlignment = { updateHomeBottomAlignment(requireContext(), prefs, viewModel) },
            onLongPressAlignment = { handleAlignmentLongClick() },
            onToggleStatusBar = { toggleStatusBar(requireActivity(), prefs) },
            onToggleAutoShowKeyboard = { toggleKeyboardText(prefs, viewModel) },
            onToggleSolidWallpaper = { toggleSolidWallpaper(prefs) },
            onLongPressSolidWallpaper = { removeWallpaper(requireActivity(), prefs, viewModel) },
            onSelectTheme = { updateTheme(requireActivity(), prefs, viewModel, it) },
            onToggleBoldFont = { toggleBoldFont(requireActivity(), prefs) },
            onAdjustTextSize = { adjustTextSizePreview(requireContext(), prefs, viewModel, it) },
            onFinalizeTextSize = { applyTextSizeScale(activity, prefs, viewModel) },
            onSwipeAppClick = { isLeft ->
                showAppListIfEnabled(
                    if (isLeft) Constants.FLAG_SET_SWIPE_LEFT_APP else Constants.FLAG_SET_SWIPE_RIGHT_APP,
                )
            },
            onSwipeAppLongPress = { isLeft ->
                if (isLeft) toggleSwipeLeft() else toggleSwipeRight()
            },
            onToggleLockMode = { toggleLockMode(requireContext(), prefs, viewModel) },
            onLongPressLockMode = { openAccessibilitySettings(requireActivity()) },
            onOpenAccessibilitySettings = { openAccessibilityService(requireActivity(), prefs, viewModel) },
            onCloseAccessibility = { viewModel.accessibilityOverlayVisible = false },
        )

    // Navigation stays in the fragment until the settings entry is rebuilt on
    // the Nav3 back stack (Stage 2, task 5).
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
}
