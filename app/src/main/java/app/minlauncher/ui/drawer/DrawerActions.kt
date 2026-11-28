package app.minlauncher.ui.drawer

import android.content.Context
import android.os.Process
import android.view.inputmethod.InputMethodManager
import app.minlauncher.DrawerViewModel
import app.minlauncher.R
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.helper.deletePinnedShortcut
import app.minlauncher.helper.isSystemApp
import app.minlauncher.helper.openAppInfo
import app.minlauncher.helper.showToast
import app.minlauncher.helper.uninstall

/**
 * The app drawer's non-navigation actions, moved out of
 * [app.minlauncher.ui.AppDrawerFragment] (Stage 2 of the fragment removal) so the
 * drawer entry composable can run the same actions without a fragment.
 *
 * These are the fragment's originals with its `requireContext()`/`getString()`
 * calls and its `prefs`/`viewModel`/transient-state fields turned into explicit
 * parameters; the bodies are otherwise unchanged. Navigation (including the
 * `exitDrawer()` at the end of `renameHomeSlot`) stays in the fragment.
 */

internal fun onAppDeleted(
    context: Context,
    viewModel: DrawerViewModel,
    appModel: AppModel,
) {
    when (appModel) {
        is AppModel.PrivateSpaceHeader -> {}
        is AppModel.PinnedShortcut ->
            context.deletePinnedShortcut(
                packageName = appModel.appPackage,
                shortcutIdToDelete = appModel.shortcutId,
                user = appModel.user,
            )

        is AppModel.App -> {
            if (appModel.user != Process.myUserHandle()) {
                openAppInfo(context, appModel.user, appModel.appPackage)
            } else if (context.isSystemApp(appModel.appPackage, appModel.user)) {
                context.showToast(context.getString(R.string.system_app_cannot_delete))
                openAppInfo(context, appModel.user, appModel.appPackage)
            } else {
                context.uninstall(appModel.appPackage)
            }
        }
    }
    viewModel.getAppList()
}

/**
 * Saves the typed name to the home slot being customized; false means the name
 * was empty. The fragment runs `exitDrawer()` when this returns true.
 */
internal fun renameHomeSlot(
    context: Context,
    prefs: Prefs,
    flag: Int,
    name: String,
): Boolean {
    if (name.isEmpty()) {
        context.showToast(context.getString(R.string.type_a_new_app_name_first))
        return false
    }
    when (flag) {
        Constants.FLAG_SET_HOME_APP_1 -> prefs.appName1 = name
        Constants.FLAG_SET_HOME_APP_2 -> prefs.appName2 = name
        Constants.FLAG_SET_HOME_APP_3 -> prefs.appName3 = name
        Constants.FLAG_SET_HOME_APP_4 -> prefs.appName4 = name
        Constants.FLAG_SET_HOME_APP_5 -> prefs.appName5 = name
        Constants.FLAG_SET_HOME_APP_6 -> prefs.appName6 = name
        Constants.FLAG_SET_HOME_APP_7 -> prefs.appName7 = name
        Constants.FLAG_SET_HOME_APP_8 -> prefs.appName8 = name
    }
    return true
}

internal fun isCjkKeyboard(context: Context): Boolean {
    val result =
        runCatching {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val subtype = imm?.currentInputMethodSubtype ?: return@runCatching false

            val language =
                subtype.languageTag.ifEmpty {
                    @Suppress("DEPRECATION")
                    subtype.locale
                }

            language.startsWith("zh") || language.startsWith("ja") || language.startsWith("ko")
        }.getOrElse { false }
    return result
}

// The fragment keeps the transient snapshots (`currentAppList` and the private
// space state); they are parameters here. A null app list returns null, mirroring
// the original's early return so the caller leaves its combined list untouched.
internal fun updateCombinedAppList(
    flag: Int,
    currentAppList: List<AppModel>?,
    privateSpaceAvailable: Boolean,
    privateSpaceLocked: Boolean,
    privateSpaceApps: List<AppModel>?,
): List<AppModel>? {
    val apps = currentAppList ?: return null
    val combined = apps.toMutableList()

    if (flag == Constants.FLAG_LAUNCH_APP && privateSpaceAvailable) {
        combined.add(AppModel.PrivateSpaceHeader(isLocked = privateSpaceLocked))
        if (!privateSpaceLocked) {
            privateSpaceApps?.let { combined.addAll(it) }
        }
    }

    return combined
}
