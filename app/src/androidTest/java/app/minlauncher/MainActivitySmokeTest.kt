package app.minlauncher

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.minlauncher.data.Prefs
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device smoke tests for the app as installed from the debug APK. They
 * prove that the app launches and its three core flows work on a real
 * (emulated) Android at minSdk; exact behavior is covered by the Robolectric
 * suite. Run with ./gradlew pixel2api30DebugAndroidTest.
 */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @Before
    fun setUp() {
        disableSystemAnimations()
        // Skip the first-open flow: it fires the system "choose home app"
        // dialog, which covers the activity and breaks every subsequent
        // Espresso interaction. The flow itself is covered by the Robolectric
        // suite; here we assert steady-state behavior.
        Prefs(InstrumentationRegistry.getInstrumentation().targetContext).firstOpen = false
    }

    @Test
    fun `launch displays the home clock`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.clock)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `swipe up opens the drawer with apps`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Not ViewActions.swipeUp(): it starts at BOTTOM_CENTER, which on
            // gesture navigation lies inside the system home-gesture zone, so
            // the system steals the stroke and the launcher never sees it.
            // Swipe from screen center to top instead.
            onView(withId(R.id.mainLayout)).perform(
                GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER, GeneralLocation.TOP_CENTER, Press.FINGER),
            )

            onView(withId(R.id.recyclerView)).perform(waitForRecyclerViewItems())
        }
    }

    @Test
    fun `long press on the home screen opens settings`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.mainLayout)).perform(holdLongPress())

            onView(withId(R.id.appInfo)).check(matches(isDisplayed()))
        }
    }
}
