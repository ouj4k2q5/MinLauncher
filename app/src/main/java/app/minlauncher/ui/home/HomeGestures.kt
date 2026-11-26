package app.minlauncher.ui.home

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import app.minlauncher.data.Constants
import kotlin.math.abs

private const val SWIPE_DISTANCE_THRESHOLD = 100f
private const val SWIPE_VELOCITY_THRESHOLD = 100f

/**
 * Background gestures, a port of OnSwipeTouchListener's contract:
 * a fling is >100px dominant-axis travel from the down to the up at >100px/s
 * (the dominant axis is decided by travel distance, and the check runs on the
 * up event, like GestureDetector.onFling(e1 = down, e2 = up)); a long press
 * fires at the system long-press timeout plus LONG_PRESS_DELAY_MS while the
 * finger stays down within the touch slop, posted on the main handler exactly
 * like the old listener so Robolectric's shadow clock drives it; a double
 * tap fires on the second down that arrives within doubleTapTimeoutMillis of
 * the previous tap's up. The second tap's up is not recorded as the start of
 * the next comparison — GestureDetector posted its TAP message only on
 * first-tap downs — so a quick third tap is a new first tap there too.
 *
 * The double-tap slop distance check GestureDetector applies is dropped
 * (ViewConfiguration exposes no double-tap slop to Compose); a second tap
 * within the time window anywhere on the background now locks the screen.
 *
 * A second pointer down cancels the pending long press and tap, like
 * GestureDetector's ACTION_POINTER_DOWN -> cancelTaps() — and, stricter
 * than cancelTaps() (which still lets the eventual up fling), it ends the
 * gesture outright: nothing fires while any other pointer is pressed, even
 * if the originally tracked pointer is still down; [awaitEachGesture] then
 * waits for all pointers to lift before starting the next gesture.
 *
 * Touches on children that consume their downs (home slots, clock, date,
 * screen time, the set-default line) never start a background gesture:
 * [awaitFirstDown]'s requireUnconsumed skips gestures whose down a child
 * already consumed in the Main pass.
 */
internal fun Modifier.homeBackgroundGestures(callbacks: HomeCallbacks): Modifier =
    pointerInput(callbacks) {
        val handler = Handler(Looper.getMainLooper())
        var lastTapUpTime = 0L
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            down.consume()
            val isDoubleTap = down.uptimeMillis - lastTapUpTime < viewConfiguration.doubleTapTimeoutMillis
            if (isDoubleTap) {
                lastTapUpTime = 0L
                callbacks.onBackgroundDoubleTap()
            }
            handleHomeGesture(
                down = down,
                handler = handler,
                onLongPress = callbacks.onBackgroundLongPress,
                onFling = callbacks::dispatchSwipe,
                onTap = null,
            )?.let { if (!isDoubleTap) lastTapUpTime = it }
        }
    }

/**
 * Home-slot gestures, a port of ViewSwipeTouchListener's contract: the same
 * fling and long-press rules as the background plus an immediate tap on the
 * up (no double-tap confirmation) and a pressed flag that drives the slot's
 * text color, matching the old isPressed handling. Slots consume their downs
 * so the background detector above skips the whole gesture.
 */
internal fun Modifier.homeSlotGestures(
    slotIndex: Int,
    callbacks: HomeCallbacks,
    onPressedChange: (Boolean) -> Unit,
): Modifier =
    pointerInput(slotIndex, callbacks) {
        val handler = Handler(Looper.getMainLooper())
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            down.consume()
            onPressedChange(true)
            try {
                handleHomeGesture(
                    down = down,
                    handler = handler,
                    onLongPress = { callbacks.onSlotLongPress(slotIndex) },
                    onFling = callbacks::dispatchSwipe,
                    onTap = { callbacks.onSlotTap(slotIndex) },
                )
            } finally {
                onPressedChange(false)
            }
        }
    }

