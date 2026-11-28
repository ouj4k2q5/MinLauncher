package app.minlauncher.ui.drawer

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import app.minlauncher.data.AppModel

/**
 * Theme attributes resolved once by DrawerEntry from the activity theme (so
 * the day/night variant is the one the activity is currently using). Compose
 * must not hardcode the palette or extend MinLauncherTheme's M3 color scheme
 * with it.
 */
@Immutable
data class DrawerColors(
    val primaryColor: Int,
    val shadeDark: Int,
    val colorTrans50: Int,
    val primaryColorTrans80: Int,
    val textShadow: Int,
    /**
     * The theme's `mainFontFamily` (already overlaid by MainActivity when the
     * bold-font pref is on); the old TextLarge / AppSearchText styles used it.
     */
    val fontFamily: FontFamily,
)

/**
 * The combined list already includes the Private Space header (and its apps
 * while unlocked) for the current flag; the screen owns only the query, the
 * open menu/rename rows and scroll state. [showKeyboardSignal] is bumped by
 * DrawerEntry on every ON_START so the screen re-shows the keyboard the way
 * the old onStart listener did, including across activity stop/start cycles.
 */
@Immutable
data class DrawerUiState(
    val apps: List<AppModel>,
    val flag: Int,
    /** Flag-dependent hint, matching the old queryHint: "___", "Hidden apps" or "Please select an app". */
    val searchHint: String,
    val canRename: Boolean,
    val appLabelAlignment: Int,
    val autoShowKeyboard: Boolean,
    val showKeyboardSignal: Int,
)

/**
 * Side effects live in DrawerEntry: selection, launch intents, uninstall,
 * app-info, the SharedPreferences hidden-apps write, navigation, toasts and
 * dialog posting. [isCjkKeyboard] is a query, cached by DrawerEntry, used to
 * suppress auto-launch while a CJK composition is in progress.
 */
@Suppress("LongParameterList") // a callback bag; each entry is one distinct screen side effect
@Immutable
data class DrawerCallbacks(
    val onAppClick: (AppModel) -> Unit,
    val onAppInfo: (AppModel) -> Unit,
    val onAppDelete: (AppModel) -> Unit,
    val onAppHide: (AppModel) -> Unit,
    val onAppRename: (AppModel, String) -> Unit,
    /** Saves the typed name to the home slot being customized; false means the name was empty. */
    val onRenameHomeSlot: (String) -> Boolean,
    val onPrivateSpaceToggle: () -> Unit,
    val onPrivateSpaceSettings: () -> Unit,
    /** Pull-down-to-exit and non-launch selections; DrawerEntry guards double pops. */
    val onExitDrawer: () -> Unit,
    val onBangSearch: (String) -> Unit,
    val onOpenSearch: (String?) -> Unit,
    val isCjkKeyboard: () -> Boolean,
)

/**
 * One button of the long-press action menu, also reused by the rename row.
 * The label/icon/alpha come from the old adapter's per-action rules (delete
 * dimmed for system apps, hide dimmed for pinned shortcuts).
 */
@Immutable
internal data class DrawerMenuAction(
    val label: String,
    val icon: Int,
    val alpha: Float,
    val testTag: String,
    val onClick: () -> Unit,
)

/**
 * Row-level interaction state for the list: which app's long-press menu is
 * open and which app is being renamed, plus everything those rows need to
 * render. Built once per drawer screen; at most one menu/rename row is open.
 * Rows close their own menu/rename when the list disposes them, mirroring the
 * old adapter's bind() reset so a row scrolling back does not reopen it.
 */
@Stable
internal class DrawerRowState(
    val flag: Int,
    val palette: DrawerPalette,
    val shadeDark: Int,
    val primaryColorTrans80: Int,
    val textAlign: TextAlign,
    val callbacks: DrawerCallbacks,
) {
    var menuFor by mutableStateOf<AppModel?>(null)
        private set
    var renameFor by mutableStateOf<AppModel?>(null)
        private set

    fun openMenu(app: AppModel) {
        menuFor = app
    }

    fun openRename(app: AppModel) {
        renameFor = app
        menuFor = null
    }

    fun closeMenu() {
        menuFor = null
    }

    fun closeRename() {
        renameFor = null
    }

    /**
     * Closes the menu/rename row when the list disposes [app]'s row, like the
     * old adapter's bind() reset. Only clears state still pointing at [app]:
     * the menu may already have moved to another row before this disposal.
     */
    fun onRowDisposed(app: AppModel) {
        if (menuFor == app) {
            menuFor = null
        }
        if (renameFor == app) {
            renameFor = null
        }
    }
}
