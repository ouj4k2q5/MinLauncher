package app.minlauncher.helper

import android.content.Context
import app.minlauncher.data.AppModel
import app.minlauncher.data.Prefs

/**
 * Test double for [AppListProvider]: returns the configured regular/hidden
 * lists as-is, depending on the include flags, without touching the system.
 */
class FakeAppListProvider(
    private val regularApps: List<AppModel> = emptyList(),
    private val hiddenApps: List<AppModel> = emptyList(),
) : AppListProvider {
    override suspend fun getAppsList(
        context: Context,
        prefs: Prefs,
        includeRegularApps: Boolean,
        includeHiddenApps: Boolean,
    ): List<AppModel> =
        buildList {
            if (includeRegularApps) addAll(regularApps)
            if (includeHiddenApps) addAll(hiddenApps)
        }
}
