package app.minlauncher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
class LauncherUiSmokeTest {
    // The home screen and the drawer are Compose; the empty rule drives
    // their semantics trees while the tests launch their own ActivityScenario.
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `home screen is shown after launch`() {
        launchLauncher(fakeApps("Browser", "Camera", "Mail")).use {
            // The clock is Compose too now; it renders text immediately
            // (the old TextClock stayed zero-width until its first tick).
            composeRule.onNodeWithTag("home.dateTime").assertIsDisplayed()
            composeRule.onNodeWithTag("home.clock").assertIsDisplayed()
        }
    }

    @Test
    fun `app drawer lists the fake apps`() {
        launchLauncher(fakeApps("Browser", "Camera", "Mail")).use { scenario ->
            scenario.openDrawer()

            composeRule.onNodeWithText("Browser").assertIsDisplayed()
            composeRule.onNodeWithText("Camera").assertIsDisplayed()
            composeRule.onNodeWithText("Mail").assertIsDisplayed()
        }
    }
}
