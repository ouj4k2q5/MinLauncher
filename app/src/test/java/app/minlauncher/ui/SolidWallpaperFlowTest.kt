package app.minlauncher.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import app.minlauncher.R
import app.minlauncher.helper.getColorFromAttr
import app.minlauncher.testing.TestMainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class SolidWallpaperFlowTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `launcher background shows a solid backdrop when the wallpaper setting is on`() {
        launchLauncher(fakeApps("Browser")) {
            firstOpen = false
            solidWallpaper = true
        }.use { scenario ->
            scenario.assertBackgroundColor { activity ->
                activity.getColorFromAttr(R.attr.primaryInverseColor)
            }
        }
    }

    @Test
    fun `launcher background stays transparent when the wallpaper setting is off`() {
        launchLauncher(fakeApps("Browser")) { firstOpen = false }.use { scenario ->
            scenario.assertBackgroundColor { Color.TRANSPARENT }
        }
    }

    @Test
    fun `turning the wallpaper off after on restores the transparent background`() {
        launchLauncher(fakeApps("Browser")) {
            firstOpen = false
        }.use { scenario ->
            openSettings()

            composeRule.onNodeWithTag("settings.wallpaper").performScrollTo().performClick()
            scenario.assertBackgroundColor { activity ->
                activity.getColorFromAttr(R.attr.primaryInverseColor)
            }

            composeRule.onNodeWithTag("settings.wallpaper").performClick()
            assertEquals(false, testPrefs().solidWallpaper)
            scenario.assertBackgroundColor { Color.TRANSPARENT }
        }
    }

    @Test
    fun `long press on the wallpaper row forces the theme and turns it off`() {
        launchLauncher(fakeApps("Browser")) {
            firstOpen = false
            solidWallpaper = true
        }.use { scenario ->
            openSettings()

            var activityBeforeLongPress: TestMainActivity? = null
            scenario.onActivity { activityBeforeLongPress = it }
            composeRule.onNodeWithTag("settings.wallpaper").performScrollTo().performTouchInput { longClick() }
            assertEquals(false, testPrefs().solidWallpaper)
            assertEquals(AppCompatDelegate.MODE_NIGHT_YES, testPrefs().appTheme)
            scenario.assertBackgroundColor { Color.TRANSPARENT }

            // The forced theme must apply immediately, not only after the next
            // recreation for an unrelated reason: the activity itself is
            // recreated, so its onCreate re-reads prefs and applies the theme.
            scenario.onActivity { activity ->
                assertNotSame(activityBeforeLongPress, activity)
            }
        }
    }

    @Test
    fun `switching the theme while the wallpaper is on repaints the background`() {
        launchLauncher(fakeApps("Browser")) {
            firstOpen = false
            solidWallpaper = true
            appTheme = AppCompatDelegate.MODE_NIGHT_NO
        }.use { scenario ->
            openSettings()

            composeRule.onNodeWithTag("settings.appTheme").performScrollTo().performClick()
            composeRule.onNodeWithTag("settings.theme.dark").performClick()
            assertEquals(AppCompatDelegate.MODE_NIGHT_YES, testPrefs().appTheme)
            // Expect the literal dark-theme backdrop, not the activity's own
            // theme attr: if setAppTheme's recreate() stopped happening, a stale
            // light-themed activity would resolve the attr to the light value
            // and match its own (unrepainted) background, hiding the bug.
            scenario.assertBackgroundColor { activity ->
                activity.getColor(R.color.whiteInverse)
            }
        }
    }

    private fun ActivityScenario<TestMainActivity>.assertBackgroundColor(expected: (TestMainActivity) -> Int) {
        onActivity { activity ->
            val background = activity.findViewById<View>(R.id.mainActivityLayout).background as ColorDrawable
            assertEquals(expected(activity), background.color)
        }
    }
}
