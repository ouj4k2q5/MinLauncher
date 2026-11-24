package app.minlauncher.ui.drawer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import app.minlauncher.testing.TestMainActivity
import app.minlauncher.ui.fakeApp
import app.minlauncher.ui.launchLauncher
import app.minlauncher.ui.openDrawer
import app.minlauncher.ui.otherProfileUser
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
class DrawerOtherProfileIndicatorTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `the other-profile dot sits in the 10dp gutter, not inside the title padding`() {
        launchLauncher(listOf(fakeApp("Mail", user = otherProfileUser()))).use { scenario ->
            scenario.openDrawer()

            val dot =
                composeRule
                    .onNodeWithTag("drawer.otherProfileIndicator", useUnmergedTree = true)
                    .assertIsDisplayed()
                    .fetchSemanticsNode()
            val density = composeRule.density

            assertEquals(
                "the dot must start 10dp from the row edge",
                with(density) { 10.dp.toPx() },
                dot.positionInRoot.x,
                0.5f,
            )
            assertEquals(
                "the dot stays 5dp wide, before the 24dp start padding",
                with(density) { 5.dp.toPx() },
                dot.size.width.toFloat(),
                0.5f,
            )
        }
    }

    @Test
    fun `own-profile apps have no dot`() {
        launchLauncher(listOf(fakeApp("Mail"))).use { scenario ->
            scenario.openDrawer()

            composeRule
                .onNodeWithTag("drawer.otherProfileIndicator", useUnmergedTree = true)
                .assertDoesNotExist()
        }
    }
}
