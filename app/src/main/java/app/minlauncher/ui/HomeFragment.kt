package app.minlauncher.ui

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import app.minlauncher.HomeViewModel
import app.minlauncher.LauncherApp
import app.minlauncher.R
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.databinding.FragmentHomeBinding
import app.minlauncher.helper.LockRequest
import app.minlauncher.helper.appUsagePermissionGranted
import app.minlauncher.helper.expandNotificationDrawer
import app.minlauncher.helper.getUserHandleFromString
import app.minlauncher.helper.isPackageInstalled
import app.minlauncher.helper.openCalendar
import app.minlauncher.helper.openCameraApp
import app.minlauncher.helper.openClock
import app.minlauncher.helper.openDialerApp
import app.minlauncher.helper.showToast
import app.minlauncher.ui.home.HomeCallbacks
import app.minlauncher.ui.home.HomeColors
import app.minlauncher.ui.home.HomeScreen
import app.minlauncher.ui.home.HomeSlotState
import app.minlauncher.ui.home.HomeUiState
import app.minlauncher.ui.home.resolveHomeColors
import java.time.Clock

private const val TAG = "HomeFragment"

// One small method per home action (the same layout as the old listener
// callbacks, split by side effect); the count is inherent to binding the home
// screen's interactions to the Fragment, not a sign of a class doing too
// many things.
@Suppress("TooManyFunctions")
class HomeFragment : BaseFragment() {
    private lateinit var prefs: Prefs

