package app.minlauncher.ui

import android.app.Application
import android.os.Looper
import android.provider.Settings
import android.view.ViewConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import app.minlauncher.data.Constants
import app.minlauncher.testing.TestMainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class SettingsFlowTest {
    // Empty rule: the test launches its own ActivityScenario (per-test seeding
    // has to happen before the activity starts), then drives the Compose tree.
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `long press on the home background opens settings`() {
        launchLauncher(fakeApps("Browser")) { firstOpen = false }.use {
            // Drive the production wiring (home.root's long-press callback adds
            // SettingsKey). The detector fires only after the extra
            // LONG_PRESS_DELAY_MS on top of the system timeout, so hold a
            // background point — top center, clear of the clock/date at the
            // top start and of the centered app slots — and advance the shadow
            // clock, like SetHomeAppFlowTest's slot long press.
            composeRule.onNodeWithTag("home.root").performTouchInput {
                down(Offset(width * 0.5f, height * 0.1f))
            }
            shadowOf(Looper.getMainLooper()).idleFor(
                Constants.LONG_PRESS_DELAY_MS + ViewConfiguration.getLongPressTimeout() + 100,
                TimeUnit.MILLISECONDS,
            )
            // Nav3 keeps one Compose root alive, so release the pointer before
            // letting the navigation settle.
            composeRule.onRoot().performTouchInput { up() }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("settings.hiddenApps").assertIsDisplayed()
            composeRule.onNodeWithTag("settings.alignment").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("settings.appTheme").performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun `long press on the lock row opens the accessibility settings`() {
        launchLauncher(fakeApps("Browser")) { firstOpen = false }.use {
            openSettings()

            val shadowApplication = shadowOf(ApplicationProvider.getApplicationContext<Application>())
            // The activity launch itself is queued here; drain it first.
            while (shadowApplication.nextStartedActivity != null) {
                // keep draining
            }

            composeRule.onNodeWithTag("settings.toggleLock").performScrollTo().performTouchInput { longClick() }

            assertEquals(
                Settings.ACTION_ACCESSIBILITY_SETTINGS,
                shadowApplication.nextStartedActivity?.action,
            )
        }
    }

    @Test
    fun `changing home alignment from settings persists it`() {
        launchLauncher(fakeApps("Browser")) { firstOpen = false }.use {
            openSettings()

            composeRule.onNodeWithTag("settings.alignment").performScrollTo().performClick()
            composeRule.onNodeWithTag("settings.alignment.center").performClick()

            assertEquals(android.view.Gravity.CENTER, testPrefs().homeAlignment)
        }
    }

    @Test
    fun `text size preview survives a round trip through the hidden apps screen`() {
        launchLauncher(fakeApps("Browser")) { firstOpen = false }.use { scenario ->
            openSettings()

            composeRule.onNodeWithTag("settings.textSize").performScrollTo().performClick()
            composeRule.onNodeWithTag("settings.textSize.plus").performClick()
            composeRule.onNodeWithTag("settings.textSize.current").assertTextEquals("1.1")

            scenario.openDrawer(flag = Constants.FLAG_HIDDEN_APPS)
            composeRule.onNodeWithText("Hidden apps").assertIsDisplayed()

            scenario.onActivity { activity ->
                activity.handle!!.backStack.removeLastOrNull()
            }

            composeRule.onNodeWithTag("settings.textSize").performScrollTo().assertTextContains("1.1")
        }
    }

    @Test
    fun `switching the theme to light persists it`() {
        launchLauncher(fakeApps("Browser")) { firstOpen = false }.use {
            openSettings()

            composeRule.onNodeWithTag("settings.appTheme").performScrollTo().performClick()
            composeRule.onNodeWithTag("settings.theme.light").performClick()

            assertEquals(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,
                testPrefs().appTheme,
            )
        }
    }
}
