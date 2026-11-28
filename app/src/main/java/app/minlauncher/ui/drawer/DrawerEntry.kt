// One small function per drawer action/observer (the same layout as the old
// Fragment's callbacks, split by side effect); the count is inherent to binding
// the drawer, not a sign of a file doing too many things.
@file:Suppress("TooManyFunctions")

package app.minlauncher.ui.drawer

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavBackStack
import app.minlauncher.DrawerViewModel
import app.minlauncher.R
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.data.LauncherRepository
import app.minlauncher.data.Prefs
import app.minlauncher.helper.openAppInfo
import app.minlauncher.helper.openSearch
import app.minlauncher.helper.openUrl
import app.minlauncher.helper.showToast
import app.minlauncher.ui.nav.DrawerKey
import app.minlauncher.ui.nav.MinLauncherNavKey
import app.minlauncher.ui.nav.SettingsKey

/**
 * The app drawer's Navigation 3 entry. The drawer is transient, so its
 * combined list and keyboard signal live in a per-entry [DrawerEntryState]
 * (remembered, not the ViewModel): leaving the drawer disposes the entry and
 * resets them, exactly like the fragment's fields.
 */
@Composable
internal fun DrawerEntry(
    repository: LauncherRepository,
    key: DrawerKey,
    backStack: NavBackStack<MinLauncherNavKey>,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val application = context.applicationContext as Application
    val prefs = remember(context) { Prefs(context) }
    val viewModel: DrawerViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer { DrawerViewModel(application, repository) }
                },
        )
    val colors = remember(activity) { activity.resolveDrawerColors() }
    val state = remember { DrawerEntryState() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val deps =
        remember(key, prefs, viewModel, backStack, keyboard, focusManager) {
            DrawerEntryDeps(
                context = context,
                prefs = prefs,
                viewModel = viewModel,
                flag = key.flag,
                backStack = backStack,
                state = state,
                hideKeyboard = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                },
            )
        }

    DrawerStartStopParity(activity, deps)
    drawerObservers(deps)

    val callbacks = remember(deps) { createDrawerCallbacks(deps) }
    DrawerScreenHost(
        colors = colors,
        callbacks = callbacks,
        apps = state.combinedList,
        flag = key.flag,
        searchHint = searchHint(context, key.flag),
        canRename = key.canRename,
        appLabelAlignment = prefs.appLabelAlignment,
        autoShowKeyboard = prefs.autoShowKeyboard,
        showKeyboardSignal = state.keyboardShowSignal,
    )
}

private class DrawerEntryState {
    var currentAppList by mutableStateOf<List<AppModel>?>(null)
    var currentPrivateSpaceApps by mutableStateOf<List<AppModel>?>(null)
    var currentPrivateSpaceLocked by mutableStateOf(true)
    var currentPrivateSpaceAvailable by mutableStateOf(false)
    var cachedIsCjkKeyboard by mutableStateOf<Boolean?>(null)

    // The combined list for the current flag (apps + the Private Space header
    // and its apps while unlocked), kept as Compose state so the screen
    // recomposes; onAppHide also updates it optimistically.
    var combinedList by mutableStateOf<List<AppModel>>(emptyList())

    // Bumped on entry so the screen re-shows the keyboard like the old
    // onStart listener did.
    var keyboardShowSignal by mutableIntStateOf(0)
}

@Suppress("LongParameterList") // one snapshot of the entry's immutable inputs plus its transient state
private class DrawerEntryDeps(
    val context: Context,
    val prefs: Prefs,
    val viewModel: DrawerViewModel,
    val flag: Int,
    val backStack: NavBackStack<MinLauncherNavKey>,
    val state: DrawerEntryState,
    val hideKeyboard: () -> Unit,
)