/**
 * One gesture shared by the background and the slots. Waits for the up (the
 * await covers the time after a long press fired, so the gesture is drained),
 * cancels the long-press task on touch-slop exit (like GestureDetector's tap
 * zone), fires [onTap] only on an in-slop up before the long-press timeout —
 * GestureDetector suppresses the tap once its long press would have fired, so
 * releasing mid window between the two timeouts does nothing, like the old
 * listeners — and evaluates the fling on the up otherwise.
 *
 * Returns the up time of a plain in-slop tap for double-tap bookkeeping, or
 * null for any other gesture end (the background caller stores it; slots
 * ignore it). A system ACTION_CANCEL is not delivered to this coroutine as a
 * cancellation: Compose dispatches a synthetic event whose changes are
 * consumed and no longer pressed, [awaitGestureOutcome] detects that as a
 * cancelled end, and the try/finally below removes the pending long-press
 * task then, like the old listeners' ACTION_CANCEL branch.
 */
private suspend fun AwaitPointerEventScope.handleHomeGesture(
    down: PointerInputChange,
    handler: Handler,
    onLongPress: () -> Unit,
    onFling: (SwipeDirection) -> Unit,
    onTap: (() -> Unit)?,
): Long? {
    var longPressAllowed = true
    var longPressFired = false
    val longPressTask =
        Runnable {
            if (longPressAllowed) {
                longPressFired = true
                onLongPress()
            }
        }
    handler.postDelayed(
        longPressTask,
        viewConfiguration.longPressTimeoutMillis + Constants.LONG_PRESS_DELAY_MS,
    )
    try {
        val outcome = awaitGestureOutcome(down) { longPressAllowed = false }
        return when {
            outcome.cancelled || longPressFired -> null

            outcome.exitedSlop -> {
                evaluateFling(down, outcome.up, outcome.tracker)?.let(onFling)
                null
            }

            else ->
                if (outcome.up.uptimeMillis - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis) {
                    onTap?.invoke()
                    outcome.up.uptimeMillis
                } else {
                    null
                }
        }
    } finally {
        longPressAllowed = false
        handler.removeCallbacks(longPressTask)
    }
}

/** The end of one tracked gesture: its up change, slop state and velocity history. */
private class GestureOutcome(
    val up: PointerInputChange,
    val exitedSlop: Boolean,
    val tracker: VelocityTracker,
    val cancelled: Boolean,
)

/** Awaits the up, recording positions and reporting when the touch slop is left. */
private suspend fun AwaitPointerEventScope.awaitGestureOutcome(
    down: PointerInputChange,
    onSlopExit: () -> Unit,
): GestureOutcome {
    val tracker = VelocityTracker()
    tracker.addPosition(down.uptimeMillis, down.position)
    var exitedSlop = false
    val slopSquared = viewConfiguration.touchSlop * viewConfiguration.touchSlop
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id }
        // A second pointer down — GestureDetector's ACTION_POINTER_DOWN
        // cancelled the taps there — or Compose's form of ACTION_CANCEL (a
        // synthetic up that is consumed): both end the gesture without firing
        // anything. The tracked pointer may still be down in the first case;
        // awaitEachGesture waits for the next down either way.
        val cancelledUp =
            when {
                event.changes.any { it.id != down.id && it.pressed } -> down
                change != null && change.changedToUpIgnoreConsumed() && change.isConsumed -> change
                else -> null
            }
        if (cancelledUp != null) {
            return GestureOutcome(cancelledUp, exitedSlop, tracker, cancelled = true)
        }
        if (change == null) continue
        tracker.addPosition(change.uptimeMillis, change.position)
        if (change.changedToUp()) {
            return GestureOutcome(change, exitedSlop, tracker, cancelled = false)
        }
        if (!exitedSlop && (change.position - down.position).getDistanceSquared() > slopSquared) {
            exitedSlop = true
            onSlopExit()
        }
    }
}

/** The old listeners' fling check, run on the up event like GestureDetector. */
private fun evaluateFling(
    down: PointerInputChange,
    up: PointerInputChange,
    tracker: VelocityTracker,
): SwipeDirection? {
    val velocity = tracker.calculateVelocity()
    val diffX = up.position.x - down.position.x
    val diffY = up.position.y - down.position.y
    return if (abs(diffX) > abs(diffY)) {
        if (abs(diffX) > SWIPE_DISTANCE_THRESHOLD && abs(velocity.x) > SWIPE_VELOCITY_THRESHOLD) {
            if (diffX > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
        } else {
            null
        }
    } else {
        if (abs(diffY) > SWIPE_DISTANCE_THRESHOLD && abs(velocity.y) > SWIPE_VELOCITY_THRESHOLD) {
            if (diffY < 0) SwipeDirection.UP else SwipeDirection.DOWN
        } else {
            null
        }
    }
}
