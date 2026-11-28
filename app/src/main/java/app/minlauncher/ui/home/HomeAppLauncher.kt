package app.minlauncher.ui.home

import android.content.Context
import android.content.pm.LauncherApps
import android.util.Log
import app.minlauncher.HomeViewModel
import app.minlauncher.R
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.helper.getUserHandleFromString
import app.minlauncher.helper.isPackageInstalled
import app.minlauncher.helper.openCameraApp
import app.minlauncher.helper.openDialerApp
import app.minlauncher.helper.showToast

private const val TAG = "HomeAppLauncher"

/**
 * The home screen's slot validation and app/shortcut launching, moved out of
 * the old HomeFragment (Stage 2 of the fragment removal) so the
 * home entry composable can run the same actions without a fragment.
 *
 * These are the fragment's originals with its `requireContext()`/`getString()`
 * calls and its `prefs`/`viewModel` fields turned into explicit parameters; the
 * bodies are otherwise unchanged.
 */
internal fun verifiedHomeAppLabel(
    context: Context,
    prefs: Prefs,
    location: Int,
): String {
    val (label, verified) =
        if (prefs.getIsShortcut(location)) {
            verifiedShortcutLabel(context, prefs, location)
        } else {
            verifiedAppLabel(context, prefs, location)
        }
    if (!verified) clearHomeApp(prefs, location)
    return label
}

