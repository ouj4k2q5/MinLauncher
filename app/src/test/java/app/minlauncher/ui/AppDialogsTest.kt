package app.minlauncher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.minlauncher.testing.TestMainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Pin the Robolectric SDK to targetSdk (36).
@Config(sdk = [36])
@RunWith(RobolectricTestRunner::class)
class AppDialogsTest {
    // Empty rule: the test launches its own ActivityScenario (the first-open
    // seed has to happen before the activity starts), then drives the
    // Compose tree, including the dialog's separate window.
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `dialog signal shows the about dialog`() {
        // firstSettingsOpen stays at its default true, so opening Settings posts
        // Constants.Dialog.ABOUT, the signal the legacy overlay rendered.
        launchLauncher(fakeApps("Browser")) { firstOpen = false }.use {
            openSettings()

            val aboutBody = "Thank you for choosing MinLauncher."
            composeRule.onNodeWithText(aboutBody, substring = true).assertIsDisplayed()

            composeRule.onNodeWithText("OK").performClick()

            composeRule.onNodeWithText(aboutBody, substring = true).assertDoesNotExist()
        }
    }
}
