package app.minlauncher.ui.nav

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import app.minlauncher.testing.TestMainActivity
import app.minlauncher.ui.fakeApps
import app.minlauncher.ui.launchLauncher
import app.minlauncher.ui.openDrawer
import app.minlauncher.ui.openSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.

/**
 * Pins the two Navigation 3 behaviors the fragment removal relies on: back at
 * the home root is swallowed (no crash, no app exit), and the remembered back
 * stack survives activity recreation.
 */
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class Nav3BehaviorTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `back at home leaves the launcher showing`() {
        launchLauncher(fakeApps("Browser")) {
            firstOpen = false
            firstSettingsOpen = false
        }.use { scenario ->
            // Espresso.pressBack() does not reach Nav3 under Robolectric, so
            // drive the Activity's dispatcher directly; back then flows through
            // AndroidX NavigationEvent to NavDisplay's onBack. Positive
            // control: it pops the drawer.
            scenario.openDrawer()
            composeRule.waitForIdle()
            scenario.onActivity { activity -> assertEquals(2, activity.handle!!.backStack.size) }
            scenario.pressBack()
            composeRule.waitForIdle()
            scenario.onActivity { activity -> assertEquals(1, activity.handle!!.backStack.size) }

            // Back at the home root is consumed instead of popping the empty
            // stack (which Nav3 would crash on), and the activity stays up.
            scenario.pressBack()
            scenario.pressBack()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("home.clock").assertIsDisplayed()
            scenario.onActivity { activity ->
                assertEquals(1, activity.handle!!.backStack.size)
            }
        }
    }

    private fun ActivityScenario<TestMainActivity>.pressBack() {
        onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun `recreate preserves the drawer position`() {
        launchLauncher(fakeApps("Browser")) {
            firstOpen = false
            firstSettingsOpen = false
        }.use { scenario ->
            scenario.openDrawer()
            composeRule.onNodeWithText("Browser").assertIsDisplayed()

            scenario.recreate()
            composeRule.waitForIdle()

            composeRule.onNodeWithText("Browser").assertIsDisplayed()
            scenario.onActivity { activity ->
                assertTrue(activity.handle!!.backStack.last() is DrawerKey)
            }
        }
    }

    @Test
    fun `first hide from a settings-opened drawer keeps settings on the stack once`() {
        launchLauncher(fakeApps("Browser", "Camera")) {
            firstOpen = false
            firstSettingsOpen = false
            firstHide = true
        }.use { scenario ->
            // The drawer can be opened from settings (hidden apps, swipe-app
            // rows); the stack is [home, settings, drawer].
            openSettings()
            scenario.openDrawer()

            composeRule.onNodeWithText("Camera").performTouchInput { longClick() }
            composeRule.onNodeWithTag("drawer.appHide").performClick()
            composeRule.waitForIdle()

            // The first hide swaps the drawer for settings, but a settings
            // entry is already on the stack below the drawer, so the swap may
            // not stack a second one on top of it.
            scenario.onActivity { activity ->
                val backStack = activity.handle!!.backStack
                assertEquals(2, backStack.size)
                assertTrue(backStack.last() is SettingsKey)
            }
        }
    }
}
