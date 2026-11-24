package app.minlauncher

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
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
