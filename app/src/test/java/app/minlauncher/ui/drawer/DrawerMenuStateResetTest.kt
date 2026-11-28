package app.minlauncher.ui.drawer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
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

/**
 * Regression protection for the old adapter's bind() reset: LazyColumn disposes
 * a row that scrolls out of the viewport, so the hoisted menu/rename state must
 * be cleared on disposal, otherwise the menu reappears (and the rename field
 * resets the user's typed text) when the row scrolls back.
 *
 * Pin the Robolectric SDK to targetSdk (36).
 */
@Config(sdk = [36])
@RunWith(RobolectricTestRunner::class)
class DrawerMenuStateResetTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    // Enough rows that the first ones are far outside the viewport at the end of the list.
    private val apps = fakeApps(*(0 until 40).map { "App %02d".format(it) }.toTypedArray())

    @Test
    fun `a menu row does not reopen after scrolling out and back`() {
        launchLauncher(apps).use { scenario ->
            scenario.openDrawer()

            composeRule.onNodeWithText("App 00").performTouchInput { longClick() }
            composeRule.onNodeWithTag("drawer.appHide").assertIsDisplayed()

            composeRule.onNodeWithTag("drawer.list").performScrollToIndex(apps.lastIndex)
            // The disposal closes the menu, so the row is gone along with its menu.
            composeRule.onNodeWithTag("drawer.appHide").assertDoesNotExist()

            composeRule.onNodeWithTag("drawer.list").performScrollToIndex(0)
            composeRule.onNodeWithTag("drawer.appHide").assertDoesNotExist()
            composeRule.onNodeWithText("App 00").assertIsDisplayed()
        }
    }

    @Test
    fun `a rename row does not reopen after scrolling out and back`() {
        launchLauncher(apps).use { scenario ->
            scenario.openDrawer()

            composeRule.onNodeWithText("App 00").performTouchInput { longClick() }
            composeRule.onNodeWithTag("drawer.appRename").performClick()
            composeRule.onNodeWithTag("drawer.renameInput").assertIsDisplayed()

            composeRule.onNodeWithTag("drawer.list").performScrollToIndex(apps.lastIndex)
            composeRule.onNodeWithTag("drawer.renameInput").assertDoesNotExist()

            composeRule.onNodeWithTag("drawer.list").performScrollToIndex(0)
            composeRule.onNodeWithTag("drawer.renameInput").assertDoesNotExist()
            composeRule.onNodeWithText("App 00").assertIsDisplayed()
        }
    }
}