// The shortcut query throws anything the platform feels like when the package
// or user vanished; a failed verification must fall back to "" instead of
// taking down the home screen.
@Suppress("TooGenericExceptionCaught")
internal fun verifiedShortcutLabel(
    context: Context,
    prefs: Prefs,
    location: Int,
): Pair<String, Boolean> {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    // Query for the specific shortcut
    val query =
        LauncherApps.ShortcutQuery().apply {
            setPackage(prefs.getAppPackage(location))
            setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        }

    return try {
        val shortcuts =
            launcherApps.getShortcuts(query, getUserHandleFromString(context, prefs.getAppUser(location)))
        // Check if our shortcut still exists
        if (shortcuts?.any { it.id == prefs.getShortcutId(location) } == true) {
            prefs.getAppName(location) to true
        } else {
            "" to false
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to verify home screen shortcut", e)
        "" to false
    }
}

internal fun verifiedAppLabel(
    context: Context,
    prefs: Prefs,
    location: Int,
): Pair<String, Boolean> =
    prefs.getAppName(location) to
        isPackageInstalled(context, prefs.getAppPackage(location), prefs.getAppUser(location))

/** The old setHomeAppText failure path: forget a slot whose app vanished. */
internal fun clearHomeApp(
    prefs: Prefs,
    location: Int,
) {
    when (location) {
        Constants.FLAG_SET_HOME_APP_1 -> {
            prefs.appName1 = ""
            prefs.appPackage1 = ""
        }

        Constants.FLAG_SET_HOME_APP_2 -> {
            prefs.appName2 = ""
            prefs.appPackage2 = ""
        }

        Constants.FLAG_SET_HOME_APP_3 -> {
            prefs.appName3 = ""
            prefs.appPackage3 = ""
        }

        Constants.FLAG_SET_HOME_APP_4 -> {
            prefs.appName4 = ""
            prefs.appPackage4 = ""
        }

        Constants.FLAG_SET_HOME_APP_5 -> {
            prefs.appName5 = ""
            prefs.appPackage5 = ""
        }

        Constants.FLAG_SET_HOME_APP_6 -> {
            prefs.appName6 = ""
            prefs.appPackage6 = ""
        }

        Constants.FLAG_SET_HOME_APP_7 -> {
            prefs.appName7 = ""
            prefs.appPackage7 = ""
        }

        Constants.FLAG_SET_HOME_APP_8 -> {
            prefs.appName8 = ""
            prefs.appPackage8 = ""
        }
    }
}

// The launching call needs the slot's five stored fields plus the context and
// view model it acts through; the count mirrors the prefs columns, not a
// hidden parameter object.
@Suppress("LongParameterList")
internal fun launchAppOrShortcut(
    context: Context,
    viewModel: HomeViewModel,
    appName: String,
    packageName: String,
    activityClassName: String?,
    shortcutId: String?,
    isShortcut: Boolean,
    userString: String,
    fallback: (() -> Unit)? = null,
) {
    if (appName.isEmpty()) {
        context.showToast(context.getString(R.string.long_press_to_select_app))
        return
    }
    if (isShortcut && !shortcutId.isNullOrEmpty()) {
        launchShortcut(
            context = context,
            viewModel = viewModel,
            packageName = packageName,
            shortcutId = shortcutId,
            shortcutLabel = appName,
            userString = userString,
        )
    } else if (packageName.isNotEmpty()) {
        launchApp(
            context = context,
            viewModel = viewModel,
            appName = appName,
            packageName = packageName,
            activityClassName = activityClassName,
            userString = userString,
        )
    } else {
        fallback?.invoke()
    }
}

// The explicit context/view model parameters mirror the old fragment's two
// fields this used to reach implicitly; the rest is the shortcut's identity.
@Suppress("LongParameterList")
internal fun launchShortcut(
    context: Context,
    viewModel: HomeViewModel,
    shortcutId: String,
    packageName: String,
    shortcutLabel: String,
    userString: String,
) {
    viewModel.selectedApp(
        AppModel.PinnedShortcut(
            shortcutId = shortcutId,
            appLabel = shortcutLabel,
            user = getUserHandleFromString(context, userString),
            key = null,
            appPackage = packageName,
            isNew = false,
        ),
        Constants.FLAG_LAUNCH_APP,
    )
}

// Same as launchShortcut: the old fragment's implicit fields named explicitly.
@Suppress("LongParameterList")
internal fun launchApp(
    context: Context,
    viewModel: HomeViewModel,
    appName: String,
    packageName: String,
    activityClassName: String?,
    userString: String,
) {
    viewModel.selectedApp(
        AppModel.App(
            appLabel = appName,
            key = null,
            appPackage = packageName,
            activityClassName = activityClassName,
            isNew = false,
            user = getUserHandleFromString(context, userString),
        ),
        Constants.FLAG_LAUNCH_APP,
    )
}

internal fun homeAppClicked(
    context: Context,
    prefs: Prefs,
    viewModel: HomeViewModel,
    location: Int,
) {
    launchAppOrShortcut(
        context = context,
        viewModel = viewModel,
        appName = prefs.getAppName(location),
        packageName = prefs.getAppPackage(location),
        activityClassName = prefs.getAppActivityClassName(location),
        shortcutId = prefs.getShortcutId(location),
        isShortcut = prefs.getIsShortcut(location),
        userString = prefs.getAppUser(location),
    )
}

internal fun openSwipeRightApp(
    context: Context,
    prefs: Prefs,
    viewModel: HomeViewModel,
) {
    if (!prefs.swipeRightEnabled) return
    launchAppOrShortcut(
        context = context,
        viewModel = viewModel,
        appName = prefs.appNameSwipeRight.ifEmpty { context.getString(R.string.swipe_right_app_default) },
        packageName = prefs.appPackageSwipeRight,
        activityClassName = prefs.appActivityClassNameRight,
        shortcutId = prefs.shortcutIdSwipeRight,
        isShortcut = prefs.isShortcutSwipeRight,
        userString = prefs.appUserSwipeRight,
        fallback = { openDialerApp(context) },
    )
}

internal fun openSwipeLeftApp(
    context: Context,
    prefs: Prefs,
    viewModel: HomeViewModel,
) {
    if (!prefs.swipeLeftEnabled) return
    launchAppOrShortcut(
        context = context,
        viewModel = viewModel,
        appName = prefs.appNameSwipeLeft.ifEmpty { context.getString(R.string.swipe_left_app_default) },
        packageName = prefs.appPackageSwipeLeft,
        activityClassName = prefs.appActivityClassNameSwipeLeft,
        shortcutId = prefs.shortcutIdSwipeLeft,
        isShortcut = prefs.isShortcutSwipeLeft,
        userString = prefs.appUserSwipeLeft,
        fallback = { openCameraApp(context) },
    )
}
