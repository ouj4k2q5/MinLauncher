package app.minlauncher.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import app.minlauncher.testing.TestMainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class SetHomeAppFlowTest {
    @After
    fun clearTestAppListProvider() {
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `long press on a home slot opens select mode and saves the chosen app to the slot`() {
        val browser = fakeApp("Browser")
        val camera = fakeApp("Camera")

        launchLauncher(listOf(browser, camera)) {
            firstOpen = false
            firstSettingsOpen = false
            appName1 = "Browser"
            appPackage1 = browser.appPackage
            appActivityClassName1 = browser.activityClassName
            isShortcut1 = false
        }.use {
            // The seeded slot shows its app.
            onView(withText("Browser")).check(matches(isDisplayed()))

            // Long press on the slot opens the drawer in select mode.
            onView(withText("Browser")).perform(holdLongPress())

            // Selecting an app closes the drawer and updates the slot.
            onView(withText("Camera")).perform(click())

            onView(withText("Camera")).check(matches(isDisplayed()))
            onView(withText("Browser")).check(doesNotExist())

            // The choice is persisted as the home app for slot 1.
            val prefs = testPrefs()
            assertEquals("Camera", prefs.appName1)
            assertEquals(camera.appPackage, prefs.appPackage1)
            assertEquals(camera.activityClassName, prefs.appActivityClassName1)
            assertFalse(prefs.isShortcut1)
        }
    }
}
