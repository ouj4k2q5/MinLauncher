package app.minlauncher

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import app.minlauncher.data.Constants
import org.hamcrest.Matcher

/**
 * Performs a long press that stays down long enough for the home screen's
 * gesture listener, which fires its onLongClick only after an additional
 * LONG_PRESS_DELAY_MS posted on top of the system long-press timeout.
 * Espresso's built-in longClick() holds only the system timeout, so the
 * listener's delayed task would be cancelled by the ACTION_UP before it runs.
 */
internal fun holdLongPress(
    at: CoordinatesProvider = GeneralLocation.CENTER,
    durationMs: Long = Constants.LONG_PRESS_DELAY_MS + ViewConfiguration.getLongPressTimeout(),
): ViewAction =
    object : ViewAction {
        override fun getConstraints(): Matcher<View> = ViewMatchers.isDisplayed()

        override fun getDescription(): String = "hold a long press for $durationMs ms"

        override fun perform(
            uiController: UiController,
            view: View,
        ) {
            val coords = at.calculateCoordinates(view)
            val downTime = SystemClock.uptimeMillis()
            uiController.injectMotionEvent(
                MotionEvent.obtain(
                    downTime,
                    downTime,
                    MotionEvent.ACTION_DOWN,
                    coords[0],
                    coords[1],
                    1f,
                    1f,
                    0,
                    1f,
                    1f,
                    0,
                    0,
                ),
            )
            uiController.loopMainThreadForAtLeast(durationMs)
            uiController.injectMotionEvent(
                MotionEvent.obtain(
                    downTime,
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP,
                    coords[0],
                    coords[1],
                    1f,
                    1f,
                    0,
                    1f,
                    1f,
                    0,
                    0,
                ),
            )
        }
    }

/**
 * Waits until the RecyclerView's adapter holds at least one item. The app
 * list is populated asynchronously from LauncherApps on a background thread,
 * which Espresso's main-thread idling cannot see.
 */
internal fun waitForRecyclerViewItems(timeoutMs: Long = 10_000L): ViewAction =
    object : ViewAction {
        override fun getConstraints(): Matcher<View> =
            ViewMatchers.isAssignableFrom(RecyclerView::class.java)

        override fun getDescription(): String = "wait up to $timeoutMs ms for the list to receive items"

        override fun perform(
            uiController: UiController,
            view: View,
        ) {
            if (view !is RecyclerView) {
                error("Expected a RecyclerView but got ${view.javaClass.simpleName}")
            }
            val deadline = SystemClock.uptimeMillis() + timeoutMs
            while ((view.adapter?.itemCount ?: 0) == 0) {
                if (SystemClock.uptimeMillis() >= deadline) {
                    throw AssertionError("RecyclerView received no items within $timeoutMs ms")
                }
                uiController.loopMainThreadForAtLeast(100)
            }
        }
    }

/**
 * Zero animation scales keep fragment transitions and RecyclerView layout
 * animations out of the way, matching how the Robolectric suite runs. The
 * managed device is discarded after the run, so nothing restores them.
 */
internal fun disableSystemAnimations() {
    val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    listOf(
        "settings put global window_animation_scale 0",
        "settings put global transition_animation_scale 0",
        "settings put global animator_duration_scale 0",
    ).forEach { uiAutomation.executeShellCommand(it).use { /* drain and close */ } }
}
