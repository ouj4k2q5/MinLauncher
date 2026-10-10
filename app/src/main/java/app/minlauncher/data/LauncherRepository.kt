package app.minlauncher.data

import android.content.Context
import android.os.Build
import android.os.UserManager
import android.util.Log
import androidx.lifecycle.MutableLiveData
import app.minlauncher.helper.AppListProvider
import app.minlauncher.helper.SingleLiveEvent
import app.minlauncher.helper.getPrivateSpaceApps
import app.minlauncher.helper.getPrivateSpaceUserHandle
import app.minlauncher.helper.isMinLauncherDefault
import app.minlauncher.helper.isPrivateSpaceLocked
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "LauncherRepository"

/**
 * Shared-state holder for the launcher's screens, extracted from
 * [app.minlauncher.MainViewModel]. Owns the LiveData backing fields and the
 * state-mutating functions; MainViewModel delegates to it.
 */
class LauncherRepository(
    private val appContext: Context,
    private val appListProvider: AppListProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val prefs = Prefs(appContext)

    val refreshHome = MutableLiveData<Boolean>()
    val toggleDateTime = MutableLiveData<Unit>()
    val updateSwipeApps = MutableLiveData<Any>()
    val appList = MutableLiveData<List<AppModel>?>()
    val hiddenApps = MutableLiveData<List<AppModel>?>()
    val isMinLauncherDefault = MutableLiveData<Boolean>()
    val homeAppAlignment = MutableLiveData<Int>()

    val privateSpaceApps = MutableLiveData<List<AppModel>?>()
    val privateSpaceLocked = MutableLiveData<Boolean>()
    val privateSpaceAvailable = MutableLiveData<Boolean>()

    // Suppress backToHomeScreen during Private Space lock/unlock auth
    var isPrivateSpaceToggling = false

    val showDialog = SingleLiveEvent<String>()
    val resetLauncherLiveData = SingleLiveEvent<Unit?>()

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
        toggleDateTime.postValue(Unit)
    }

    fun updateSwipeApps() {
        updateSwipeApps.postValue(Unit)
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
}