/**
 * The old fragment's onStart/onStop. The entry composition covers the drawer's
 * own opening, but it survives an activity stop/start, and a drawer can still
 * be on top across that cycle (the private-space settings round trip skips
 * backToHomeScreen). So the CJK cache reset and keyboard-show bump also ride the
 * activity lifecycle: START resets them, STOP hides the keyboard.
 *
 * There is deliberately no separate composition-time bump: adding an observer
 * to a STARTED/RESUMED activity replays every state event up to the current
 * state, so registration itself delivers an ON_START (and a CREATED activity
 * delivers the real one when it starts). Bumping here as well would increment
 * keyboardShowSignal twice per drawer open.
 */
@Composable
private fun DrawerStartStopParity(
    activity: Activity,
    deps: DrawerEntryDeps,
) {
    val state = deps.state
    DisposableEffect(activity, deps) {
        val activityLifecycle = (activity as LifecycleOwner).lifecycle
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        state.cachedIsCjkKeyboard = null
                        state.keyboardShowSignal++
                    }

                    Lifecycle.Event.ON_STOP -> deps.hideKeyboard()
                    else -> Unit
                }
            }
        activityLifecycle.addObserver(observer)
        onDispose {
            activityLifecycle.removeObserver(observer)
            deps.hideKeyboard()
        }
    }
}

@Composable
private fun drawerObservers(deps: DrawerEntryDeps) {
    val viewModel = deps.viewModel
    val flag = deps.flag
    if (flag == Constants.FLAG_HIDDEN_APPS) {
        LaunchedEffect(viewModel, flag) {
            viewModel.hiddenApps.collect { hidden ->
                hidden?.let { deps.state.combinedList = it }
            }
        }
        return
    }
    LaunchedEffect(viewModel, flag) {
        viewModel.appList.collect { apps ->
            deps.state.currentAppList = apps
            updateCombinedAppList(deps)
        }
    }
    if (flag != Constants.FLAG_LAUNCH_APP) return
    LaunchedEffect(viewModel, flag) {
        viewModel.privateSpaceAvailable.collect {
            deps.state.currentPrivateSpaceAvailable = it
            updateCombinedAppList(deps)
        }
    }
    LaunchedEffect(viewModel, flag) {
        viewModel.privateSpaceLocked.collect {
            deps.state.currentPrivateSpaceLocked = it
            updateCombinedAppList(deps)
        }
    }
    LaunchedEffect(viewModel, flag) {
        viewModel.privateSpaceApps.collect {
            deps.state.currentPrivateSpaceApps = it
            updateCombinedAppList(deps)
        }
    }
}

private fun updateCombinedAppList(deps: DrawerEntryDeps) {
    val state = deps.state
    updateCombinedAppList(
        flag = deps.flag,
        currentAppList = state.currentAppList,
        privateSpaceAvailable = state.currentPrivateSpaceAvailable,
        privateSpaceLocked = state.currentPrivateSpaceLocked,
        privateSpaceApps = state.currentPrivateSpaceApps,
    )?.let { state.combinedList = it }
}

private fun searchHint(
    context: Context,
    flag: Int,
): String =
    when {
        flag == Constants.FLAG_HIDDEN_APPS -> context.getString(R.string.hidden_apps)
        flag in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_SWIPE_RIGHT_APP ->
            context.getString(R.string.select_app_hint)

        else -> "___"
    }

private fun createDrawerCallbacks(deps: DrawerEntryDeps): DrawerCallbacks {
    val context = deps.context
    return DrawerCallbacks(
        onAppClick = { appModel -> onAppClicked(deps, appModel) },
        onAppInfo = { appModel ->
            openAppInfo(context, appModel.user, appModel.appPackage)
            collapseToHome(deps)
        },
        onAppDelete = { appModel -> onAppDeleted(context, deps.viewModel, appModel) },
        onAppHide = { appModel -> onAppHidden(deps, appModel) },
        onAppRename = { appModel, label -> onAppRenamed(deps, appModel, label) },
        onRenameHomeSlot = { name -> renameHomeSlot(deps, name) },
        onPrivateSpaceToggle = deps.viewModel::togglePrivateSpaceLock,
        onPrivateSpaceSettings = {
            deps.viewModel.openPrivateSpaceSettings()
            collapseToHome(deps)
        },
        onExitDrawer = { exitDrawer(deps) },
        onBangSearch = { query -> context.openUrl(Constants.URL_DUCK_SEARCH + Uri.encode(query)) },
        onOpenSearch = { query -> context.openSearch(query) },
        isCjkKeyboard = { isCjkKeyboard(deps) },
    )
}

