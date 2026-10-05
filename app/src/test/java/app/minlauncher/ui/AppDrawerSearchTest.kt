package app.minlauncher.ui

import androidx.appcompat.R.id.search_src_text
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class AppDrawerSearchTest {
    private val apps = fakeApps("Browser", "Camera", "Mail", "Maps", "Music")

    @Test
    fun `typing a query narrows the drawer to matching apps`() {
        launchLauncher(apps).use { scenario ->
            scenario.openDrawer()

            onView(withText("Browser")).check(matches(isDisplayed()))

            onView(withId(search_src_text)).perform(typeText("ma"))

            onView(withText("Mail")).check(matches(isDisplayed()))
            onView(withText("Maps")).check(matches(isDisplayed()))
            onView(withText("Browser")).check(doesNotExist())
            onView(withText("Camera")).check(doesNotExist())
            onView(withText("Music")).check(doesNotExist())
        }
    }

    @Test
    fun `clearing the query restores the full list`() {
        launchLauncher(apps).use { scenario ->
            scenario.openDrawer()

            onView(withId(search_src_text)).perform(replaceText("ma"))
            onView(withText("Browser")).check(doesNotExist())

            onView(withId(search_src_text)).perform(replaceText(""))

            onView(withText("Browser")).check(matches(isDisplayed()))
            onView(withText("Camera")).check(matches(isDisplayed()))
            onView(withText("Music")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `a non-matching query empties the list without crashing`() {
        launchLauncher(apps).use { scenario ->
            scenario.openDrawer()

            onView(withId(search_src_text)).perform(typeText("zzz"))

            onView(withText("Browser")).check(doesNotExist())
            onView(withText("Camera")).check(doesNotExist())
            onView(withText("Mail")).check(doesNotExist())
            onView(withText("Maps")).check(doesNotExist())
            onView(withText("Music")).check(doesNotExist())
        }
    }
}
