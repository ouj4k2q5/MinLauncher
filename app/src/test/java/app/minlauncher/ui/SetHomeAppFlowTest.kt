package app.minlauncher.ui

import android.os.Looper
import android.view.ViewConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import app.minlauncher.data.Constants
import app.minlauncher.testing.TestMainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.util.concurrent.TimeUnit

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class SetHomeAppFlowTest {
    // The home screen and the drawer are Compose; the empty rule drives their
    // semantics trees while the tests launch their own ActivityScenario.
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `the set-home drawer shows the flag-specific search hint`() {
        launchLauncher(fakeApps("Browser")) {
            firstOpen = false
            firstSettingsOpen = false
        }.use { scenario ->
            scenario.openDrawer(flag = Constants.FLAG_SET_HOME_APP_1)

            composeRule.onNodeWithText("Please select an app").assertIsDisplayed()
        }
    }

    @Test
    fun `long press on a home slot opens select mode and saves the chosen app to the slot`() {
        val browser = fakeApp("Browser")
        val camera = fakeApp("Camera")

        launchLauncher(listOf(browser, camera)) {
            firstOpen = false
            firstSettingsOpen = false
            appName1 = "Browser"
            appPackage1 = browser.appPackage
            appActivityClassName1 = browser.activityClassName
            isShortcut1 = false
        }.use {
            // The seeded slot shows its app.
            composeRule.onNodeWithText("Browser").assertIsDisplayed()

            // Long press on the slot opens the drawer in select mode. The slot
            // is a Compose node, so the press goes through the compose rule;
            // its detector fires the long press at the system long-press
            // timeout plus LONG_PRESS_DELAY_MS after the down (the old
            // listener timing), which the shadow clock covers.
            composeRule.onNodeWithText("Browser").performTouchInput { down(center) }
            shadowOf(Looper.getMainLooper()).idleFor(
                Constants.LONG_PRESS_DELAY_MS + ViewConfiguration.getLongPressTimeout() + 100,
                TimeUnit.MILLISECONDS,
            )
            // Nav3 hosts every screen in one Compose root, so unlike the old
            // fragment world the down is not dropped by the home view's
            // destruction: release it against the still-live root before the
            // next injection. The long press has already fired at this point.
            composeRule.onRoot().performTouchInput { up() }

            // Selecting an app closes the drawer and updates the slot.
            composeRule.onNodeWithText("Camera").performClick()

            composeRule.onNodeWithText("Camera").assertIsDisplayed()
            composeRule.onNodeWithText("Browser").assertDoesNotExist()

            // The choice is persisted as the home app for slot 1.
            val prefs = testPrefs()
            assertEquals("Camera", prefs.appName1)
            assertEquals(camera.appPackage, prefs.appPackage1)
            assertEquals(camera.activityClassName, prefs.appActivityClassName1)
            assertFalse(prefs.isShortcut1)
        }
    }

    @Test
    fun `a cancelled touch fires nothing and later gestures still work`() {
        val browser = fakeApp("Browser")
        val camera = fakeApp("Camera")

        launchLauncher(listOf(browser, camera)) {
            firstOpen = false
            firstSettingsOpen = false
            appName1 = "Browser"
            appPackage1 = browser.appPackage
            appActivityClassName1 = browser.activityClassName
            isShortcut1 = false
        }.use {
            composeRule.onNodeWithText("Browser").assertIsDisplayed()

            // The system takes the touch over (gesture navigation, palm
            // rejection): Compose delivers ACTION_CANCEL as a synthetic
            // consumed up, which the detector must treat as the end of the
            // gesture.
            composeRule.onNodeWithText("Browser").performTouchInput {
                down(center)
                cancel()
            }

            // Past the long-press window: nothing fired, the home screen is
            // still up (the drawer's rows would show "Browser" too, so the
            // home-only clock tag decides)...
            shadowOf(Looper.getMainLooper()).idleFor(
                Constants.LONG_PRESS_DELAY_MS + ViewConfiguration.getLongPressTimeout() + 100,
                TimeUnit.MILLISECONDS,
            )
            composeRule.onNodeWithTag("home.clock").assertIsDisplayed()

            // ...and the detector recovered: the next long press still opens
            // the drawer (Camera only exists there).
            composeRule.onNodeWithText("Browser").performTouchInput { down(center) }
            shadowOf(Looper.getMainLooper()).idleFor(
                Constants.LONG_PRESS_DELAY_MS + ViewConfiguration.getLongPressTimeout() + 100,
                TimeUnit.MILLISECONDS,
            )
            composeRule.onNodeWithText("Camera").assertIsDisplayed()
        }
    }

    @Test
    fun `a second pointer down cancels the pending long press`() {
        val browser = fakeApp("Browser")
        val camera = fakeApp("Camera")

        launchLauncher(listOf(browser, camera)) {
            firstOpen = false
            firstSettingsOpen = false
            appName1 = "Browser"
            appPackage1 = browser.appPackage
            appActivityClassName1 = browser.activityClassName
            isShortcut1 = false
        }.use {
            composeRule.onNodeWithText("Browser").assertIsDisplayed()

            // A second finger joins before the long press fires: like
            // GestureDetector's ACTION_POINTER_DOWN -> cancelTaps(), the
            // pending long press and tap must be dropped.
            composeRule.onNodeWithText("Browser").performTouchInput {
                down(pointerId = 0, position = center)
                down(pointerId = 1, position = center + Offset(10f, 0f))
            }
            composeRule.waitForIdle()

            // Hold past the long-press window: nothing fires while the extra
            // pointer stays down, and the home screen is still showing.
            shadowOf(Looper.getMainLooper()).idleFor(
                Constants.LONG_PRESS_DELAY_MS + ViewConfiguration.getLongPressTimeout() + 100,
                TimeUnit.MILLISECONDS,
            )
            composeRule.onNodeWithTag("home.clock").assertIsDisplayed()

            // Release both pointers, then a normal long press still opens the
            // drawer (the detector recovered).
            composeRule.onNodeWithText("Browser").performTouchInput {
                advanceEventTime(1_000)
                up(pointerId = 1)
                up(pointerId = 0)
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Browser").performTouchInput { down(center) }
            shadowOf(Looper.getMainLooper()).idleFor(
                Constants.LONG_PRESS_DELAY_MS + ViewConfiguration.getLongPressTimeout() + 100,
                TimeUnit.MILLISECONDS,
            )
            composeRule.onNodeWithText("Camera").assertIsDisplayed()
        }
    }

    @Test
    fun `d-pad center on a focused home slot taps it`() {
        launchLauncher(fakeApps()) {
            firstOpen = false
            firstSettingsOpen = false
            homeAppsNum = 1
        }.use {
            composeRule.onNodeWithText("App").assertIsDisplayed()

            // The old slot TextViews were focusable, so a d-pad/keyboard could
            // focus a slot and launch it with center/Enter; the Compose slot
            // restores that with focusable + onKeyEvent.
            composeRule.onNodeWithText("App").requestFocus()
            composeRule.onNodeWithText("App").performKeyInput { pressKey(Key.DirectionCenter) }
            composeRule.waitForIdle()

            // Same outcome as the quick tap above: the unset slot shows its
            // hint toast, once.
            assertEquals(1, ShadowToast.shownToastCount())
        }
    }

    @Test
    fun `releasing a slot between the two long-press timeouts does nothing`() {
        launchLauncher(fakeApps()) {
            firstOpen = false
            firstSettingsOpen = false
            homeAppsNum = 1
        }.use {
            composeRule.onNodeWithText("App").assertIsDisplayed()

            // Positive control: a quick tap on the unset slot fires the hint
            // toast, so a fired tap below would be observable.
            composeRule.onNodeWithText("App").performClick()
            assertEquals(1, ShadowToast.shownToastCount())

            // Held past the system long-press timeout but released before the
            // extra LONG_PRESS_DELAY_MS: GestureDetector suppressed the tap
            // once its long press had fired, so neither tap nor long press
            // may fire.
            composeRule.onNodeWithText("App").performTouchInput {
                down(center)
                advanceEventTime(ViewConfiguration.getLongPressTimeout() + 50L)
                up()
            }

            composeRule.onNodeWithText("App").assertIsDisplayed()
            assertEquals(1, ShadowToast.shownToastCount())
        }
    }
}
