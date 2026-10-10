package app.minlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import app.minlauncher.R
import app.minlauncher.helper.AppListProvider
import app.minlauncher.helper.getPrivateSpaceApps
import app.minlauncher.helper.getPrivateSpaceUserHandle
import app.minlauncher.helper.isMinLauncherDefault
import app.minlauncher.helper.isPrivateSpaceLocked
import app.minlauncher.helper.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private const val TAG = "LauncherRepository"

/**
 * Shared-state holder for the launcher's screens. Owns the StateFlow backing
 * fields and the state-mutating functions; MainActivity and the screen
 * ViewModels (HomeViewModel, DrawerViewModel, SettingsViewModel) delegate to
 * it.
 */
class LauncherRepository(
    private val appContext: Context,
    private val appListProvider: AppListProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val prefs = Prefs(appContext)

    val refreshHome = MutableStateFlow<Boolean>(false)
    val toggleDateTime = MutableStateFlow<Unit?>(null)
    val updateSwipeApps = MutableStateFlow<Any?>(null)
    val appList = MutableStateFlow<List<AppModel>?>(null)
    val hiddenApps = MutableStateFlow<List<AppModel>?>(null)
    val isMinLauncherDefault = MutableStateFlow<Boolean>(false)
    val homeAppAlignment = MutableStateFlow(prefs.homeAlignment)

    val privateSpaceApps = MutableStateFlow<List<AppModel>?>(null)
    val privateSpaceLocked = MutableStateFlow<Boolean>(true)
    val privateSpaceAvailable = MutableStateFlow<Boolean>(false)

    // Suppress backToHomeScreen during Private Space lock/unlock auth
    var isPrivateSpaceToggling = false

    private val _showDialog = Channel<String>(Channel.BUFFERED)
    val showDialog = _showDialog.receiveAsFlow()

    private val _resetLauncher = Channel<Unit>(Channel.BUFFERED)
    val resetLauncher = _resetLauncher.receiveAsFlow()

    fun postDialog(dialog: String) {
        _showDialog.trySend(dialog)
    }

    fun requestResetLauncher() {
        _resetLauncher.trySend(Unit)
    }

    fun getAppList(includeHiddenApps: Boolean = false) {
        scope.launch {
            val apps =
                appListProvider.getAppsList(appContext, prefs, includeRegularApps = true, includeHiddenApps)
            appList.value = apps
        }
        getPrivateSpaceAppList()
    }

    fun getHiddenApps() {
        scope.launch {
            hiddenApps.value =
                appListProvider.getAppsList(appContext, prefs, includeRegularApps = false, includeHiddenApps = true)
        }
    }

    fun isMinLauncherDefault() {
        isMinLauncherDefault.value = isMinLauncherDefault(appContext)
    }

    fun updateHomeAlignment(gravity: Int) {
        prefs.homeAlignment = gravity
        homeAppAlignment.value = prefs.homeAlignment
    }

    fun refreshHome(appCountUpdated: Boolean) {
        refreshHome.value = appCountUpdated
    }

    fun toggleDateTime() {
        toggleDateTime.value = Unit
    }

    fun updateSwipeApps() {
        updateSwipeApps.value = Unit
    }

    fun getPrivateSpaceAppList() {
        scope.launch {
            val handle = getPrivateSpaceUserHandle(appContext)
            privateSpaceAvailable.value = handle != null
            if (handle != null) {
                privateSpaceLocked.value = isPrivateSpaceLocked(appContext, handle)
                privateSpaceApps.value = getPrivateSpaceApps(appContext, prefs)
            } else {
                privateSpaceLocked.value = true
                privateSpaceApps.value = emptyList()
            }
        }
    }

    fun togglePrivateSpaceLock() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val handle = getPrivateSpaceUserHandle(appContext) ?: return
        try {
            isPrivateSpaceToggling = true
            val userManager = appContext.getSystemService(Context.USER_SERVICE) as UserManager
            val currentlyLocked = userManager.isQuietModeEnabled(handle)
            userManager.requestQuietModeEnabled(!currentlyLocked, handle)
        } catch (e: Exception) {
            isPrivateSpaceToggling = false
            Log.e(TAG, "Failed to toggle private space lock", e)
        }
    }

    fun selectedApp(
        appModel: AppModel,
        flag: Int,
    ) {
        if (appModel is AppModel.PrivateSpaceHeader) return
        when (flag) {
            Constants.FLAG_LAUNCH_APP -> {
                when (appModel) {
                    is AppModel.PinnedShortcut -> launchShortcut(appModel)
                    is AppModel.App ->
                        launchApp(appModel.appPackage, appModel.activityClassName, appModel.user)

                    else -> {}
                }
            }

            Constants.FLAG_HIDDEN_APPS -> {
                if (appModel is AppModel.App) {
                    launchApp(appModel.appPackage, appModel.activityClassName, appModel.user)
                }
            }

            Constants.FLAG_SET_HOME_APP_1 -> saveHomeApp(appModel, 1)
            Constants.FLAG_SET_HOME_APP_2 -> saveHomeApp(appModel, 2)
            Constants.FLAG_SET_HOME_APP_3 -> saveHomeApp(appModel, 3)
            Constants.FLAG_SET_HOME_APP_4 -> saveHomeApp(appModel, 4)
            Constants.FLAG_SET_HOME_APP_5 -> saveHomeApp(appModel, 5)
            Constants.FLAG_SET_HOME_APP_6 -> saveHomeApp(appModel, 6)
            Constants.FLAG_SET_HOME_APP_7 -> saveHomeApp(appModel, 7)
            Constants.FLAG_SET_HOME_APP_8 -> saveHomeApp(appModel, 8)

            Constants.FLAG_SET_SWIPE_LEFT_APP -> saveSwipeApp(appModel, isLeft = true)
            Constants.FLAG_SET_SWIPE_RIGHT_APP -> saveSwipeApp(appModel, isLeft = false)
        }
    }

    private fun launchShortcut(appModel: AppModel.PinnedShortcut) {
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val query =
            LauncherApps.ShortcutQuery().apply {
                setPackage(appModel.appPackage)
                setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }
        try {
            val shortcut =
                launcher
                    .getShortcuts(query, appModel.user)
                    ?.find { it.id == appModel.shortcutId }
            if (shortcut == null) {
                appContext.showToast(appContext.getString(R.string.shortcut_not_found))
                return
            }
            launcher.startShortcut(shortcut, null, null)
        } catch (_: Exception) {
            appContext.showToast(appContext.getString(R.string.unable_to_open_shortcut))
        }
    }

    private fun saveHomeApp(
        appModel: AppModel,
        position: Int,
    ) {
        when (appModel) {
            is AppModel.PrivateSpaceHeader -> return
            is AppModel.App -> {
                when (position) {
                    1 -> {
                        prefs.appName1 = appModel.appLabel
                        prefs.appPackage1 = appModel.appPackage
                        prefs.appUser1 = appModel.user.toString()
                        prefs.appActivityClassName1 = appModel.activityClassName
                        prefs.isShortcut1 = false
                        prefs.shortcutId1 = ""
                    }

                    2 -> {
                        prefs.appName2 = appModel.appLabel
                        prefs.appPackage2 = appModel.appPackage
                        prefs.appUser2 = appModel.user.toString()
                        prefs.appActivityClassName2 = appModel.activityClassName
                        prefs.isShortcut2 = false
                        prefs.shortcutId2 = ""
                    }

                    3 -> {
                        prefs.appName3 = appModel.appLabel
                        prefs.appPackage3 = appModel.appPackage
                        prefs.appUser3 = appModel.user.toString()
                        prefs.appActivityClassName3 = appModel.activityClassName
                        prefs.isShortcut3 = false
                        prefs.shortcutId3 = ""
                    }

                    4 -> {
                        prefs.appName4 = appModel.appLabel
                        prefs.appPackage4 = appModel.appPackage
                        prefs.appUser4 = appModel.user.toString()
                        prefs.appActivityClassName4 = appModel.activityClassName
                        prefs.isShortcut4 = false
                        prefs.shortcutId4 = ""
                    }

                    5 -> {
                        prefs.appName5 = appModel.appLabel
                        prefs.appPackage5 = appModel.appPackage
                        prefs.appUser5 = appModel.user.toString()
                        prefs.appActivityClassName5 = appModel.activityClassName
                        prefs.isShortcut5 = false
                        prefs.shortcutId5 = ""
                    }

                    6 -> {
                        prefs.appName6 = appModel.appLabel
                        prefs.appPackage6 = appModel.appPackage
                        prefs.appUser6 = appModel.user.toString()
                        prefs.appActivityClassName6 = appModel.activityClassName
                        prefs.isShortcut6 = false
                        prefs.shortcutId6 = ""
                    }

                    7 -> {
                        prefs.appName7 = appModel.appLabel
                        prefs.appPackage7 = appModel.appPackage
                        prefs.appUser7 = appModel.user.toString()
                        prefs.appActivityClassName7 = appModel.activityClassName
                        prefs.isShortcut7 = false
                        prefs.shortcutId7 = ""
                    }

                    8 -> {
                        prefs.appName8 = appModel.appLabel
                        prefs.appPackage8 = appModel.appPackage
                        prefs.appUser8 = appModel.user.toString()
                        prefs.appActivityClassName8 = appModel.activityClassName
                        prefs.isShortcut8 = false
                        prefs.shortcutId8 = ""
                    }
                }
            }

            is AppModel.PinnedShortcut -> {
                when (position) {
                    1 -> {
                        prefs.appName1 = appModel.appLabel
                        prefs.appPackage1 = appModel.appPackage
                        prefs.appUser1 = appModel.user.toString()
                        prefs.appActivityClassName1 = null
                        prefs.isShortcut1 = true
                        prefs.shortcutId1 = appModel.shortcutId
                    }

                    2 -> {
                        prefs.appName2 = appModel.appLabel
                        prefs.appPackage2 = appModel.appPackage
                        prefs.appUser2 = appModel.user.toString()
                        prefs.appActivityClassName2 = null
                        prefs.isShortcut2 = true
                        prefs.shortcutId2 = appModel.shortcutId
                    }

                    3 -> {
                        prefs.appName3 = appModel.appLabel
                        prefs.appPackage3 = appModel.appPackage
                        prefs.appUser3 = appModel.user.toString()
                        prefs.appActivityClassName3 = null
                        prefs.isShortcut3 = true
                        prefs.shortcutId3 = appModel.shortcutId
                    }

                    4 -> {
                        prefs.appName4 = appModel.appLabel
                        prefs.appPackage4 = appModel.appPackage
                        prefs.appUser4 = appModel.user.toString()
                        prefs.appActivityClassName4 = null
                        prefs.isShortcut4 = true
                        prefs.shortcutId4 = appModel.shortcutId
                    }

                    5 -> {
                        prefs.appName5 = appModel.appLabel
                        prefs.appPackage5 = appModel.appPackage
                        prefs.appUser5 = appModel.user.toString()
                        prefs.appActivityClassName5 = null
                        prefs.isShortcut5 = true
                        prefs.shortcutId5 = appModel.shortcutId
                    }

                    6 -> {
                        prefs.appName6 = appModel.appLabel
                        prefs.appPackage6 = appModel.appPackage
                        prefs.appUser6 = appModel.user.toString()
                        prefs.appActivityClassName6 = null
                        prefs.isShortcut6 = true
                        prefs.shortcutId6 = appModel.shortcutId
                    }

                    7 -> {
                        prefs.appName7 = appModel.appLabel
                        prefs.appPackage7 = appModel.appPackage
                        prefs.appUser7 = appModel.user.toString()
                        prefs.appActivityClassName7 = null
                        prefs.isShortcut7 = true
                        prefs.shortcutId7 = appModel.shortcutId
                    }

                    8 -> {
                        prefs.appName8 = appModel.appLabel
                        prefs.appPackage8 = appModel.appPackage
                        prefs.appUser8 = appModel.user.toString()
                        prefs.appActivityClassName8 = null
                        prefs.isShortcut8 = true
                        prefs.shortcutId8 = appModel.shortcutId
                    }
                }
            }
        }
        refreshHome(false)
    }

    private fun saveSwipeApp(
        appModel: AppModel,
        isLeft: Boolean,
    ) {
        when (appModel) {
            is AppModel.PrivateSpaceHeader -> return
            is AppModel.App -> {
                if (isLeft) {
                    prefs.appNameSwipeLeft = appModel.appLabel
                    prefs.appPackageSwipeLeft = appModel.appPackage
                    prefs.appUserSwipeLeft = appModel.user.toString()
                    prefs.appActivityClassNameSwipeLeft = appModel.activityClassName
                    prefs.isShortcutSwipeLeft = false
                    prefs.shortcutIdSwipeLeft = ""
                } else {
                    prefs.appNameSwipeRight = appModel.appLabel
                    prefs.appPackageSwipeRight = appModel.appPackage
                    prefs.appUserSwipeRight = appModel.user.toString()
                    prefs.appActivityClassNameRight = appModel.activityClassName
                    prefs.isShortcutSwipeRight = false
                    prefs.shortcutIdSwipeRight = ""
                }
            }

            is AppModel.PinnedShortcut -> {
                if (isLeft) {
                    prefs.appNameSwipeLeft = appModel.appLabel
                    prefs.appPackageSwipeLeft = appModel.appPackage
                    prefs.appUserSwipeLeft = appModel.user.toString()
                    prefs.appActivityClassNameSwipeLeft = null
                    prefs.isShortcutSwipeLeft = true
                    prefs.shortcutIdSwipeLeft = appModel.shortcutId
                } else {
                    prefs.appNameSwipeRight = appModel.appLabel
                    prefs.appPackageSwipeRight = appModel.appPackage
                    prefs.appUserSwipeRight = appModel.user.toString()
                    prefs.appActivityClassNameRight = null
                    prefs.isShortcutSwipeRight = true
                    prefs.shortcutIdSwipeRight = appModel.shortcutId
                }
            }
        }
        updateSwipeApps()
    }

    private fun launchApp(
        packageName: String,
        activityClassName: String?,
        userHandle: UserHandle,
    ) {
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityInfo = launcher.getActivityList(packageName, userHandle)

        val isActivityValid =
            activityClassName.isNullOrBlank().not() &&
                activityInfo.any { it.componentName.className == activityClassName }

        val component =
            if (isActivityValid) {
                ComponentName(packageName, activityClassName)
            } else {
                when (activityInfo.size) {
                    0 -> {
                        appContext.showToast(appContext.getString(R.string.app_not_found))
                        return
                    }

                    1 -> ComponentName(packageName, activityInfo[0].name)
                    else -> ComponentName(packageName, activityInfo[activityInfo.size - 1].name)
                }.also { prefs.updateAppActivityClassName(packageName, it.className) }
            }

        try {
            launcher.startMainActivity(component, userHandle, null, null)
        } catch (_: SecurityException) {
            try {
                launcher.startMainActivity(component, android.os.Process.myUserHandle(), null, null)
            } catch (_: Exception) {
                appContext.showToast(appContext.getString(R.string.unable_to_open_app))
            }
        } catch (_: Exception) {
            appContext.showToast(appContext.getString(R.string.unable_to_open_app))
        }
    }
}