    // Explicit factory: under Robolectric the default factory's Application
    // fallback can be stale across tests, so read the repository from the
    // current activity's app. In production the activity's app is the one
    // and only LauncherApp instance.
    private val viewModel: HomeViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application
                HomeViewModel(app, (app as LauncherApp).launcherRepository)
            }
        }
    }

    private var viewBinding: FragmentHomeBinding? = null
    private val binding get() = viewBinding!!

    // The renderable home screen, kept as Compose state (the AppDrawerFragment
    // pattern): each fragment-side populate/update writes state and the
    // screen recomposes, replacing the old binding.view writes.
    private var slots by mutableStateOf<List<HomeSlotState>>(emptyList())
    private var homeAppAlignment by mutableIntStateOf(Gravity.START)
    private var homeBottomAlignment by mutableStateOf(false)
    private var dateTimeVisibility by mutableIntStateOf(Constants.DateTime.ON)
    private var showStatusBar by mutableStateOf(false)
    private var showFirstRunTips by mutableStateOf(false)
    private var showSetDefaultLauncher by mutableStateOf(false)
    private var screenTimeText by mutableStateOf<String?>(null)

    // Resolved once for the whole screen lifetime; theme switches recreate.
    private var homeColors: HomeColors? = null
    private var callbacks: HomeCallbacks? = null

    companion object {
        /**
         * Test seam: the home clock and date line render from this clock,
         * which would make screenshot baselines time-dependent. Tests can
         * point this at a fixed instant; production behavior is unchanged.
         */
        internal var clock: Clock = Clock.systemDefaultZone()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewBinding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())

        homeColors = requireActivity().resolveHomeColors()
        callbacks = createCallbacks()
        binding.homeContent.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { HomeScreenHost() }
        }

        initObservers()
        setHomeAlignment(viewModel.homeAppAlignment.value)
    }

    override fun onResume() {
        super.onResume()
        populateHomeScreen()
        viewModel.isMinLauncherDefault()
        if (prefs.showStatusBar) {
            showStatusBar()
        } else {
            hideStatusBar()
        }
    }

    @Composable
    private fun HomeScreenHost() {
        HomeScreen(
            state =
                HomeUiState(
                    slots = slots,
                    homeAppAlignment = homeAppAlignment,
                    homeBottomAlignment = homeBottomAlignment,
                    dateTimeVisibility = dateTimeVisibility,
                    showStatusBar = showStatusBar,
                    showFirstRunTips = showFirstRunTips,
                    showSetDefaultLauncher = showSetDefaultLauncher,
                    screenTime = screenTimeText,
                ),
            colors = requireNotNull(homeColors) { "homeColors must be set in onViewCreated" },
            clock = clock,
            callbacks = requireNotNull(callbacks) { "callbacks must be set in onViewCreated" },
        )
    }

    private fun initObservers() {
        showFirstRunTips = prefs.firstSettingsOpen

        collectOnStart(viewModel.refreshHome) {
            populateHomeScreen()
        }
        collectOnStart(viewModel.isMinLauncherDefault) {
            if (it == false) {
                prefs.homeBottomAlignment = false
                setHomeAlignment()
            }
            if (showFirstRunTips) return@collectOnStart
            showSetDefaultLauncher = it == false && prefs.hideSetDefaultLauncher.not()
        }
        collectOnStart(viewModel.homeAppAlignment) {
            setHomeAlignment(it)
        }
        collectOnStart(viewModel.homeBottomAlignment) {
            setHomeAlignment()
        }
        collectOnStart(viewModel.toggleDateTime) {
            populateDateTime()
        }
        collectOnStart(viewModel.screenTimeValue) {
            it?.let { screenTimeText = it }
        }
    }

    private fun setHomeAlignment(horizontalGravity: Int = viewModel.homeAppAlignment.value) {
        homeAppAlignment = horizontalGravity
        homeBottomAlignment = viewModel.homeBottomAlignment.value
    }

    private fun populateDateTime() {
        dateTimeVisibility = prefs.dateTimeVisibility
        // Gates the battery suffix on the date line; re-read on every
        // populate like the old binding write.
        showStatusBar = prefs.showStatusBar
    }

    private fun populateScreenTime() {
        if (requireContext().appUsagePermissionGranted().not() || prefs.screenTimeEnabled.not()) {
            screenTimeText = null
            return
        }

        viewModel.getTodaysScreenTime()
        // The previous line stays until the flow delivers the new value, like
        // the old TextView whose text only the collector rewrote.
    }

    private fun populateHomeScreen() {
        populateDateTime()
        populateScreenTime()

        // (1..0) is empty, like the old early return for homeAppsNum == 0.
        slots =
            (1..prefs.homeAppsNum).map { location ->
                HomeSlotState(index = location, label = verifiedHomeAppLabel(location))
            }
    }

    /**
     * The old setHomeAppText: verifies the slot's app or pinned shortcut
     * still exists, clears the stored slot when it does not, and returns the
     * label to render ("" shows the hint).
     */
    private fun verifiedHomeAppLabel(location: Int): String {
        val (label, verified) =
            if (prefs.getIsShortcut(location)) verifiedShortcutLabel(location) else verifiedAppLabel(location)
        if (!verified) clearHomeApp(location)
        return label
    }

    private fun verifiedShortcutLabel(location: Int): Pair<String, Boolean> {
        val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        // Query for the specific shortcut
        val query =
            LauncherApps.ShortcutQuery().apply {
                setPackage(prefs.getAppPackage(location))
                setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }

        return try {
            val shortcuts =
                launcherApps.getShortcuts(query, getUserHandleFromString(requireContext(), prefs.getAppUser(location)))
            // Check if our shortcut still exists
            if (shortcuts?.any { it.id == prefs.getShortcutId(location) } == true) {
                prefs.getAppName(location) to true
            } else {
                "" to false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify home screen shortcut", e)
            "" to false
        }
    }

    private fun verifiedAppLabel(location: Int): Pair<String, Boolean> =
        prefs.getAppName(location) to
            isPackageInstalled(requireContext(), prefs.getAppPackage(location), prefs.getAppUser(location))

    /** The old setHomeAppText failure path: forget a slot whose app vanished. */
    private fun clearHomeApp(location: Int) {
        when (location) {
            Constants.FLAG_SET_HOME_APP_1 -> {
                prefs.appName1 = ""
                prefs.appPackage1 = ""
            }

            Constants.FLAG_SET_HOME_APP_2 -> {
                prefs.appName2 = ""
                prefs.appPackage2 = ""
            }

            Constants.FLAG_SET_HOME_APP_3 -> {
                prefs.appName3 = ""
                prefs.appPackage3 = ""
            }

            Constants.FLAG_SET_HOME_APP_4 -> {
                prefs.appName4 = ""
                prefs.appPackage4 = ""
            }

            Constants.FLAG_SET_HOME_APP_5 -> {
                prefs.appName5 = ""
                prefs.appPackage5 = ""
            }

            Constants.FLAG_SET_HOME_APP_6 -> {
                prefs.appName6 = ""
                prefs.appPackage6 = ""
            }

            Constants.FLAG_SET_HOME_APP_7 -> {
                prefs.appName7 = ""
                prefs.appPackage7 = ""
            }

            Constants.FLAG_SET_HOME_APP_8 -> {
                prefs.appName8 = ""
                prefs.appPackage8 = ""
            }
        }
    }

    private fun launchAppOrShortcut(
        appName: String,
        packageName: String,
        activityClassName: String?,
        shortcutId: String?,
        isShortcut: Boolean,
        userString: String,
        fallback: (() -> Unit)? = null,
    ) {
        if (appName.isEmpty()) {
            showLongPressToast()
            return
        }
        if (isShortcut && !shortcutId.isNullOrEmpty()) {
            launchShortcut(
                packageName = packageName,
                shortcutId = shortcutId,
                shortcutLabel = appName,
                userString = userString,
            )
        } else if (packageName.isNotEmpty()) {
            launchApp(
                appName = appName,
                packageName = packageName,
                activityClassName = activityClassName,
                userString = userString,
            )
        } else {
            fallback?.invoke()
        }
    }

    private fun launchShortcut(
        shortcutId: String,
        packageName: String,
        shortcutLabel: String,
        userString: String,
    ) {
        viewModel.selectedApp(
            AppModel.PinnedShortcut(
                shortcutId = shortcutId,
                appLabel = shortcutLabel,
                user = getUserHandleFromString(requireContext(), userString),
                key = null,
                appPackage = packageName,
                isNew = false,
            ),
            Constants.FLAG_LAUNCH_APP,
        )
    }

    private fun launchApp(
        appName: String,
        packageName: String,
        activityClassName: String?,
        userString: String,
    ) {
        viewModel.selectedApp(
            AppModel.App(
                appLabel = appName,
                key = null,
                appPackage = packageName,
                activityClassName = activityClassName,
                isNew = false,
                user = getUserHandleFromString(requireContext(), userString),
            ),
            Constants.FLAG_LAUNCH_APP,
        )
    }

    private fun homeAppClicked(location: Int) {
        launchAppOrShortcut(
            appName = prefs.getAppName(location),
            packageName = prefs.getAppPackage(location),
            activityClassName = prefs.getAppActivityClassName(location),
            shortcutId = prefs.getShortcutId(location),
            isShortcut = prefs.getIsShortcut(location),
            userString = prefs.getAppUser(location),
        )
    }

    private fun openSwipeRightApp() {
        if (!prefs.swipeRightEnabled) return
        launchAppOrShortcut(
            appName = prefs.appNameSwipeRight.ifEmpty { getString(R.string.swipe_right_app_default) },
            packageName = prefs.appPackageSwipeRight,
            activityClassName = prefs.appActivityClassNameRight,
            shortcutId = prefs.shortcutIdSwipeRight,
            isShortcut = prefs.isShortcutSwipeRight,
            userString = prefs.appUserSwipeRight,
            fallback = { openDialerApp(requireContext()) },
        )
    }

    private fun openSwipeLeftApp() {
        if (!prefs.swipeLeftEnabled) return
        launchAppOrShortcut(
            appName = prefs.appNameSwipeLeft.ifEmpty { getString(R.string.swipe_left_app_default) },
            packageName = prefs.appPackageSwipeLeft,
            activityClassName = prefs.appActivityClassNameSwipeLeft,
            shortcutId = prefs.shortcutIdSwipeLeft,
            isShortcut = prefs.isShortcutSwipeLeft,
            userString = prefs.appUserSwipeLeft,
            fallback = { openCameraApp(requireContext()) },
        )
    }

    private fun showAppList(
        flag: Int,
        rename: Boolean = false,
        includeHiddenApps: Boolean = false,
    ) {
        viewModel.getAppList(includeHiddenApps)
        val controller = findNavController()
        if (controller.currentDestination == null) {
            // The nav back stack was emptied by some earlier corruption; rebuild it
            // from the start destination first, otherwise the drawer would be added
            // as an orphan fragment with no way back to the home screen.
            controller.navigate(R.id.mainFragment)
        }
        when (controller.currentDestination?.id) {
            R.id.mainFragment ->
                controller.navigate(
                    R.id.action_mainFragment_to_appListFragment,
                    Bundle().apply {
                        putInt(Constants.Key.FLAG, flag)
                        putBoolean(Constants.Key.RENAME, rename)
                    },
                )

            R.id.appListFragment ->
                Log.w(
                    TAG,
                    "Ignoring app list request (flag=$flag, rename=$rename) " +
                        "while the app list is already open",
                )

            else ->
                Log.w(
                    TAG,
                    "Ignoring app list request from destination ${controller.currentDestination?.id}",
                )
        }
    }

    private fun showStatusBar() {
        requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
    }

    private fun hideStatusBar() {
        requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
    }

    private fun openScreenTimeDigitalWellbeing() {
        val intent = Intent()
        try {
            intent.setClassName(
                Constants.DIGITAL_WELLBEING_PACKAGE_NAME,
                Constants.DIGITAL_WELLBEING_ACTIVITY,
            )
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Digital Wellbeing app", e)
            try {
                intent.setClassName(
                    Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME,
                    Constants.DIGITAL_WELLBEING_SAMSUNG_ACTIVITY,
                )
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open Samsung Digital Wellbeing app", e)
            }
        }
    }

    private fun showLongPressToast() = requireContext().showToast(getString(R.string.long_press_to_select_app))

    private fun onSetDefaultLauncherLongPressed() {
        prefs.hideSetDefaultLauncher = true
        showSetDefaultLauncher = false
        if (viewModel.isMinLauncherDefault.value != true) {
            requireContext().showToast(R.string.set_as_default_launcher)
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
        }
    }

    private fun createCallbacks(): HomeCallbacks =
        HomeCallbacks(
            onSwipeLeft = ::openSwipeLeftApp,
            onSwipeRight = ::openSwipeRightApp,
            onSwipeUp = { showAppList(Constants.FLAG_LAUNCH_APP) },
            onSwipeDown = { expandNotificationDrawer(requireContext()) },
            onBackgroundLongPress = {
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to navigate to settings fragment", e)
                }
            },
            onBackgroundDoubleTap = {
                if (prefs.lockModeOn) {
                    // Preserve the system click feedback the old lock tap played.
                    binding.root.playSoundEffect(SoundEffectConstants.CLICK)
                    LockRequest.send(requireContext())
                }
            },
            onSlotTap = ::homeAppClicked,
            // The slot number doubles as Constants.FLAG_SET_HOME_APP_n (1..8).
            onSlotLongPress = { location ->
                showAppList(location, prefs.getAppName(location).isNotEmpty(), true)
            },
            onClockTap = { openClock(requireContext()) },
            onDateTap = { openCalendar(requireContext()) },
            onScreenTimeTap = ::openScreenTimeDigitalWellbeing,
            onSetDefaultLauncherTap = { viewModel.requestResetLauncher() },
            onSetDefaultLauncherLongPress = ::onSetDefaultLauncherLongPressed,
        )

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }
}
