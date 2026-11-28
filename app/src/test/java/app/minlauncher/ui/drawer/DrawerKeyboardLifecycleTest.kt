package app.minlauncher.ui.drawer

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.navigation3.runtime.NavBackStack
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.minlauncher.DrawerViewModel
import app.minlauncher.data.Constants
import app.minlauncher.data.LauncherRepository
import app.minlauncher.data.Prefs
import app.minlauncher.helper.FakeAppListProvider
import app.minlauncher.testing.TestMainActivity
import app.minlauncher.ui.nav.DrawerKey
import app.minlauncher.ui.nav.HomeKey
import app.minlauncher.ui.nav.MinLauncherNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.

/**
 * Pins the drawer keyboard lifecycle against the Nav3 zombie-entry state: a
 * popped entry stays composed until its exit transition ends — and across an
 * activity stopped mid-transition — so the keyboard must hide the moment the
 * stack moves on, and ON_START must not re-show it for the lingering entry.
 */
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class DrawerKeyboardLifecycleTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private fun createDeps(
        key: DrawerKey,
        state: DrawerEntryState,
        hideRecorder: () -> Unit,
    ): Pair<DrawerEntryDeps, NavBackStack<MinLauncherNavKey>> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val application = context.applicationContext as Application
        val backStack: NavBackStack<MinLauncherNavKey> = NavBackStack(HomeKey, key)
        val deps =
            DrawerEntryDeps(
                context = context,
                prefs = Prefs(context),
                viewModel =
                    DrawerViewModel(
                        application,
                        LauncherRepository(context, FakeAppListProvider()),
                    ),
                key = key,
                backStack = backStack,
                state = state,
                hideKeyboard = hideRecorder,
            )
        return deps to backStack
    }

    /** Hosts the given content in a ComposeView on the test activity. */
    private fun host(content: @Composable () -> Unit): ActivityScenario<TestMainActivity> {
        val scenario = ActivityScenario.launch(TestMainActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContentView(ComposeView(activity).apply { setContent(content) })
        }
        composeRule.waitForIdle()
        return scenario
    }

    @Test
    fun `on start does not re-show the keyboard once the drawer is no longer the current entry`() {
        val owner = TestLifecycleOwner()
        val state = DrawerEntryState()
        val key = DrawerKey(Constants.FLAG_LAUNCH_APP, canRename = false)
        val (deps, backStack) = createDeps(key, state) {}

        host { DrawerStartStopParity(owner, deps) }.use {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            composeRule.waitForIdle()
            assertEquals(1, state.keyboardShowSignal)

            // The popped entry lingers composed while home is the current
            // destination (an activity stopped mid-transition keeps it there
            // across the stop); ON_START in that state — waking the screen or
            // returning from an app — must not re-show the keyboard over home.
            backStack.removeAt(backStack.lastIndex)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            composeRule.waitForIdle()

            assertEquals(1, state.keyboardShowSignal)
        }
    }

    @Test
    fun `keyboard hides as soon as the drawer leaves the top of the back stack`() {
        var hidden = false
        val state = DrawerEntryState()
        val key = DrawerKey(Constants.FLAG_LAUNCH_APP, canRename = false)
        val (deps, backStack) = createDeps(key, state) { hidden = true }

        host { hideKeyboardWhenNoLongerTop(deps) }.use {
            composeRule.waitForIdle()
            assertFalse(hidden)

            // The hide must not wait for the entry's disposal, which Nav3 defers
            // to the end of the exit transition.
            backStack.removeAt(backStack.lastIndex)
            composeRule.waitForIdle()
            assertTrue(hidden)
        }
    }
}
