package app.minlauncher.ui.drawer

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import app.minlauncher.testing.TestMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class DrawerScrollKeyboardTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private class FakeKeyboard : SoftwareKeyboardController {
        val calls = mutableListOf<String>()

        override fun show() {
            calls += "show"
        }

        override fun hide() {
            calls += "hide"
        }
    }

    private class Harness(
        val scenario: ActivityScenario<TestMainActivity>,
        val scope: CoroutineScope,
        val listState: LazyListState,
    ) : AutoCloseable {
        override fun close() = scenario.close()
    }

    /**
     * The keyboard-follows-scroll logic in isolation: a tall list plus the
     * effect under test, hosted in a ComposeView on the test activity. The
     * scope and list state are captured so the test can drive scrolls through
     * the same [ScrollableState.scroll] machinery a real drag uses.
     */
    private fun launchHarness(keyboard: FakeKeyboard): Harness {
        var scope: CoroutineScope? = null
        var listState: LazyListState? = null
        val scenario = ActivityScenario.launch(TestMainActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContentView(
                ComposeView(activity).apply {
                    setContent {
                        val state = rememberLazyListState()
                        val coroutineScope = rememberCoroutineScope()
                        val searchFocus = remember { FocusRequester() }
                        Box {
                            LazyColumn(state = state, modifier = Modifier.height(200.dp)) {
                                items(50) { index -> Text("row $index", Modifier.height(40.dp)) }
                            }
                            Box(Modifier.focusRequester(searchFocus).focusable())
                            observeScrollForKeyboard(
                                listState = state,
                                autoShowKeyboard = true,
                                searchFocus = searchFocus,
                                keyboard = keyboard,
                            )
                        }
                        scope = coroutineScope
                        listState = state
                    }
                },
            )
        }
        composeRule.waitForIdle()
        // The composition-time emission fires before the first layout, when
        // canScrollForward is still false — the real app's showKeyboardOnStart
        // would override it right after. Drop that noise so every test only
        // sees its own scrolls.
        keyboard.calls.clear()
        return Harness(scenario, scope!!, listState!!)
    }

    /**
     * Drives a scroll that spans a real frame, like a user drag: the delta is
     * applied first, then one frame runs (apply + measure/layout), and only
     * then does the snapshotFlow reader observe the scroll — with the
     * position already advanced, exactly what a real drag looks like to the
     * collector. The scroll stays in progress across that frame because the
     * block suspends in [withFrameNanos] until the frame has passed.
     */
    private fun scrollWithApply(
        harness: Harness,
        distance: Float,
    ) {
        composeRule.runOnIdle {
            harness.scope.launch {
                harness.listState.scroll {
                    scrollBy(distance)
                    withFrameNanos { }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {}
    }

    @Test
    fun `a scroll that starts at the top hides the keyboard`() {
        val keyboard = FakeKeyboard()
        launchHarness(keyboard).use {
            scrollWithApply(it, distance = 100f)

            assertTrue(
                "scrolling away from the top must hide the keyboard, calls=${keyboard.calls}",
                keyboard.calls.contains("hide"),
            )
        }
    }

    @Test
    fun `a scroll that starts below the top does not hide the keyboard again`() {
        val keyboard = FakeKeyboard()
        launchHarness(keyboard).use {
            scrollWithApply(it, distance = 100f)
            assertEquals(1, keyboard.calls.count { call -> call == "hide" })

            scrollWithApply(it, distance = 100f)

            assertEquals(
                "a drag started mid-list must not fire another hide, calls=${keyboard.calls}",
                1,
                keyboard.calls.count { call -> call == "hide" },
            )
        }
    }

    @Test
    fun `settling at the bottom of the list hides the keyboard`() {
        val keyboard = FakeKeyboard()
        launchHarness(keyboard).use {
            scrollWithApply(it, distance = 100f) // leave the top (hides once at drag start)
            assertEquals(1, keyboard.calls.count { call -> call == "hide" })

            scrollWithApply(it, distance = 20_000f) // reach the bottom; drag started mid-list

            assertEquals(
                "settling at the bottom must hide the keyboard, calls=${keyboard.calls}",
                2,
                keyboard.calls.count { call -> call == "hide" },
            )
        }
    }

    @Test
    fun `settling back at the top re-shows the keyboard`() {
        val keyboard = FakeKeyboard()
        launchHarness(keyboard).use {
            scrollWithApply(it, distance = 100f)
            assertEquals(1, keyboard.calls.count { call -> call == "hide" })

            scrollWithApply(it, distance = -100f)

            assertEquals(
                "returning to the top after a mid-list start must re-show, calls=${keyboard.calls}",
                "show",
                keyboard.calls.last(),
            )
        }
    }
}
