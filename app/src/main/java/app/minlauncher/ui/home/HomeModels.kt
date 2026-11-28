package app.minlauncher.ui.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily

/**
 * Theme attributes resolved once by HomeEntry from the activity theme, so
 * the day/night variant is the one the displayed screen uses, and the
 * bold-font overlay MainActivity applies on top of AppTheme is included via
 * [fontFamily] (the old TextLarge style read the same attribute).
 */
@Immutable
data class HomeColors(
    val primaryColor: Int,
    val primaryColorTrans80: Int,
    val textShadow: Int,
    val fontFamily: FontFamily,
)

/**
 * One home-app slot. [index] is the 1-based slot number (it doubles as
 * Constants.FLAG_SET_HOME_APP_n); an empty [label] is an unset slot, which
 * renders the "App" hint like the old TextView hint did.
 */
@Immutable
data class HomeSlotState(
    val index: Int,
    val label: String,
)

/**
 * The renderable home screen: verified slots (already cleared in prefs when
 * their app vanished, like the old populateHomeScreen), the alignment
 * settings as Gravity constants, and the date/screen-time visibility flags.
 * The clock and date *text* is computed inside Compose from the injected
 * clock, not carried here.
 */
@Immutable
data class HomeUiState(
    val slots: List<HomeSlotState>,
    /** Gravity.START or Gravity.END. */
    val homeAppAlignment: Int,
    val homeBottomAlignment: Boolean,
    /** One of Constants.DateTime. */
    val dateTimeVisibility: Int,
    /** When false (status bar hidden), the date line appends the battery level. */
    val showStatusBar: Boolean,
    val showFirstRunTips: Boolean,
    val showSetDefaultLauncher: Boolean,
    /** The screen-time line; null hides it. */
    val screenTime: String?,
)

/**
 * Side effects live in HomeEntry: app launching, navigation, drawer and
 * settings opening, notification drawer expansion and sending the lock-mode
 * broadcast. Slot and background gestures share the swipe callbacks, like
 * OnSwipeTouchListener and ViewSwipeTouchListener did.
 */
@Suppress("LongParameterList") // a callback bag; each entry is one distinct screen side effect
@Immutable
data class HomeCallbacks(
    val onSwipeLeft: () -> Unit,
    val onSwipeRight: () -> Unit,
    val onSwipeUp: () -> Unit,
    val onSwipeDown: () -> Unit,
    val onBackgroundLongPress: () -> Unit,
    val onBackgroundDoubleTap: () -> Unit,
    val onSlotTap: (Int) -> Unit,
    val onSlotLongPress: (Int) -> Unit,
    val onClockTap: () -> Unit,
    val onDateTap: () -> Unit,
    val onScreenTimeTap: () -> Unit,
    val onSetDefaultLauncherTap: () -> Unit,
    val onSetDefaultLauncherLongPress: () -> Unit,
) {
    fun dispatchSwipe(direction: SwipeDirection) {
        when (direction) {
            SwipeDirection.LEFT -> onSwipeLeft()
            SwipeDirection.RIGHT -> onSwipeRight()
            SwipeDirection.UP -> onSwipeUp()
            SwipeDirection.DOWN -> onSwipeDown()
        }
    }
}

/** The four fling directions the old listeners reported. */
enum class SwipeDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN,
}
