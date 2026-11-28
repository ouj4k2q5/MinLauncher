package app.minlauncher.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import app.minlauncher.R
import app.minlauncher.data.LauncherRepository
import app.minlauncher.helper.getColorFromAttr
import app.minlauncher.helper.isEinkDisplay
import app.minlauncher.helper.isSystemAnimationsDisabled
import app.minlauncher.ui.drawer.DrawerEntry
import app.minlauncher.ui.home.HomeEntry
import app.minlauncher.ui.nav.DrawerKey
import app.minlauncher.ui.nav.HomeKey
import app.minlauncher.ui.nav.MinLauncherNavKey
import app.minlauncher.ui.nav.SettingsKey
import app.minlauncher.ui.settings.SettingsEntry
import app.minlauncher.ui.theme.MinLauncherTheme

/**
 * The launcher's whole navigation: a Navigation 3 [NavDisplay] over the
 * [MinLauncherNavKey] back stack, wrapped in the app theme and the solid
 * wallpaper background.
 *
 * [appTheme] is read once by the host activity at `setContent` time.
 * [LauncherRepository] deliberately has no theme flow (theme changes propagate
 * through `prefs` + `Activity.recreate()`), so a flow here would be a second
 * source of truth.
 *
 * The back handler pops only while something is above the home screen. Leaving
 * it enabled with a no-op at home is intentional: Nav3's default handler pops
 * the last entry and crashes on the resulting empty stack, whereas the guard
 * makes back at home a consumed no-op.
 *
 * [onHandleReady] publishes a handle for the host activity (back-stack access
 * for `backToHomeScreen`) and is called with null on dispose.
 */
@Composable
fun AppRoot(
    repository: LauncherRepository,
    appTheme: Int,
    onHandleReady: (AppRootHandle?) -> Unit = {},
) {
    val context = LocalContext.current

    // rememberNavBackStack's public return type is NavBackStack<NavKey>; the
    // launcher only ever pushes the sealed MinLauncherNavKey subtypes, so the
    // narrower type is safe and keeps every consumer exhaustive.
    @Suppress("UNCHECKED_CAST")
    val backStack = rememberNavBackStack(HomeKey) as NavBackStack<MinLauncherNavKey>
    // Animation scales are static for the activity's lifetime; e-ink and
    // "remove animations" devices get instant transitions like the fragment
    // animator did.
    val instant = remember(context) { context.isEinkDisplay() || context.isSystemAnimationsDisabled() }
    val solidWallpaper by repository.solidWallpaper.collectAsStateWithLifecycle()
    val backgroundColor =
        if (solidWallpaper) Color(context.getColorFromAttr(R.attr.primaryInverseColor)) else Color.Transparent

    // The dialog is root-scoped, like the old activity-wide message overlay: it
    // stays up across entry changes (a hide in the drawer posts HIDDEN and then
    // lands on settings) until the user dismisses it or back-to-home collapses.
    var dialog by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(repository) {
        repository.showDialog.collect { dialog = it }
    }

    MinLauncherTheme(appTheme) {
        Box(
            Modifier
                .fillMaxSize()
                .testTag("root")
                .background(backgroundColor),
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                transitionSpec = if (instant) NoTransition else defaultTransitionSpec<MinLauncherNavKey>(),
                popTransitionSpec = if (instant) NoTransition else defaultPopTransitionSpec<MinLauncherNavKey>(),
                predictivePopTransitionSpec =
                    if (instant) NoPredictiveTransition else defaultPredictivePopTransitionSpec<MinLauncherNavKey>(),
                entryProvider =
                    entryProvider<MinLauncherNavKey> {
                        entry<HomeKey> { HomeEntry(repository, backStack) }
                        entry<DrawerKey> { key -> DrawerEntry(repository, key, backStack) }
                        entry<SettingsKey> { SettingsEntry(repository, backStack) }
                    },
            )
            dialog?.let { posted -> AppDialog(posted) { dialog = null } }
            DisposableEffect(backStack) {
                onHandleReady(AppRootHandle(backStack) { dialog = null })
                onDispose { onHandleReady(null) }
            }
        }
    }
}

/**
 * Back-stack access for the host activity; mutate on the main thread only.
 *
 * [dismissDialog] closes the root-scoped message dialog, so collapsing to home
 * is total: it clears both the navigation stack and any lingering dialog, like
 * the old `backToHomeScreen` hiding the message overlay before popping.
 */
class AppRootHandle(
    val backStack: NavBackStack<MinLauncherNavKey>,
    private val dismissDialog: () -> Unit = {},
) {
    /** Closes any open dialog and pops everything above the home screen. */
    fun collapseToHome() {
        dismissDialog()
        while (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }
}

private val NoTransition: AnimatedContentTransitionScope<Scene<MinLauncherNavKey>>.() -> ContentTransform =
    { EnterTransition.None togetherWith ExitTransition.None }

private val NoPredictiveTransition:
    AnimatedContentTransitionScope<Scene<MinLauncherNavKey>>.(Int) -> ContentTransform =
    { _ -> EnterTransition.None togetherWith ExitTransition.None }
