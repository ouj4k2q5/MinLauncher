package app.minlauncher.ui

import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.minlauncher.BuildConfig
import app.minlauncher.MainViewModel
import app.minlauncher.R
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.databinding.FragmentSettingsBinding
import app.minlauncher.helper.animateAlpha
import app.minlauncher.helper.appUsagePermissionGranted
import app.minlauncher.helper.getColorFromAttr
import app.minlauncher.helper.isAccessServiceEnabled
import app.minlauncher.helper.isEinkDisplay
import app.minlauncher.helper.isTablet
import app.minlauncher.helper.openAppInfo
import app.minlauncher.helper.setPlainWallpaperByTheme
import app.minlauncher.helper.showToast
import kotlin.math.roundToInt

class SettingsFragment :
    BaseFragment(),
    View.OnClickListener,
    View.OnLongClickListener {
    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    private var viewBinding: FragmentSettingsBinding? = null
    private val binding get() = viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewBinding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: error("Fragment is not attached to an activity")
        viewModel.isMinLauncherDefault()

        binding.homeAppsNum.text = prefs.homeAppsNum.toString()
        populateKeyboardText()
        populateScreenTimeOnOff()
        populateLockSettings()
        // Home button for recents feature disabled
        // populateHomeButtonRecents()
        populateWallpaperText()
        populateAppThemeText()
        populateTextSize()
        populateBoldFont()
        populateAlignment()
        populateStatusBar()
        populateDateTime()
        populateSwipeApps()
        populateSwipeDownAction()
        initClickListeners()
        initObservers()
    }

    override fun onClick(view: View) {
        binding.appsNumSelectLayout.visibility = View.GONE
        binding.dateTimeSelectLayout.visibility = View.GONE
        binding.appThemeSelectLayout.visibility = View.GONE
        binding.swipeDownSelectLayout.visibility = View.GONE
        if (view.id != R.id.textSizeMinus && view.id != R.id.textSizePlus) {
            if (binding.textSizesLayout.isVisible) {
                binding.textSizesLayout.visibility = View.GONE
                applyTextSizeScale()
            }
        }
        if (view.id != R.id.alignmentBottom) {
            binding.alignmentSelectLayout.visibility = View.GONE
        }

        when (view.id) {
            R.id.minlauncherHiddenApps -> showHiddenApps()
            R.id.screenTimeOnOff -> viewModel.showDialog.postValue(Constants.Dialog.DIGITAL_WELLBEING)
            R.id.appInfo -> openAppInfo(requireContext(), Process.myUserHandle(), BuildConfig.APPLICATION_ID)
            R.id.setLauncher -> viewModel.resetLauncherLiveData.call()
            R.id.toggleLock -> toggleLockMode()
            // Home button for recents feature disabled
            // R.id.homeButtonRecents -> toggleHomeButtonRecents()
            R.id.autoShowKeyboard -> toggleKeyboardText()
            R.id.homeAppsNum -> binding.appsNumSelectLayout.visibility = View.VISIBLE
            R.id.solidWallpaper -> toggleSolidWallpaper()
            R.id.alignment -> binding.alignmentSelectLayout.visibility = View.VISIBLE
            R.id.alignmentLeft -> viewModel.updateHomeAlignment(Gravity.START)
            R.id.alignmentCenter -> viewModel.updateHomeAlignment(Gravity.CENTER)
            R.id.alignmentRight -> viewModel.updateHomeAlignment(Gravity.END)
            R.id.alignmentBottom -> updateHomeBottomAlignment()
            R.id.statusBar -> toggleStatusBar()
            R.id.dateTime -> binding.dateTimeSelectLayout.visibility = View.VISIBLE
            R.id.dateTimeOn -> toggleDateTime(Constants.DateTime.ON)
            R.id.dateTimeOff -> toggleDateTime(Constants.DateTime.OFF)
            R.id.dateOnly -> toggleDateTime(Constants.DateTime.DATE_ONLY)
            R.id.appThemeText -> binding.appThemeSelectLayout.visibility = View.VISIBLE
            R.id.themeLight -> updateTheme(AppCompatDelegate.MODE_NIGHT_NO)
            R.id.themeDark -> updateTheme(AppCompatDelegate.MODE_NIGHT_YES)
            R.id.themeSystem -> updateTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            R.id.textSizeValue -> binding.textSizesLayout.visibility = View.VISIBLE
            R.id.boldFont -> toggleBoldFont()
            R.id.actionAccessibility -> openAccessibilityService()
            R.id.closeAccessibility -> toggleAccessibilityVisibility(false)

            R.id.tvGestures -> binding.flSwipeDown.visibility = View.VISIBLE

            R.id.maxApps0 -> updateHomeAppsNum(0)
            R.id.maxApps1 -> updateHomeAppsNum(1)
            R.id.maxApps2 -> updateHomeAppsNum(2)
            R.id.maxApps3 -> updateHomeAppsNum(3)
            R.id.maxApps4 -> updateHomeAppsNum(4)
            R.id.maxApps5 -> updateHomeAppsNum(5)
            R.id.maxApps6 -> updateHomeAppsNum(6)
            R.id.maxApps7 -> updateHomeAppsNum(7)
            R.id.maxApps8 -> updateHomeAppsNum(8)

            R.id.textSizeMinus -> adjustTextSizePreview(-0.1f)
            R.id.textSizePlus -> adjustTextSizePreview(0.1f)

            R.id.swipeLeftApp -> showAppListIfEnabled(Constants.FLAG_SET_SWIPE_LEFT_APP)
            R.id.swipeRightApp -> showAppListIfEnabled(Constants.FLAG_SET_SWIPE_RIGHT_APP)
            R.id.swipeDownAction -> binding.swipeDownSelectLayout.visibility = View.VISIBLE
            R.id.notifications -> updateSwipeDownAction(Constants.SwipeDownAction.NOTIFICATIONS)
            R.id.search -> updateSwipeDownAction(Constants.SwipeDownAction.SEARCH)
        }
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.alignment -> {
                prefs.appLabelAlignment = prefs.homeAlignment
                findNavController().navigate(R.id.action_settingsFragment_to_appListFragment)
                requireContext().showToast(getString(R.string.alignment_changed))
            }

            R.id.solidWallpaper -> removeWallpaper()
            R.id.appThemeText -> {
                binding.appThemeSelectLayout.visibility = View.VISIBLE
                binding.themeSystem.visibility = View.VISIBLE
            }

            R.id.swipeLeftApp -> toggleSwipeLeft()
            R.id.swipeRightApp -> toggleSwipeRight()
            R.id.toggleLock -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        return true
    }

    private fun initClickListeners() {
        binding.minlauncherHiddenApps.setOnClickListener(this)
        binding.scrollLayout.setOnClickListener(this)
        binding.appInfo.setOnClickListener(this)
        binding.setLauncher.setOnClickListener(this)
        binding.autoShowKeyboard.setOnClickListener(this)
        binding.toggleLock.setOnClickListener(this)
        // Home button for recents feature disabled
        // binding.homeButtonRecents.setOnClickListener(this)
        binding.homeAppsNum.setOnClickListener(this)
        binding.screenTimeOnOff.setOnClickListener(this)
        binding.solidWallpaper.setOnClickListener(this)
        binding.alignment.setOnClickListener(this)
        binding.alignmentLeft.setOnClickListener(this)
        binding.alignmentCenter.setOnClickListener(this)
        binding.alignmentRight.setOnClickListener(this)
        binding.alignmentBottom.setOnClickListener(this)
        binding.statusBar.setOnClickListener(this)
        binding.dateTime.setOnClickListener(this)
        binding.dateTimeOn.setOnClickListener(this)
        binding.dateTimeOff.setOnClickListener(this)
        binding.dateOnly.setOnClickListener(this)
        binding.swipeLeftApp.setOnClickListener(this)
        binding.swipeRightApp.setOnClickListener(this)
        binding.swipeDownAction.setOnClickListener(this)
        binding.search.setOnClickListener(this)
        binding.notifications.setOnClickListener(this)
        binding.appThemeText.setOnClickListener(this)
        binding.themeLight.setOnClickListener(this)
        binding.themeDark.setOnClickListener(this)
        binding.themeSystem.setOnClickListener(this)
        binding.textSizeValue.setOnClickListener(this)
        binding.boldFont.setOnClickListener(this)
        binding.actionAccessibility.setOnClickListener(this)
        binding.closeAccessibility.setOnClickListener(this)

        binding.maxApps0.setOnClickListener(this)
        binding.maxApps1.setOnClickListener(this)
        binding.maxApps2.setOnClickListener(this)
        binding.maxApps3.setOnClickListener(this)
        binding.maxApps4.setOnClickListener(this)
        binding.maxApps5.setOnClickListener(this)
        binding.maxApps6.setOnClickListener(this)
        binding.maxApps7.setOnClickListener(this)
        binding.maxApps8.setOnClickListener(this)

        binding.textSizeMinus.setOnClickListener(this)
        binding.textSizePlus.setOnClickListener(this)

        binding.solidWallpaper.setOnLongClickListener(this)
        binding.alignment.setOnLongClickListener(this)
        binding.appThemeText.setOnLongClickListener(this)
        binding.swipeLeftApp.setOnLongClickListener(this)
        binding.swipeRightApp.setOnLongClickListener(this)
        binding.toggleLock.setOnLongClickListener(this)
    }

    private fun initObservers() {
        if (prefs.firstSettingsOpen) {
            viewModel.showDialog.postValue(Constants.Dialog.ABOUT)
            prefs.firstSettingsOpen = false
        }
        viewModel.isMinLauncherDefault.observe(viewLifecycleOwner) {
            if (it) binding.setLauncher.text = getString(R.string.change_default_launcher)
        }
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) {
            populateAlignment()
        }
        viewModel.updateSwipeApps.observe(viewLifecycleOwner) {
            populateSwipeApps()
        }
    }

    private fun toggleSwipeLeft() {
        prefs.swipeLeftEnabled = !prefs.swipeLeftEnabled
        if (prefs.swipeLeftEnabled) {
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            requireContext().showToast(getString(R.string.swipe_left_app_enabled))
        } else {
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            requireContext().showToast(getString(R.string.swipe_left_app_disabled))
        }
    }

    private fun toggleSwipeRight() {
        prefs.swipeRightEnabled = !prefs.swipeRightEnabled
        if (prefs.swipeRightEnabled) {
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColor))
            requireContext().showToast(getString(R.string.swipe_right_app_enabled))
        } else {
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
            requireContext().showToast(getString(R.string.swipe_right_app_disabled))
        }
    }

    private fun toggleStatusBar() {
        prefs.showStatusBar = !prefs.showStatusBar
        populateStatusBar()
    }

    private fun populateStatusBar() {
        if (prefs.showStatusBar) {
            showStatusBar()
            binding.statusBar.text = getString(R.string.on)
        } else {
            hideStatusBar()
            binding.statusBar.text = getString(R.string.off)
        }
    }

    private fun toggleDateTime(selected: Int) {
        prefs.dateTimeVisibility = selected
        populateDateTime()
        viewModel.toggleDateTime()
    }

    private fun populateDateTime() {
        binding.dateTime.text =
            getString(
                when (prefs.dateTimeVisibility) {
                    Constants.DateTime.DATE_ONLY -> R.string.date
                    Constants.DateTime.ON -> R.string.on
                    else -> R.string.off
                },
            )
    }

    private fun showStatusBar() {
        requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
    }

    private fun hideStatusBar() {
        requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
    }

    private fun showHiddenApps() {
        if (prefs.hiddenApps.isEmpty()) {
            requireContext().showToast(getString(R.string.no_hidden_apps))
            return
        }
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf(Constants.Key.FLAG to Constants.FLAG_HIDDEN_APPS),
        )
    }

    private fun toggleAccessibilityVisibility(show: Boolean) {
        if (isAccessServiceEnabled(requireContext())) {
            binding.actionAccessibility.text = getString(R.string.disable)
        }
        binding.accessibilityLayout.isVisible = show
        binding.scrollView.animateAlpha(if (show) 0.5f else 1f)
    }

    private fun openAccessibilityService() {
        toggleAccessibilityVisibility(false)
        // prefs.lockModeOn = true
        populateLockSettings()
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun toggleLockMode() {
        if (!prefs.lockModeOn && !isAccessServiceEnabled(requireContext())) {
            toggleAccessibilityVisibility(true)
            return
        }
        prefs.lockModeOn = !prefs.lockModeOn
        populateLockSettings()
    }

    /**
     * Long press on the wallpaper row: force the theme to one the solid wallpaper suits
     * and repaint it, then switch the toggle off.
     */
    private fun removeWallpaper() {
        prefs.appTheme =
            if (requireContext().isEinkDisplay()) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }
        setPlainWallpaperByTheme(requireContext(), prefs.appTheme)
        prefs.solidWallpaper = false
        populateWallpaperText()
    }

    private fun toggleSolidWallpaper() {
        prefs.solidWallpaper = !prefs.solidWallpaper
        populateWallpaperText()
        if (prefs.solidWallpaper) {
            setPlainWallpaperByTheme(requireContext(), prefs.appTheme)
        }
    }

    private fun updateHomeAppsNum(num: Int) {
        binding.homeAppsNum.text = num.toString()
        binding.appsNumSelectLayout.visibility = View.GONE
        prefs.homeAppsNum = num
        viewModel.refreshHome(true)
    }

    private var pendingTextSizeScale: Float = -1f

    private fun adjustTextSizePreview(delta: Float) {
        val maxScale = if (isTablet(requireContext())) 2.0f else 1.5f
        val current = if (pendingTextSizeScale > 0) pendingTextSizeScale else prefs.textSizeScale
        val newScale = ((current + delta) * 10f).roundToInt() / 10f
        val clamped = newScale.coerceIn(0.5f, maxScale)
        if (clamped == current) return
        pendingTextSizeScale = clamped
        val formatted = "%.1f".format(clamped)
        binding.textSizeValue.text = formatted
        binding.textSizeCurrent.text = formatted
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
            viewModel.showDialog.postValue(Constants.Dialog.KEYBOARD)
            prefs.keyboardMessageShown = true
        } else {
            prefs.autoShowKeyboard = !prefs.autoShowKeyboard
            populateKeyboardText()
        }
    }

    private fun updateTheme(appTheme: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == appTheme) return
        prefs.appTheme = appTheme
        populateAppThemeText(appTheme)
        setAppTheme(appTheme)
    }

    private fun setAppTheme(theme: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == theme) return
        if (prefs.solidWallpaper) setPlainWallpaperByTheme(requireContext(), theme)
        requireActivity().recreate()
    }

    private fun populateAppThemeText(appTheme: Int = prefs.appTheme) {
        // MainActivity forces the light theme on e-ink, so the switcher would do nothing there.
        if (requireContext().isEinkDisplay()) {
            binding.appThemeLayout.visibility = View.GONE
            return
        }
        when (appTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> binding.appThemeText.text = getString(R.string.dark)
            AppCompatDelegate.MODE_NIGHT_NO -> binding.appThemeText.text = getString(R.string.light)
            else -> binding.appThemeText.text = getString(R.string.system_default)
        }
    }

    private fun populateTextSize() {
        val formatted = "%.1f".format(prefs.textSizeScale)
        binding.textSizeValue.text = formatted
        binding.textSizeCurrent.text = formatted
    }

    private fun toggleBoldFont() {
        prefs.boldFont = !prefs.boldFont
        populateBoldFont()
        requireActivity().recreate()
    }

    private fun populateBoldFont() {
        binding.boldFont.text = getString(if (prefs.boldFont) R.string.on else R.string.off)
    }

    private fun populateScreenTimeOnOff() {
        binding.screenTimeOnOff.text =
            getString(if (requireContext().appUsagePermissionGranted()) R.string.on else R.string.off)
    }

    private fun populateKeyboardText() {
        if (prefs.autoShowKeyboard) {
            binding.autoShowKeyboard.text = getString(R.string.on)
        } else {
            binding.autoShowKeyboard.text = getString(R.string.off)
        }
    }

    private fun populateWallpaperText() {
        binding.solidWallpaper.text = getString(if (prefs.solidWallpaper) R.string.on else R.string.off)
    }

    private fun updateHomeBottomAlignment() {
        if (viewModel.isMinLauncherDefault.value != true) {
            requireContext().showToast(getString(R.string.please_set_minlauncher_as_default_first), Toast.LENGTH_LONG)
            return
        }
        prefs.homeBottomAlignment = !prefs.homeBottomAlignment
        populateAlignment()
        viewModel.updateHomeAlignment(prefs.homeAlignment)
    }

    private fun populateAlignment() {
        when (prefs.homeAlignment) {
            Gravity.START -> binding.alignment.text = getString(R.string.left)
            Gravity.CENTER -> binding.alignment.text = getString(R.string.center)
            Gravity.END -> binding.alignment.text = getString(R.string.right)
        }
        binding.alignmentBottom.text =
            if (prefs.homeBottomAlignment) {
                getString(R.string.bottom_on)
            } else {
                getString(R.string.bottom_off)
            }
    }

    // Home button for recents feature disabled
    // private fun toggleHomeButtonRecents() {
    //     if (!prefs.homeButtonShowRecents && !isAccessServiceEnabled(requireContext())) {
    //         toggleAccessibilityVisibility(true)
    //         return
    //     }
    //     prefs.homeButtonShowRecents = !prefs.homeButtonShowRecents
    //     populateHomeButtonRecents()
    // }

    // private fun populateHomeButtonRecents() {
    //     binding.homeButtonRecents.text = getString(
    //         if (prefs.homeButtonShowRecents && isAccessServiceEnabled(requireContext())) R.string.on
    //         else R.string.off
    //     )
    // }

    private fun populateLockSettings() {
        binding.toggleLock.text =
            getString(
                if (prefs.lockModeOn && isAccessServiceEnabled(requireContext())) {
                    R.string.on
                } else {
                    R.string.off
                },
            )
    }

    private fun populateSwipeDownAction() {
        binding.swipeDownAction.text =
            when (prefs.swipeDownAction) {
                Constants.SwipeDownAction.NOTIFICATIONS -> getString(R.string.notifications)
                else -> getString(R.string.search)
            }
    }

    private fun updateSwipeDownAction(swipeDownFor: Int) {
        if (prefs.swipeDownAction == swipeDownFor) return
        prefs.swipeDownAction = swipeDownFor
        populateSwipeDownAction()
    }

    private fun populateSwipeApps() {
        binding.swipeLeftApp.text = prefs.appNameSwipeLeft.ifEmpty { getString(R.string.swipe_left_app_default) }
        binding.swipeRightApp.text = prefs.appNameSwipeRight.ifEmpty { getString(R.string.swipe_right_app_default) }
        if (!prefs.swipeLeftEnabled) {
            binding.swipeLeftApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
        }
        if (!prefs.swipeRightEnabled) {
            binding.swipeRightApp.setTextColor(requireContext().getColorFromAttr(R.attr.primaryColorTrans50))
        }
    }

//    private fun populateDigitalWellbeing() {
//        binding.digitalWellbeing.isVisible = requireContext()
//                .isPackageInstalled(Constants.DIGITAL_WELLBEING_PACKAGE_NAME).not()
//                && requireContext().isPackageInstalled(Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME).not()
//                && prefs.hideDigitalWellbeing.not()
//    }

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
            bundleOf(Constants.Key.FLAG to flag),
        )
    }

    override fun onDestroyView() {
        applyTextSizeScale()
        super.onDestroyView()
        viewBinding = null
    }
}
