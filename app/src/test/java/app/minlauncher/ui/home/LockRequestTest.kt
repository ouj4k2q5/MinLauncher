package app.minlauncher.ui.home

import android.app.Application
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import app.minlauncher.helper.LockRequest
import app.minlauncher.testing.TestMainActivity
import app.minlauncher.ui.launchLauncher
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class LockRequestTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `a background double tap sends the lock broadcast when lock mode is on`() {
        launchLauncher {
            firstOpen = false
            firstSettingsOpen = false
            homeAppsNum = 0
            lockModeOn = true
        }.use {
            // The injected event clock starts at zero, where the detector's
            // lastTapUpTime sentinel would make even the first tap look like a
            // double tap. Move past the double-tap timeout so the second tap
            // is the one that fires.
            composeRule.mainClock.advanceTimeBy(1_000)
            composeRule.onRoot().performTouchInput { doubleClick(center) }
            composeRule.waitForIdle()

            val sent = sentLockBroadcasts()
            assertTrue("expected exactly one ${LockRequest.ACTION_LOCK_SCREEN} broadcast, got $sent", sent.size == 1)
        }
    }

    @Test
    fun `a triple tap fires the lock broadcast once`() {
        launchLauncher {
            firstOpen = false
            firstSettingsOpen = false
            homeAppsNum = 0
            lockModeOn = true
        }.use {
            composeRule.mainClock.advanceTimeBy(1_000)
            // Three quick taps, each gap far under the double-tap timeout:
            // the second fires the double tap, and the third must be treated
            // as a new first tap — GestureDetector's TAP message was posted
            // only on first-tap downs, so the old listener fired once too.
            composeRule.onRoot().performTouchInput {
                down(center)
                advanceEventTime(50)
                up()
                advanceEventTime(100)
                down(center)
                advanceEventTime(50)
                up()
                advanceEventTime(100)
                down(center)
                advanceEventTime(50)
                up()
            }
            composeRule.waitForIdle()

            val sent = sentLockBroadcasts()
            assertTrue("expected exactly one ${LockRequest.ACTION_LOCK_SCREEN} broadcast, got $sent", sent.size == 1)
        }
    }

    @Test
    fun `a background double tap sends no lock broadcast when lock mode is off`() {
        launchLauncher {
            firstOpen = false
            firstSettingsOpen = false
            homeAppsNum = 0
            lockModeOn = false
        }.use {
            composeRule.mainClock.advanceTimeBy(1_000)
            composeRule.onRoot().performTouchInput { doubleClick(center) }
            composeRule.waitForIdle()

            val sent = sentLockBroadcasts()
            assertFalse("expected no ${LockRequest.ACTION_LOCK_SCREEN} broadcast, got $sent", sent.isNotEmpty())
        }
    }

    @Test
    fun `the TalkBack lock action on the background sends the lock broadcast when lock mode is on`() {
        launchLauncher {
            firstOpen = false
            firstSettingsOpen = false
            homeAppsNum = 0
            lockModeOn = true
        }.use {
            // The same callback the touch double tap uses, exposed as a
            // semantics click on the background for TalkBack (it replaces the
            // old invisible 1dp lock view).
            composeRule.onNodeWithTag("home.root").performSemanticsAction(SemanticsActions.OnClick)
            composeRule.waitForIdle()

            val sent = sentLockBroadcasts()
            assertTrue("expected exactly one ${LockRequest.ACTION_LOCK_SCREEN} broadcast, got $sent", sent.size == 1)
        }
    }

    /** Every recorded broadcast carrying the lock action. */
    private fun sentLockBroadcasts(): List<Intent> =
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .broadcastIntents
            .filter { it.action == LockRequest.ACTION_LOCK_SCREEN }
}