private fun onAppClicked(
    deps: DrawerEntryDeps,
    appModel: AppModel,
) {
    deps.viewModel.selectedApp(appModel, deps.flag)
    if (deps.flag == Constants.FLAG_LAUNCH_APP || deps.flag == Constants.FLAG_HIDDEN_APPS) {
        collapseToHome(deps)
    } else {
        exitDrawer(deps)
    }
}

private fun onAppHidden(
    deps: DrawerEntryDeps,
    appModel: AppModel,
) {
    val context = deps.context
    if (appModel is AppModel.PinnedShortcut) {
        context.showToast(R.string.hide_pinned_shortcut_not_supported)
        return
    }
    // Optimistic removal, replacing the old adapter.removeApp.
    deps.state.combinedList = deps.state.combinedList.filterNot { it == appModel }

    val newSet = mutableSetOf<String>()
    newSet.addAll(deps.prefs.hiddenApps)
    if (deps.flag == Constants.FLAG_HIDDEN_APPS) {
        newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
    } else {
        newSet.add(appModel.appPackage + "|" + appModel.user.toString())
    }

    deps.prefs.hiddenApps = newSet
    if (newSet.isEmpty()) {
        exitDrawer(deps)
    }
    if (deps.prefs.firstHide) {
        deps.hideKeyboard()
        deps.prefs.firstHide = false
        deps.viewModel.postDialog(Constants.Dialog.HIDDEN)
        // Replace the drawer with settings only while it is still showing: an
        // empty hidden list already popped it above. The drawer may itself
        // sit on top of settings (hidden apps, swipe-app rows); reuse that
        // entry instead of stacking a second settings on top of it.
        if (deps.backStack.lastOrNull() is DrawerKey) {
            deps.backStack.removeLastOrNull()
            if (deps.backStack.lastOrNull() !is SettingsKey) {
                deps.backStack.add(SettingsKey)
            }
        }
    }
    deps.viewModel.getAppList()
    deps.viewModel.getHiddenApps()
}

private fun onAppRenamed(
    deps: DrawerEntryDeps,
    appModel: AppModel,
    renameLabel: String,
) {
    val identifier =
        when (appModel) {
            is AppModel.PinnedShortcut -> appModel.identity
            is AppModel.App -> appModel.appPackage
            else -> return
        }
    deps.prefs.setAppRenameLabel(identifier, renameLabel)
    deps.viewModel.getAppList()
}

/** Saves the typed name to the home slot being customized; false means the name was empty. */
private fun renameHomeSlot(
    deps: DrawerEntryDeps,
    name: String,
): Boolean {
    val renamed =
        renameHomeSlot(
            context = deps.context,
            prefs = deps.prefs,
            flag = deps.flag,
            name = name,
        )
    if (renamed) exitDrawer(deps)
    return renamed
}

private fun isCjkKeyboard(deps: DrawerEntryDeps): Boolean {
    deps.state.cachedIsCjkKeyboard?.let { return it }
    val result = isCjkKeyboard(deps.context)
    deps.state.cachedIsCjkKeyboard = result
    return result
}

private fun collapseToHome(deps: DrawerEntryDeps) {
    while (deps.backStack.size > 1) {
        deps.backStack.removeLastOrNull()
    }
}

private fun exitDrawer(deps: DrawerEntryDeps) {
    // Pop only while the drawer is still on top: a second invocation from the
    // same gesture becomes a no-op instead of popping further entries.
    if (deps.backStack.lastOrNull() is DrawerKey) {
        deps.backStack.removeLastOrNull()
    }
}
