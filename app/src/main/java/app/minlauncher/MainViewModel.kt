package app.minlauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.minlauncher.data.AppModel
import app.minlauncher.data.LauncherRepository
import app.minlauncher.data.Prefs
import app.minlauncher.helper.formattedTimeSpent
import app.minlauncher.helper.hasBeenMinutes
import app.minlauncher.helper.usageStats.EventLogWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(
    application: Application,
    repository: LauncherRepository,
) : AndroidViewModel(application) {
    // Secondary constructor for ViewModelProvider's default factory; uses the
    // app-scoped repository from LauncherApp. Tests inject a repository through
    // the primary constructor.
    constructor(application: Application) : this(application, (application as LauncherApp).launcherRepository)

    private val repository = repository
    private val appContext by lazy { application.applicationContext }
    private val prefs = Prefs(appContext)
    private var screenTimeJob: Job? = null

    val refreshHome get() = repository.refreshHome
    val toggleDateTime get() = repository.toggleDateTime
    val updateSwipeApps get() = repository.updateSwipeApps
    val appList get() = repository.appList
    val hiddenApps get() = repository.hiddenApps
    val isMinLauncherDefault get() = repository.isMinLauncherDefault
    val launcherResetFailed = MutableLiveData<Boolean>()
    val homeAppAlignment get() = repository.homeAppAlignment
    val screenTimeValue = MutableLiveData<String>()

    val privateSpaceApps get() = repository.privateSpaceApps
    val privateSpaceLocked get() = repository.privateSpaceLocked
    val privateSpaceAvailable get() = repository.privateSpaceAvailable

    // Suppress backToHomeScreen during Private Space lock/unlock auth
    var isPrivateSpaceToggling: Boolean
        get() = repository.isPrivateSpaceToggling
        set(value) {
            repository.isPrivateSpaceToggling = value
        }

    val showDialog get() = repository.showDialog
    val resetLauncherLiveData get() = repository.resetLauncherLiveData
    // Home button for recents feature disabled

    fun selectedApp(
        appModel: AppModel,
        flag: Int,
    ) = repository.selectedApp(appModel, flag)

    fun refreshHome(appCountUpdated: Boolean) = repository.refreshHome(appCountUpdated)

    fun toggleDateTime() = repository.toggleDateTime()

    fun getAppList(includeHiddenApps: Boolean = false) = repository.getAppList(includeHiddenApps)

    fun getHiddenApps() = repository.getHiddenApps()

    fun isMinLauncherDefault() = repository.isMinLauncherDefault()

    fun updateHomeAlignment(gravity: Int) = repository.updateHomeAlignment(gravity)

    fun getTodaysScreenTime() {
        if (prefs.screenTimeLastUpdated.hasBeenMinutes(1).not()) return
        if (screenTimeJob?.isActive == true) return

        screenTimeJob =
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val eventLogWrapper = EventLogWrapper(appContext)
                    // Start of today in millis
                    val calendar =
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                    val startTime = calendar.timeInMillis
                    val endTime = System.currentTimeMillis()

                    val timeSpent =
                        eventLogWrapper.aggregateSimpleUsageStats(
                            eventLogWrapper.aggregateForegroundStats(
                                eventLogWrapper.getForegroundStatsByTimestamps(startTime, endTime),
                            ),
                        )
                    val viewTimeSpent = appContext.formattedTimeSpent(timeSpent)
                    screenTimeValue.postValue(viewTimeSpent)
                    prefs.screenTimeLastUpdated = endTime
                } catch (_: SecurityException) {
                }
            }
    }

    fun getPrivateSpaceAppList() = repository.getPrivateSpaceAppList()

    fun togglePrivateSpaceLock() = repository.togglePrivateSpaceLock()
}
