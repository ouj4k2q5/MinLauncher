package app.minlauncher.ui.drawer

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import kotlinx.coroutines.delay

/** Pull-down-to-exit: a finger drag leaving >10px unconsumed above the top of the list exits. */
@Composable
internal fun rememberPullDownExit(onExitDrawer: () -> Unit): NestedScrollConnection {
    // Same rule as the old LinearLayoutManager override — any single finger
    // drag that leaves more than 10px unconsumed above the top of the list
    // exits the drawer (DrawerEntry guards double pops). Compose nested
    // scroll deltas follow the pointer direction (a downward drag is positive
    // y), the opposite of View's dy, hence > and not <.
    val exitDrawer by rememberUpdatedState(onExitDrawer)
    return remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source == NestedScrollSource.Drag && available.y > 10f) exitDrawer()
                return Offset.Zero
            }
        }
    }
}

/**
 * Auto-launch fires when the settled filter result is exactly one app: never
 * for a blank query (a one-app device would launch it the moment the drawer
 * opens), a leading space (explicit opt-out), a "!" search, or while a CJK
 * composition is in progress.
 *
 * It is additionally gated on the user having edited the query during this
 * composition's lifetime: the query is [androidx.compose.runtime.saveable.rememberSaveable]d
 * by the screen, so it can be non-empty before any user input when it was
 * restored after rotation/process death. A restored query must not launch the
 * app the user searched earlier — not on the first effect run, and not on a
 * later run when the app list arrives asynchronously after process death.
 */
@Composable
internal fun autoLaunchOnSingleResult(
    filtered: List<AppModel>,
    query: TextFieldValue,
    userEditedQuery: Boolean,
    flag: Int,
    callbacks: DrawerCallbacks,
) {
    val latestQuery by rememberUpdatedState(query)
    val latestUserEditedQuery by rememberUpdatedState(userEditedQuery)
    val isCjkKeyboard by rememberUpdatedState(callbacks.isCjkKeyboard)
    LaunchedEffect(filtered) {
        if (!latestUserEditedQuery) return@LaunchedEffect
        val current = latestQuery
        if (isSoleAppResult(filtered) && autoLaunchAllowed(current, flag, isCjkKeyboard())) {
            callbacks.onAppClick(filtered[0])
        }
    }
}

private fun isSoleAppResult(filtered: List<AppModel>): Boolean =
    filtered.size == 1 && filtered[0] !is AppModel.PrivateSpaceHeader

private fun autoLaunchAllowed(
    query: TextFieldValue,
    flag: Int,
    isCjkKeyboard: Boolean,
): Boolean =
    flag == Constants.FLAG_LAUNCH_APP &&
        shouldAutoLaunch(query.text) &&
        !isBangSearch(query.text) &&
        !(query.composition != null && isCjkKeyboard)

/**
 * Keyboard follows scroll like the old RecyclerView listener: hide when a drag
 * starts at the top or settles at the bottom; re-show (per pref) when it
 * settles back at the top after a scroll.
 *
 * The old DRAGGING callback fired before any scroll delta was consumed, so its
 * top check was exact. [LazyListState.isScrollInProgress] flips in the same
 * snapshot as the first delta, and the collector runs after both — by then a
 * read of canScrollBackward misreports the pre-drag position (a drag away from
 * the top looks like it started mid-list, and a drag back to the top looks
 * like it started there). The top-ness is therefore recorded while idle
 * ([idleAtTop]) and consulted at drag start instead.
 */
@Composable
internal fun observeScrollForKeyboard(
    listState: LazyListState,
    autoShowKeyboard: Boolean,
    searchFocus: FocusRequester,
    keyboard: SoftwareKeyboardController?,
) {
    val focusManager = LocalFocusManager.current
    var idleAtTop by remember { mutableStateOf(true) }
    var dragStartedOnTop by remember { mutableStateOf(false) }
    LaunchedEffect(listState, autoShowKeyboard) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) {
                dragStartedOnTop = idleAtTop
                if (dragStartedOnTop) {
                    // The old View helper also dropped focus when hiding, which
                    // kept the IME from re-serving the still-focused editor.
                    focusManager.clearFocus()
                    keyboard?.hide()
                }
            } else {
                if (!listState.canScrollForward) {
                    keyboard?.hide()
                } else if (!listState.canScrollBackward && !dragStartedOnTop && autoShowKeyboard) {
                    searchFocus.requestFocus()
                    keyboard?.show()
                }
                idleAtTop = !listState.canScrollBackward
            }
        }
    }
}

/** The old onStart: show the keyboard when the auto-show pref is on. */
@Composable
internal fun showKeyboardOnStart(
    state: DrawerUiState,
    searchFocus: FocusRequester,
    keyboard: SoftwareKeyboardController?,
) {
    val view = LocalView.current
    LaunchedEffect(state.showKeyboardSignal) {
        if (state.autoShowKeyboard) {
            // onStart can run before the window regains focus (returning from
            // another app), and showSoftInput is a no-op without it. The old
            // View helper postDelayed(100)'d the show for exactly this reason;
            // retry the same way when the first show raced the focus.
            val windowFocused = view.hasWindowFocus()
            searchFocus.requestFocus()
            keyboard?.show()
            if (!windowFocused) {
                delay(100)
                keyboard?.show()
            }
        }
    }
}
