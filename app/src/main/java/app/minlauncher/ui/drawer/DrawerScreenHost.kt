package app.minlauncher.ui.drawer

import androidx.compose.runtime.Composable
import app.minlauncher.data.AppModel

/**
 * The app drawer's host: adapts the drawer's fragment-held state and the
 * resolved colors/callbacks into [DrawerScreen]. Moved out of the Fragment
 * (Stage 1 of the fragment removal).
 *
 * State is deliberately not hoisted: the drawer is a transient screen that is
 * destroyed on back navigation and on drawer-to-settings navigation, so the
 * Fragment keeps `combinedList` / `keyboardShowSignal` and passes them in.
 */
@Suppress("LongParameterList") // one flat slice of the host's inputs, mirroring DrawerUiState
@Composable
internal fun DrawerScreenHost(
    colors: DrawerColors,
    callbacks: DrawerCallbacks,
    apps: List<AppModel>,
    flag: Int,
    searchHint: String,
    canRename: Boolean,
    appLabelAlignment: Int,
    autoShowKeyboard: Boolean,
    showKeyboardSignal: Int,
) {
    DrawerScreen(
        state =
            DrawerUiState(
                apps = apps,
                flag = flag,
                searchHint = searchHint,
                canRename = canRename,
                appLabelAlignment = appLabelAlignment,
                autoShowKeyboard = autoShowKeyboard,
                showKeyboardSignal = showKeyboardSignal,
            ),
        colors = colors,
        callbacks = callbacks,
    )
}
