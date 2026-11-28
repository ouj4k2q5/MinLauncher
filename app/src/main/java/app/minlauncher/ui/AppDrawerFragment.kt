package app.minlauncher.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import app.minlauncher.DrawerViewModel
import app.minlauncher.LauncherApp
import app.minlauncher.R
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.helper.deletePinnedShortcut
import app.minlauncher.helper.hideKeyboard
import app.minlauncher.helper.isSystemApp
import app.minlauncher.helper.openAppInfo
import app.minlauncher.helper.openSearch
import app.minlauncher.helper.openUrl
import app.minlauncher.helper.showToast
import app.minlauncher.helper.uninstall
import app.minlauncher.ui.drawer.DrawerCallbacks
import app.minlauncher.ui.drawer.DrawerScreenHost
import app.minlauncher.ui.drawer.resolveDrawerColors

// One small method per drawer action (the same layout as the old callback
// lambdas, split by side effect); the count is inherent to binding the drawer's
// interactions to the Fragment, not a sign of a class doing too many things.
@Suppress("TooManyFunctions")
class AppDrawerFragment : BaseFragment() {
    private lateinit var prefs: Prefs

    private var flag = Constants.FLAG_LAUNCH_APP
    private var canRename = false
    private var currentAppList: List<AppModel>? = null
    private var currentPrivateSpaceApps: List<AppModel>? = null
    private var currentPrivateSpaceLocked = true
    private var currentPrivateSpaceAvailable = false
    private var cachedIsCjkKeyboard: Boolean? = null

    // The combined list for the current flag (apps + the Private Space header
    // and its apps while unlocked), kept as Compose state so the screen
    // recomposes; onAppHide also updates it optimistically, replacing the old
    // adapter.removeApp.
    private var combinedList by mutableStateOf<List<AppModel>>(emptyList())

    // Bumped from onStart so the screen re-shows the keyboard like the old
    // onStart listener did, including across activity stop/start cycles.
    private var keyboardShowSignal by mutableIntStateOf(0)

    // Explicit factory: under Robolectric the default factory's Application
    // fallback can be stale across tests, so read the repository from the
    // current activity's app. In production the activity's app is the one
    // and only LauncherApp instance.
    private val viewModel: DrawerViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application
                DrawerViewModel(app, (app as LauncherApp).launcherRepository)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        prefs = Prefs(requireContext())
        arguments?.let {
            flag = it.getInt(Constants.Key.FLAG, Constants.FLAG_LAUNCH_APP)
            canRename = it.getBoolean(Constants.Key.RENAME, false)
        }

        val colors = requireActivity().resolveDrawerColors()
        val callbacks = createCallbacks()
        val searchHint =
            when {
                flag == Constants.FLAG_HIDDEN_APPS -> getString(R.string.hidden_apps)
                flag in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_SWIPE_RIGHT_APP ->
                    getString(R.string.select_app_hint)
                else -> "___"
            }

        return ComposeView(requireContext()).apply {
            setBackgroundColor(colors.shadeDark)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DrawerScreenHost(
                    colors = colors,
                    callbacks = callbacks,
                    apps = combinedList,
                    flag = flag,
                    searchHint = searchHint,
                    canRename = canRename,
                    appLabelAlignment = prefs.appLabelAlignment,
                    autoShowKeyboard = prefs.autoShowKeyboard,
                    showKeyboardSignal = keyboardShowSignal,
                )
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
    }

