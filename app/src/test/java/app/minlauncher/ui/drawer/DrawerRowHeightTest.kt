package app.minlauncher.ui.drawer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import app.minlauncher.testing.TestMainActivity
import app.minlauncher.ui.fakeApps
import app.minlauncher.ui.launchLauncher
import app.minlauncher.ui.openDrawer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Pin the Robolectric SDK to targetSdk (36).
@Config(sdk = [36])
@RunWith(RobolectricTestRunner::class)
class DrawerRowHeightTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `opening the action menu does not shift the rows below`() {
        launchLauncher(fakeApps("Browser", "Camera", "Mail", "Maps", "Music")).use { scenario ->
            scenario.openDrawer()

            val cameraBefore =
                composeRule
                    .onNodeWithText("Camera")
                    .fetchSemanticsNode()
                    .positionInRoot.y

            composeRule.onNodeWithText("Browser").performTouchInput { longClick() }
            composeRule.onNodeWithTag("drawer.appDelete").assertIsDisplayed()

            val cameraAfter =
                composeRule
                    .onNodeWithText("Camera")
                    .fetchSemanticsNode()
                    .positionInRoot.y
            assertEquals(
                "the menu row must keep the title row's height",
                cameraBefore,
                cameraAfter,
                0.01f,
            )
        }
    }

    @Test
    fun `opening the rename row does not shift the rows below`() {
        launchLauncher(fakeApps("Browser", "Camera", "Mail", "Maps", "Music")).use { scenario ->
            scenario.openDrawer()

            val cameraBefore =
                composeRule
                    .onNodeWithText("Camera")
                    .fetchSemanticsNode()
                    .positionInRoot.y

            composeRule.onNodeWithText("Browser").performTouchInput { longClick() }
            composeRule.onNodeWithTag("drawer.appRename").performClick()

            composeRule.onNodeWithTag("drawer.renameInput").assertIsDisplayed()

            val cameraAfter =
                composeRule
                    .onNodeWithText("Camera")
                    .fetchSemanticsNode()
                    .positionInRoot.y
            assertEquals(
                "the rename row must keep the title row's height",
                cameraBefore,
                cameraAfter,
                0.01f,
            )
        }
    }
}
