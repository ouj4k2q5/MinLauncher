package app.minlauncher.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import app.minlauncher.R
import app.minlauncher.data.Constants
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class HideAppFlowTest {
    @Test
    fun `hiding an app removes it from the drawer and records it in prefs`() {
        val camera = fakeApp("Camera")
        launchLauncher(fakeApps("Browser", "Camera", "Mail")) { firstHide = false }.use { scenario ->
            scenario.openDrawer()

            onView(withText("Camera")).perform(longClick())
            onView(allOf(withId(R.id.appHide), isDisplayed())).perform(click())

            // removeApp goes through submitList, so the row disappears via an
            // async diff; not(isDisplayed()) retries until that lands.
            onView(withText("Camera")).check(matches(not(isDisplayed())))
            onView(withText("Browser")).check(matches(isDisplayed()))
            onView(withText("Mail")).check(matches(isDisplayed()))
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

            onView(withText("Camera")).check(doesNotExist())
            onView(withText("Browser")).check(matches(isDisplayed()))
            onView(withText("Mail")).check(matches(isDisplayed()))
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
            onView(withText("Camera")).check(matches(isDisplayed()))

            onView(withText("Camera")).perform(longClick())
            onView(allOf(withId(R.id.appHide), isDisplayed())).perform(click())

            assertEquals(emptySet<String>(), testPrefs().hiddenApps)
        }
    }
}
