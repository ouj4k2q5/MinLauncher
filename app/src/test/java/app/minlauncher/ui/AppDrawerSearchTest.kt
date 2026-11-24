package app.minlauncher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import app.minlauncher.testing.TestMainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class AppDrawerSearchTest {
    // The drawer is Compose; the empty rule drives its semantics tree while
    // the tests launch their own ActivityScenario.
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    private val apps = fakeApps("Browser", "Camera", "Mail", "Maps", "Music")

    @Test
    fun `typing a query narrows the drawer to matching apps`() {
        launchLauncher(apps).use { scenario ->
            scenario.openDrawer()

            composeRule.onNodeWithText("Browser").assertIsDisplayed()

            composeRule.onNodeWithTag("drawer.search").performTextInput("ma")

            composeRule.onNodeWithText("Mail").assertIsDisplayed()
            composeRule.onNodeWithText("Maps").assertIsDisplayed()
            composeRule.onNodeWithText("Browser").assertDoesNotExist()
            composeRule.onNodeWithText("Camera").assertDoesNotExist()
            composeRule.onNodeWithText("Music").assertDoesNotExist()
        }
    }

    @Test
    fun `clearing the query restores the full list`() {
        launchLauncher(apps).use { scenario ->
            scenario.openDrawer()

            composeRule.onNodeWithTag("drawer.search").performTextReplacement("ma")
            composeRule.onNodeWithText("Browser").assertDoesNotExist()

            composeRule.onNodeWithTag("drawer.search").performTextClearance()

            composeRule.onNodeWithText("Browser").assertIsDisplayed()
            composeRule.onNodeWithText("Camera").assertIsDisplayed()
            composeRule.onNodeWithText("Music").assertIsDisplayed()
        }
    }

    @Test
    fun `a non-matching query empties the list without crashing`() {
        launchLauncher(apps).use { scenario ->
            scenario.openDrawer()

            composeRule.onNodeWithTag("drawer.search").performTextInput("zzz")

            composeRule.onNodeWithText("Browser").assertDoesNotExist()
            composeRule.onNodeWithText("Camera").assertDoesNotExist()
            composeRule.onNodeWithText("Mail").assertDoesNotExist()
            composeRule.onNodeWithText("Maps").assertDoesNotExist()
            composeRule.onNodeWithText("Music").assertDoesNotExist()
        }
    }
}
