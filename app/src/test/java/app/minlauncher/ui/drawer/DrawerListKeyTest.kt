package app.minlauncher.ui.drawer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.minlauncher.testing.TestMainActivity
import app.minlauncher.ui.fakeApp
import app.minlauncher.ui.launchLauncher
import app.minlauncher.ui.openDrawer
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class DrawerListKeyTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `two launcher activities of one package render as separate rows`() {
        // RealAppListProvider emits one row per launchable activity, with no
        // package dedup, so the list key must include the activity class or
        // LazyColumn throws on the duplicate key and the drawer never opens.
        val sharedPackage = "app.minlauncher.ui.fixture.dual"
        val main =
            fakeApp(
                label = "Dual Main",
                appPackage = sharedPackage,
                activityClassName = "$sharedPackage.MainActivity",
            )
        val alias =
            fakeApp(
                label = "Dual Alias",
                appPackage = sharedPackage,
                activityClassName = "$sharedPackage.AliasActivity",
            )

        launchLauncher(listOf(main, alias)).use { scenario ->
            scenario.openDrawer()

            composeRule.onNodeWithText("Dual Main").assertIsDisplayed()
            composeRule.onNodeWithText("Dual Alias").assertIsDisplayed()
        }
    }
}
