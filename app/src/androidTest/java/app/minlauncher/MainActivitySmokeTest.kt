package app.minlauncher

import android.view.ViewConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device smoke tests for the app as installed from the debug APK. They
 * prove that the app launches and its three core flows work on a real
 * (emulated) Android at minSdk; exact behavior is covered by the Robolectric
 * suite. Run with ./gradlew pixel2api30DebugAndroidTest.
 *
 * The activity is the production [MainActivity] hosting the Nav3 `AppRoot`
 * compose tree; the tests drive the same tags the Robolectric suite uses and
 * assert the resulting screens, not View internals.
 */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    // Launching the activity through the rule gives the tests the live Compose
    // tree; the one-time steady-state prefs are set in @BeforeClass so they are
    // in place before this rule creates the activity.
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `launch displays the home clock`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home.clock").assertIsDisplayed()
    }

    @Test
    fun `swipe up opens the drawer with apps`() {
        // Compose tests inject the whole gesture into the app's view hierarchy
        // directly, so the system gesture zone that stole Espresso's
        // bottom-start swipe cannot intercept it. Start at the screen center
        // and fling up to the top edge anyway, matching the fling the home
        // background detector expects (>100px travel, >100px/s).
        composeRule.onNodeWithTag("home.background").performTouchInput {
            swipe(start = center, end = topCenter, durationMillis = 200L)
        }

        // The drawer is populated asynchronously from LauncherApps on a
        // background thread that idling cannot see, so poll the semantics
        // tree. Waiting for the list container alone is not enough — it
        // composes even with a permanently empty list — so also require at
        // least one child node: LazyColumn only composes children for actual
        // items. The item comes from DummyLauncherActivity in this test APK:
        // the aosp-atd image has no other launchable apps, and the drawer
        // never lists the launcher itself.
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            composeRule
                .onAllNodesWithTag("drawer.list")
                .fetchSemanticsNodes()
                .firstOrNull()
                ?.children
                ?.isNotEmpty() == true
        }
    }

    @Test
    fun `long press on the home screen opens settings`() {
        // The home background detector posts its long-press Runnable on the
        // main Handler, so it needs real elapsed time with the main thread
        // free. Compose's performTouchInput flushes the whole gesture into one
        // main-thread batch, which would deliver the up before the Runnable
        // runs, so inject the down/up with Espresso and hold the looper in
        // between, like the pre-Compose test did.
        onView(withId(android.R.id.content)).perform(
            holdLongPress(
                at = backgroundPoint,
                durationMs = Constants.LONG_PRESS_DELAY_MS + ViewConfiguration.getLongPressTimeout() + 250L,
            ),
        )

        composeRule.waitUntil(timeoutMillis = 10_000L) {
            composeRule.onAllNodesWithTag("settings.appInfo").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings.appInfo").assertIsDisplayed()
    }

    companion object {
        // A point on the home background clear of the top-start clock/date
        // column and of the vertically centered home app slots, so the press
        // cannot be consumed by an interactive child. The top centre is not
        // safe: the large clock text is wide enough to reach it on a phone. The
        // 0.9w x-coordinate can overlap the screen-time text (top-end, ~72dp
        // top margin) when state.screenTime != null; it is null on a fresh GMD
        // because usage access is not granted, which is why the point is safe
        // today.
        private val backgroundPoint =
            CoordinatesProvider { view ->
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                floatArrayOf(
                    location[0] + view.width * 0.9f,
                    location[1] + view.height * 0.1f,
                )
            }

        /**
         * Puts the device in the steady state the smoke tests assert, before
         * the rule launches the activity: skip the first-open flow (it fires
         * the system "choose home app" dialog, which covers the activity and
         * breaks every later interaction) and the four-hour background
         * restart (a fresh install would otherwise recreate the activity while
         * the compose rule is attaching to it).
         */
        @JvmStatic
        @BeforeClass
        fun reachSteadyState() {
            disableSystemAnimations()
            Prefs(InstrumentationRegistry.getInstrumentation().targetContext).apply {
                firstOpen = false
                launcherRestartTimestamp = System.currentTimeMillis()
            }
        }
    }
}
