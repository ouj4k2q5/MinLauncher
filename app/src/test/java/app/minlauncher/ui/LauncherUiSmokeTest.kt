package app.minlauncher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import app.minlauncher.R
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
    // The drawer is Compose; the empty rule drives its semantics tree while
    // the tests launch their own ActivityScenario.
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `home screen is shown after launch`() {
        launchLauncher(fakeApps("Browser", "Camera", "Mail")).use {
            // The clock itself stays zero-width until its first tick under
            // Robolectric, so assert on its always-laid-out container.
            onView(withId(R.id.dateTimeLayout)).check(matches(isDisplayed()))
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
