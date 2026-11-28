package app.minlauncher.ui.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The launcher's Navigation 3 destinations. A sealed hierarchy gives the entry
 * provider an exhaustive `when` and lets [androidx.navigation3.runtime.rememberNavBackStack]
 * serialize the stack across process death (the runtime stores each key by its
 * concrete class name).
 *
 * Settings takes no arguments: the alignment/theme rows live on the settings
 * screen itself. The drawer's customization arguments belong to the drawer.
 */
@Serializable
sealed interface MinLauncherNavKey : NavKey

/** The permanent home screen; the back stack's root. */
@Serializable
data object HomeKey : MinLauncherNavKey

/**
 * The app drawer. [flag] is one of [app.minlauncher.data.Constants]' FLAG_*
 * values (launch, hidden apps, home-slot/swipe-app customization) and
 * [canRename] enables the rename row for a home-slot customization.
 */
@Serializable
data class DrawerKey(
    val flag: Int,
    val canRename: Boolean,
) : MinLauncherNavKey

/** The settings screen. */
@Serializable
data object SettingsKey : MinLauncherNavKey