    private fun initObservers() {
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            collectOnStart(viewModel.hiddenApps) {
                it?.let { combinedList = it }
            }
        } else {
            collectOnStart(viewModel.appList) {
                currentAppList = it
                updateCombinedAppList()
            }
            if (flag == Constants.FLAG_LAUNCH_APP) {
                collectOnStart(viewModel.privateSpaceAvailable) {
                    currentPrivateSpaceAvailable = it
                    updateCombinedAppList()
                }
                collectOnStart(viewModel.privateSpaceLocked) {
                    currentPrivateSpaceLocked = it
                    updateCombinedAppList()
                }
                collectOnStart(viewModel.privateSpaceApps) {
                    currentPrivateSpaceApps = it
                    updateCombinedAppList()
                }
            }
        }
    }

    private fun updateCombinedAppList() {
        val apps = currentAppList ?: return
        val combined = apps.toMutableList()

        if (flag == Constants.FLAG_LAUNCH_APP && currentPrivateSpaceAvailable) {
            combined.add(AppModel.PrivateSpaceHeader(isLocked = currentPrivateSpaceLocked))
            if (!currentPrivateSpaceLocked) {
                currentPrivateSpaceApps?.let { combined.addAll(it) }
            }
        }

        combinedList = combined
    }

    private fun createCallbacks(): DrawerCallbacks =
        DrawerCallbacks(
            onAppClick = ::onAppClicked,
            onAppInfo = ::onAppInfoOpened,
            onAppDelete = ::onAppDeleted,
            onAppHide = ::onAppHidden,
            onAppRename = ::onAppRenamed,
            onRenameHomeSlot = ::renameHomeSlot,
            onPrivateSpaceToggle = viewModel::togglePrivateSpaceLock,
            onPrivateSpaceSettings = ::openPrivateSpaceSettings,
            onExitDrawer = ::exitDrawer,
            onBangSearch = ::openBangSearch,
            onOpenSearch = ::openSearch,
            isCjkKeyboard = ::isCjkKeyboard,
        )

    private fun onAppClicked(appModel: AppModel) {
        viewModel.selectedApp(appModel, flag)
        if (flag == Constants.FLAG_LAUNCH_APP || flag == Constants.FLAG_HIDDEN_APPS) {
            findNavController().popBackStack(R.id.mainFragment, false)
        } else {
            exitDrawer()
        }
    }

    private fun onAppInfoOpened(appModel: AppModel) {
        openAppInfo(
            requireContext(),
            appModel.user,
            appModel.appPackage,
        )
        findNavController().popBackStack(R.id.mainFragment, false)
    }

    private fun onAppDeleted(appModel: AppModel) {
        when (appModel) {
            is AppModel.PrivateSpaceHeader -> {}
            is AppModel.PinnedShortcut ->
                requireContext().deletePinnedShortcut(
                    packageName = appModel.appPackage,
                    shortcutIdToDelete = appModel.shortcutId,
                    user = appModel.user,
                )

            is AppModel.App -> {
                if (appModel.user != Process.myUserHandle()) {
                    openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                } else if (requireContext().isSystemApp(appModel.appPackage, appModel.user)) {
                    requireContext().showToast(getString(R.string.system_app_cannot_delete))
                    openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                } else {
                    requireContext().uninstall(appModel.appPackage)
                }
            }
        }
        viewModel.getAppList()
    }

    private fun onAppHidden(appModel: AppModel) {
        if (appModel is AppModel.PinnedShortcut) {
            requireContext().showToast(getString(R.string.hide_pinned_shortcut_not_supported))
            return
        }
        // Optimistic removal, replacing the old adapter.removeApp.
        combinedList = combinedList.filterNot { it == appModel }

        val newSet = mutableSetOf<String>()
        newSet.addAll(prefs.hiddenApps)
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
        } else {
            newSet.add(appModel.appPackage + "|" + appModel.user.toString())
        }

        prefs.hiddenApps = newSet
        if (newSet.isEmpty()) {
            exitDrawer()
        }
        if (prefs.firstHide) {
            view?.hideKeyboard()
            prefs.firstHide = false
            viewModel.postDialog(Constants.Dialog.HIDDEN)
            // The drawer may have just been popped above; navigate only while
            // it is still the current destination, otherwise the action cannot
            // be resolved from the new current destination.
            if (findNavController().currentDestination?.id == R.id.appListFragment) {
                findNavController().navigate(R.id.action_appListFragment_to_settingsFragment2)
            }
        }
        viewModel.getAppList()
        viewModel.getHiddenApps()
    }

    private fun onAppRenamed(
        appModel: AppModel,
        renameLabel: String,
    ) {
        val identifier =
            when (appModel) {
                is AppModel.PinnedShortcut -> appModel.identity
                is AppModel.App -> appModel.appPackage
                else -> return
            }
        prefs.setAppRenameLabel(identifier, renameLabel)
        viewModel.getAppList()
    }

    /** Saves the typed name to the home slot being customized; false means the name was empty. */
    private fun renameHomeSlot(name: String): Boolean {
        if (name.isEmpty()) {
            requireContext().showToast(getString(R.string.type_a_new_app_name_first))
            return false
        }
        when (flag) {
            Constants.FLAG_SET_HOME_APP_1 -> prefs.appName1 = name
            Constants.FLAG_SET_HOME_APP_2 -> prefs.appName2 = name
            Constants.FLAG_SET_HOME_APP_3 -> prefs.appName3 = name
            Constants.FLAG_SET_HOME_APP_4 -> prefs.appName4 = name
            Constants.FLAG_SET_HOME_APP_5 -> prefs.appName5 = name
            Constants.FLAG_SET_HOME_APP_6 -> prefs.appName6 = name
            Constants.FLAG_SET_HOME_APP_7 -> prefs.appName7 = name
            Constants.FLAG_SET_HOME_APP_8 -> prefs.appName8 = name
        }
        exitDrawer()
        return true
    }

    private fun openPrivateSpaceSettings() {
        viewModel.openPrivateSpaceSettings()
        findNavController().popBackStack(R.id.mainFragment, false)
    }

    private fun openBangSearch(query: String) {
        requireContext().openUrl(Constants.URL_DUCK_SEARCH + Uri.encode(query))
    }

    private fun openSearch(query: String?) {
        requireContext().openSearch(query)
    }

    private fun isCjkKeyboard(): Boolean {
        cachedIsCjkKeyboard?.let { return it }

        val result =
            runCatching {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val subtype = imm?.currentInputMethodSubtype ?: return@runCatching false

                val language =
                    subtype.languageTag.ifEmpty {
                        @Suppress("DEPRECATION")
                        subtype.locale
                    }

                language.startsWith("zh") || language.startsWith("ja") || language.startsWith("ko")
            }.getOrElse { false }

        cachedIsCjkKeyboard = result
        return result
    }

    private fun exitDrawer() {
        // Pop only while the drawer is still the current destination: popBackStack()
        // updates the nav back stack synchronously, so a second invocation from the
        // same gesture (a later MOVE event of one drag, or a double tap during the
        // exit animation) becomes a no-op instead of popping further entries and
        // corrupting the back stack.
        val controller = findNavController()
        if (controller.currentDestination?.id == R.id.appListFragment) {
            controller.popBackStack()
        }
    }

    override fun onStart() {
        super.onStart()
        cachedIsCjkKeyboard = null
        keyboardShowSignal++
    }

    override fun onStop() {
        view?.hideKeyboard()
        super.onStop()
    }
}
