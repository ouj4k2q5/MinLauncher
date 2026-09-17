package app.minlauncher.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.app.SearchManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import app.minlauncher.BuildConfig
import app.minlauncher.R
import app.minlauncher.data.Constants

private const val TAG = "Extensions"

fun View.hideKeyboard() {
    clearFocus()
    windowInsetsController?.hide(WindowInsets.Type.ime())
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard(show: Boolean = true) {
    if (show.not()) return
    if (requestFocus()) {
        postDelayed({
            val targetView = findFocus() ?: this
            targetView.windowInsetsController?.show(WindowInsets.Type.ime())
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(targetView, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }
}

fun Activity.showLauncherSelector(requestCode: Int) {
    val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
    if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        startActivityForResult(intent, requestCode)
    } else {
        resetDefaultLauncher()
    }
}

// The implicit intent is the mechanism here: FakeHomeActivity is briefly enabled so that
// more than one activity answers CATEGORY_HOME, which is what makes the system show its
// launcher chooser. Setting an explicit component would launch FakeHomeActivity directly
// and no chooser would appear.
@SuppressLint("UnsafeImplicitIntentLaunch")
fun Context.resetDefaultLauncher() {
    try {
        val componentName = ComponentName(this, FakeHomeActivity::class.java)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        val selector = Intent(Intent.ACTION_MAIN)
        selector.addCategory(Intent.CATEGORY_HOME)
        startActivity(selector)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to reset default launcher", e)
    }
}

fun Context.isDefaultLauncher(): Boolean {
    val launcherPackageName = getDefaultLauncherPackage(this)
    return BuildConfig.APPLICATION_ID == launcherPackageName
}

fun Context.resetLauncherViaFakeActivity() {
    resetDefaultLauncher()
    if (getDefaultLauncherPackage(this).contains(".")) {
        startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    }
}

fun Context.openSearch(query: String? = null) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH)
    intent.putExtra(SearchManager.QUERY, query ?: "")
    startActivity(intent)
}

private var isEinkDevice: Boolean? = null

fun Context.isEinkDisplay(): Boolean {
    isEinkDevice?.let { return it }
    return (hasEinkRefreshRate() || isOnyxDevice() || isKnownEinkModel())
        .also { isEinkDevice = it }
}

private fun Context.hasEinkRefreshRate(): Boolean {
    return try {
        // Check max supported refresh rate, not the current one, because adaptive
        // refresh rate displays can drop to 30Hz or lower without being e-ink
        val maxRefreshRate = display.supportedModes.maxOfOrNull { it.refreshRate } ?: return false
        maxRefreshRate <= Constants.MIN_ANIM_REFRESH_RATE
    } catch (e: Exception) {
        Log.e(TAG, "Failed to check display refresh rate", e)
        false
    }
}

// Boox devices report 60Hz+ refresh rates, so the refresh rate check misses them
@SuppressLint("PrivateApi")
private fun isOnyxDevice(): Boolean {
    if (Build.MANUFACTURER.equals("ONYX", ignoreCase = true)) return true
    return try {
        // Onyx firmware ships its e-ink SDK classes in the boot classpath
        Class.forName("android.onyx.ViewUpdateHelper")
        true
    } catch (_: Throwable) {
        false
    }
}

private fun isKnownEinkModel(): Boolean {
    val brand = Build.BRAND.lowercase()
    val manufacturer = Build.MANUFACTURER.lowercase()
    val einkOnlyBrands = listOf("onyx", "boox", "dasung", "bigme", "boyue", "meebook", "mudita")
    if (einkOnlyBrands.any { brand.contains(it) || manufacturer.contains(it) }) return true
    // Hisense also sells LCD phones, so match only their e-ink line
    if (brand.contains("hisense") || manufacturer.contains("hisense")) {
        return Regex("\\bA[579]\\b|TOUCH|HI READER").containsMatchIn(Build.MODEL.uppercase())
    }
    return false
}

fun Context.isSystemAnimationsDisabled(): Boolean =
    try {
        Settings.Global.getFloat(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1f) == 0f ||
            Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f ||
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    } catch (e: Exception) {
        Log.e(TAG, "Failed to check system animations setting", e)
        false
    }

fun Context.isPackageInstalled(
    packageName: String,
    userHandle: UserHandle = android.os.Process.myUserHandle(),
): Boolean {
    val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val activityInfo = launcher.getActivityList(packageName, userHandle)
    return activityInfo.isNotEmpty()
}

fun Context.appUsagePermissionGranted(): Boolean {
    val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return appOpsManager.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        packageName,
    ) == AppOpsManager.MODE_ALLOWED
}

fun Context.formattedTimeSpent(timeSpent: Long): String {
    val seconds = timeSpent / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        timeSpent == 0L -> "0m"

        hours > 0 ->
            getString(
                R.string.time_spent_hour,
                hours.toString(),
                remainingMinutes.toString(),
            )

        minutes > 0 -> {
            getString(R.string.time_spent_min, minutes.toString())
        }

        else -> "<1m"
    }
}

fun Long.hasBeenHours(hours: Int): Boolean =
    ((System.currentTimeMillis() - this) / Constants.ONE_HOUR_IN_MILLIS) >= hours

fun Long.hasBeenMinutes(minutes: Int): Boolean =
    ((System.currentTimeMillis() - this) / Constants.ONE_MINUTE_IN_MILLIS) >= minutes

fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
