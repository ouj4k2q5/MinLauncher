package app.minlauncher.helper

import android.content.Context
import app.minlauncher.data.AppModel
import app.minlauncher.data.Prefs

/**
 * Test double for [AppListProvider]. The world is the union of
 * [regularApps] and [hiddenApps]; membership of each app follows
 * [Prefs.hiddenApps] and the include flags via [shouldIncludeApp],
 * mirroring [RealAppListProvider] so hide/unhide flows behave like
 * production. Seed [Prefs.hiddenApps] to control which apps are hidden.
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
        (regularApps + hiddenApps).filter { app ->
            shouldIncludeApp(
                selfPackage = context.packageName,
                appPackage = app.appPackage,
                userKey = app.user.toString(),
                hiddenApps = prefs.hiddenApps,
                includeRegularApps = includeRegularApps,
                includeHiddenApps = includeHiddenApps,
            )
        }
}
