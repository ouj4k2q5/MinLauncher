package app.minlauncher.ui.drawer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import app.minlauncher.testing.TestMainActivity
import app.minlauncher.ui.fakeApps
import app.minlauncher.ui.launchLauncher
import app.minlauncher.ui.openDrawer
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Pin the Robolectric SDK to targetSdk (36).
@Config(sdk = [36])
@RunWith(RobolectricTestRunner::class)
class DrawerPullDownExitTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    // Enough apps that the list actually scrolls in the Robolectric viewport.
    private val manyApps =
        fakeApps(
            "Alpha",
            "Bravo",
            "Charlie",
            "Delta",
            "Echo",
            "Foxtrot",
            "Golf",
            "Hotel",
            "India",
            "Juliet",
            "Kilo",
            "Lima",
        )

    @Test
    fun `pulling down at the top exits the drawer`() {
        launchLauncher(manyApps).use { scenario ->
            scenario.openDrawer()

            composeRule.onNodeWithTag("drawer.list").performTouchInput { swipeDown() }

            composeRule.waitForIdle()
            composeRule.onNodeWithTag("drawer.list").assertDoesNotExist()
        }
    }

    @Test
    fun `pulling down at the top after scrolling exits the drawer`() {
        launchLauncher(manyApps).use { scenario ->
            scenario.openDrawer()

            // Scroll into the list, then back up to the very top. The return
            // uses a programmatic scroll: a fast return gesture would pass the
            // top within one drag and legitimately exit it (the ported rule
            // exits any single drag that overshoots the top by >10px).
            composeRule.onNodeWithTag("drawer.list").performTouchInput { swipeUp() }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("drawer.list").performScrollToIndex(0)
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Alpha").assertIsDisplayed()

            // The pull-down past the top must exit the drawer.
            composeRule.onNodeWithTag("drawer.list").performTouchInput { swipeDown() }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("drawer.list").assertDoesNotExist()
        }
    }

    @Test
    fun `swiping up at the bottom does not exit the drawer`() {
        launchLauncher(manyApps).use { scenario ->
            scenario.openDrawer()

            composeRule.onNodeWithTag("drawer.list").performScrollToIndex(manyApps.size - 1)
            composeRule.waitForIdle()

            // Overscrolling past the end must not pop the drawer — the sign
            // bug made exactly this gesture exit.
            composeRule.onNodeWithTag("drawer.list").performTouchInput { swipeUp() }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("drawer.list").assertExists()
        }
    }
}
