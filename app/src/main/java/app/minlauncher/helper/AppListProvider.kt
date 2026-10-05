package app.minlauncher.helper

import android.content.Context
import app.minlauncher.data.AppModel
import app.minlauncher.data.Prefs

/**
 * Seam for swapping the source of the app list between production and tests.
 *
 * Production code uses [RealAppListProvider]; tests inject a fake so that
 * ViewModel behaviour can be verified without `LauncherApps`.
 */
interface AppListProvider {
    suspend fun getAppsList(
        context: Context,
        prefs: Prefs,
        includeRegularApps: Boolean = true,
        includeHiddenApps: Boolean = false,
    ): List<AppModel>
}
