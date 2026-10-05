package app.minlauncher.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import app.minlauncher.R
import app.minlauncher.testing.TestMainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class SettingsFlowTest {
    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    private fun openSettings() {
        // Long press on the left edge of the home screen, clear of the home
        // app slots, lands on the background where a long press opens settings.
        onView(withId(R.id.mainLayout)).perform(
            holdLongPress(
                at = { view ->
                    floatArrayOf(view.width * 0.02f, view.height * 0.5f)
                },
            ),
        )
    }

    @Test
    fun `long press on the home background opens settings`() {
        launchLauncher(fakeApps("Browser")) { firstOpen = false }.use {
            openSettings()

            onView(withId(R.id.minlauncherHiddenApps)).check(matches(isDisplayed()))
            onView(withId(R.id.alignment)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.appThemeText)).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `changing home alignment from settings persists it`() {
        launchLauncher(fakeApps("Browser")) { firstOpen = false }.use {
            openSettings()

            onView(withId(R.id.alignment)).perform(scrollTo(), click())
            onView(withId(R.id.alignmentCenter)).perform(click())

            assertEquals(android.view.Gravity.CENTER, testPrefs().homeAlignment)
        }
    }

    @Test
    fun `switching the theme to light persists it`() {
        launchLauncher(fakeApps("Browser")) { firstOpen = false }.use {
            openSettings()

            onView(withId(R.id.appThemeText)).perform(scrollTo(), click())
            onView(withId(R.id.themeLight)).perform(click())

            assertEquals(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,
                testPrefs().appTheme,
            )
        }
    }
}
