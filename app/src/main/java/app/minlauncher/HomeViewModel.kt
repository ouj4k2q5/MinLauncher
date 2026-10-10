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

/**
 * Screen adapter for the home screen; all shared state and selection logic live
 * in [LauncherRepository], which is app-scoped so the home and drawer screens
 * observe the same data.
 *
 * Constructed only through the explicit factory in HomeFragment, which reads
 * the current activity's application (under Robolectric the default factory's
 * Application fallback can be stale across tests).
 */
class HomeViewModel(
    application: Application,
    private val repository: LauncherRepository,
) : AndroidViewModel(application) {
    private val appContext by lazy { application.applicationContext }
    private val prefs = Prefs(appContext)
    private var screenTimeJob: Job? = null

    val refreshHome get() = repository.refreshHome
    val isMinLauncherDefault get() = repository.isMinLauncherDefault
    val homeAppAlignment get() = repository.homeAppAlignment
    val toggleDateTime get() = repository.toggleDateTime
    val resetLauncherLiveData get() = repository.resetLauncherLiveData
    val screenTimeValue = MutableLiveData<String>()

    fun isMinLauncherDefault() = repository.isMinLauncherDefault()

    fun selectedApp(
        appModel: AppModel,
        flag: Int,
    ) = repository.selectedApp(appModel, flag)

    fun getAppList(includeHiddenApps: Boolean = false) = repository.getAppList(includeHiddenApps)

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
}
