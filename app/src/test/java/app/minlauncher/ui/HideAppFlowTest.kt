package app.minlauncher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import app.minlauncher.data.Constants
import app.minlauncher.testing.TestMainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class HideAppFlowTest {
    // The drawer is Compose; the empty rule drives its semantics tree while
    // the tests launch their own ActivityScenario.
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `hiding an app removes it from the drawer and records it in prefs`() {
        val camera = fakeApp("Camera")
        launchLauncher(fakeApps("Browser", "Camera", "Mail")) { firstHide = false }.use { scenario ->
            scenario.openDrawer()

            composeRule.onNodeWithText("Camera").performTouchInput { longClick() }
            composeRule.onNodeWithTag("drawer.appHide").performClick()

            // The removal is an optimistic state update in the Fragment;
            // settle the recomposition before asserting.
            composeRule.waitForIdle()
            // While the row menu is open the title is alpha(0f) with cleared
            // semantics, so the menu having closed proves the hide succeeded;
            // only then can the Camera-absence assertion mean anything.
            composeRule.onNodeWithTag("drawer.appHide").assertDoesNotExist()
            composeRule.onNodeWithText("Camera").assertDoesNotExist()
            composeRule.onNodeWithText("Browser").assertIsDisplayed()
            composeRule.onNodeWithText("Mail").assertIsDisplayed()
            assertEquals(setOf("${camera.appPackage}|${camera.user}"), testPrefs().hiddenApps)
        }
    }

    @Test
    fun `an app hidden in prefs is absent from the regular drawer`() {
        val camera = fakeApp("Camera")
        launchLauncher(
            apps = fakeApps("Browser", "Mail"),
            hidden = listOf(camera),
        ) {
            firstHide = false
            hiddenApps = mutableSetOf("${camera.appPackage}|${camera.user}")
        }.use { scenario ->
            scenario.openDrawer()

            composeRule.onNodeWithText("Camera").assertDoesNotExist()
            composeRule.onNodeWithText("Browser").assertIsDisplayed()
            composeRule.onNodeWithText("Mail").assertIsDisplayed()
        }
    }

    @Test
    fun `the hidden apps drawer shows the flag-specific search hint`() {
        launchLauncher(fakeApps("Browser")) { firstHide = false }.use { scenario ->
            scenario.openDrawer(flag = Constants.FLAG_HIDDEN_APPS)

            composeRule.onNodeWithText("Hidden apps").assertIsDisplayed()
        }
    }

    @Test
    fun `an app hidden in prefs is restored from the hidden apps list`() {
        val camera = fakeApp("Camera")
        launchLauncher(
            apps = fakeApps("Browser", "Mail"),
            hidden = listOf(camera),
        ) {
            firstHide = false
            hiddenApps = mutableSetOf("${camera.appPackage}|${camera.user}")
        }.use { scenario ->
            scenario.openDrawer(flag = Constants.FLAG_HIDDEN_APPS)
            composeRule.onNodeWithText("Camera").assertIsDisplayed()

            composeRule.onNodeWithText("Camera").performTouchInput { longClick() }
            composeRule.onNodeWithTag("drawer.appHide").performClick()
            // The restore also closes the row menu; assert it so a pass cannot
            // come from the menu still covering the drawer.
            composeRule.onNodeWithTag("drawer.appHide").assertDoesNotExist()

            assertEquals(emptySet<String>(), testPrefs().hiddenApps)
        }
    }
}
