package app.minlauncher.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import app.minlauncher.HomeViewModel
import app.minlauncher.LauncherApp
import app.minlauncher.R
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.databinding.FragmentHomeBinding
import app.minlauncher.helper.LockRequest
import app.minlauncher.helper.appUsagePermissionGranted
import app.minlauncher.helper.expandNotificationDrawer
import app.minlauncher.helper.openCalendar
import app.minlauncher.helper.openClock
import app.minlauncher.helper.showToast
import app.minlauncher.ui.home.HomeCallbacks
import app.minlauncher.ui.home.HomeClockSource
import app.minlauncher.ui.home.HomeColors
import app.minlauncher.ui.home.HomeScreenHost
import app.minlauncher.ui.home.HomeSlotState
import app.minlauncher.ui.home.homeAppClicked
import app.minlauncher.ui.home.openSwipeLeftApp
import app.minlauncher.ui.home.openSwipeRightApp
import app.minlauncher.ui.home.resolveHomeColors
import app.minlauncher.ui.home.verifiedHomeAppLabel
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
    // Activity-scoped so the permanent home screen's state survives fragment
    // view recreation (Stage 2 removes the fragment entirely).
    private val viewModel: HomeViewModel by activityViewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application
                HomeViewModel(app, (app as LauncherApp).launcherRepository)
            }
        }
    }

    private var viewBinding: FragmentHomeBinding? = null
    private val binding get() = viewBinding!!

    // Resolved once for the whole screen lifetime; theme switches recreate.
    private var homeColors: HomeColors? = null
    private var callbacks: HomeCallbacks? = null

    companion object {
        /**
         * Test seam: delegation alias to [HomeClockSource], kept so existing
         * tests referencing `HomeFragment.clock` keep compiling while the
         * seam moves next to the home UI.
         */
        internal var clock: Clock
            get() = HomeClockSource.clock
            set(value) {
                HomeClockSource.clock = value
            }
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
            setContent {
                HomeScreenHost(
                    viewModel = viewModel,
                    colors = requireNotNull(homeColors) { "homeColors must be set in onViewCreated" },
                    callbacks = requireNotNull(callbacks) { "callbacks must be set in onViewCreated" },
                )
            }
        }

        initObservers()
        setHomeAlignment(viewModel.homeAppAlignmentFlow.value)
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

    private fun initObservers() {
        viewModel.showFirstRunTips = prefs.firstSettingsOpen

        collectOnStart(viewModel.refreshHome) {
            populateHomeScreen()
        }
        collectOnStart(viewModel.isMinLauncherDefault) {
            if (it == false) {
                prefs.homeBottomAlignment = false
                setHomeAlignment()
            }
            if (viewModel.showFirstRunTips) return@collectOnStart
            viewModel.showSetDefaultLauncher = it == false && prefs.hideSetDefaultLauncher.not()
        }
        collectOnStart(viewModel.homeAppAlignmentFlow) {
            setHomeAlignment(it)
        }
        collectOnStart(viewModel.homeBottomAlignmentFlow) {
            setHomeAlignment()
        }
        collectOnStart(viewModel.toggleDateTime) {
            populateDateTime()
        }
        collectOnStart(viewModel.screenTimeValue) {
            it?.let { viewModel.screenTimeText = it }
        }
    }

    private fun setHomeAlignment(horizontalGravity: Int = viewModel.homeAppAlignmentFlow.value) {
        viewModel.homeAppAlignment = horizontalGravity
        viewModel.homeBottomAlignment = viewModel.homeBottomAlignmentFlow.value
    }

    private fun populateDateTime() {
        viewModel.dateTimeVisibility = prefs.dateTimeVisibility
        // Gates the battery suffix on the date line; re-read on every
        // populate like the old binding write.
        viewModel.showStatusBar = prefs.showStatusBar
    }

    private fun populateScreenTime() {
        if (requireContext().appUsagePermissionGranted().not() || prefs.screenTimeEnabled.not()) {
            viewModel.screenTimeText = null
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
        viewModel.slots =
            (1..prefs.homeAppsNum).map { location ->
                HomeSlotState(
                    index = location,
                    label = verifiedHomeAppLabel(requireContext(), prefs, location),
                )
            }
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

    private fun onSetDefaultLauncherLongPressed() {
        prefs.hideSetDefaultLauncher = true
        viewModel.showSetDefaultLauncher = false
        if (viewModel.isMinLauncherDefault.value != true) {
            requireContext().showToast(R.string.set_as_default_launcher)
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
        }
    }

    private fun createCallbacks(): HomeCallbacks =
        HomeCallbacks(
            onSwipeLeft = { openSwipeLeftApp(requireContext(), prefs, viewModel) },
            onSwipeRight = { openSwipeRightApp(requireContext(), prefs, viewModel) },
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
            onSlotTap = { location -> homeAppClicked(requireContext(), prefs, viewModel, location) },
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
