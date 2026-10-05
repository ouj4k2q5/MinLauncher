package app.minlauncher.helper

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import android.util.Log
import app.minlauncher.BuildConfig
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.data.shortcutIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

private const val TAG = "RealAppListProvider"

/**
 * Production [AppListProvider]: enumerates apps and pinned shortcuts via
 * `LauncherApps` / `UserManager`. Body moved from `Utils.getAppsList`.
 */
object RealAppListProvider : AppListProvider {
    // A launcher must survive any system-service failure while enumerating apps:
    // return the (possibly partial) list instead of crashing, so the broad
    // catch is intentional and the generic-exception rule is suppressed here.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun getAppsList(
        context: Context,
        prefs: Prefs,
        includeRegularApps: Boolean,
        includeHiddenApps: Boolean,
    ): List<AppModel> =
        withContext(Dispatchers.IO) {
            val appList: MutableList<AppModel> = mutableListOf()

            try {
                if (!prefs.hiddenAppsUpdated) upgradeHiddenApps(prefs)
                val hiddenApps = prefs.hiddenApps

                val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
                val launcherApps =
                    context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val collator = Collator.getInstance()

                for (profile in userManager.userProfiles) {
                    if (isPrivateSpaceProfile(context, profile)) continue
                    for (app in launcherApps.getActivityList(null, profile)) {
                        val appLabelShown =
                            prefs
                                .getAppRenameLabel(app.applicationInfo.packageName)
                                .ifBlank { app.label.toString() }
                        val appModel =
                            AppModel.App(
                                appLabel = appLabelShown,
                                key = collator.getCollationKey(app.label.toString()),
                                appPackage = app.applicationInfo.packageName,
                                activityClassName = app.componentName.className,
                                isNew =
                                    (System.currentTimeMillis() - app.firstInstallTime) <
                                        Constants.ONE_HOUR_IN_MILLIS,
                                user = profile,
                            )

                        // if the current app is not MinLauncher
                        if (app.applicationInfo.packageName != BuildConfig.APPLICATION_ID) {
                            // is this a hidden app?
                            if (hiddenApps.contains(app.applicationInfo.packageName + "|" + profile.toString())) {
                                if (includeHiddenApps) {
                                    appList.add(appModel)
                                }
                            } else {
                                // this is a regular app
                                if (includeRegularApps) {
                                    appList.add(appModel)
                                }
                            }
                        }
                    }
                }

                // Add shortcuts if we're getting regular apps
                if (includeRegularApps) {
                    val pinned =
                        try {
                            getPinnedShortcuts(context, prefs, collator)
                        } catch (_: Exception) {
                            emptyList()
                        }
                    appList.addAll(pinned)
                }

                appList.sortWith(compareBy(collator) { it.appLabel })
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load apps list", e)
            }
            appList
        }

    // Same policy as getAppsList: a failure reading one profile's shortcuts must
    // not abort the whole app list, so the broad catch is intentional.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun getPinnedShortcuts(
        context: Context,
        prefs: Prefs,
        collator: Collator,
    ): List<AppModel.PinnedShortcut> =
        withContext(Dispatchers.IO) {
            val pinnedShortcuts = mutableListOf<AppModel.PinnedShortcut>()
            val shortcuts = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            if (shortcuts?.hasShortcutHostPermission() == true) {
                val query =
                    LauncherApps.ShortcutQuery().apply {
                        setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                    }
                shortcuts.profiles.forEach { profile ->
                    if (isPrivateSpaceProfile(context, profile)) return@forEach
                    try {
                        shortcuts.getShortcuts(query, profile)?.forEach { shortcut ->
                            val identity =
                                shortcutIdentity(
                                    shortcut.`package`,
                                    shortcut.id,
                                    profile.toString(),
                                )
                            if (shortcut.isPinned && pinnedShortcuts.none { it.identity == identity }) {
                                val label =
                                    prefs
                                        .getAppRenameLabel(identity)
                                        .ifBlank { prefs.getAppRenameLabel(shortcut.id) }
                                        .takeIf { it.isNotBlank() }
                                        ?: shortcut.shortLabel?.toString()
                                        ?: shortcut.longLabel?.toString().orEmpty()
                                pinnedShortcuts.add(
                                    AppModel.PinnedShortcut(
                                        appLabel = label,
                                        key = collator.getCollationKey(label),
                                        appPackage = shortcut.`package`,
                                        shortcutId = shortcut.id,
                                        isNew = false,
                                        user = profile,
                                    ),
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load pinned shortcuts", e)
                    }
                }
            }
            pinnedShortcuts
        }

    // This is to ensure backward compatibility with older app versions
    // which did not support multiple user profiles
    private fun upgradeHiddenApps(prefs: Prefs) {
        val hiddenAppsSet = prefs.hiddenApps
        val newHiddenAppsSet = mutableSetOf<String>()
        for (hiddenPackage in hiddenAppsSet) {
            if (hiddenPackage.contains("|")) {
                newHiddenAppsSet.add(hiddenPackage)
            } else {
                newHiddenAppsSet.add(
                    hiddenPackage +
                        android.os.Process
                            .myUserHandle()
                            .toString(),
                )
            }
        }
        prefs.hiddenApps = newHiddenAppsSet
        prefs.hiddenAppsUpdated = true
    }
}
