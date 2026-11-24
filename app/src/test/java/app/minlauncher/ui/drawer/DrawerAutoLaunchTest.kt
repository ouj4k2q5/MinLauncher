package app.minlauncher.ui.drawer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ActivityScenario
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.testing.TestMainActivity
import app.minlauncher.ui.fakeApps
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
class DrawerAutoLaunchTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private class Harness(
        val scenario: ActivityScenario<TestMainActivity>,
        val query: MutableState<TextFieldValue>,
        val filtered: MutableState<List<AppModel>>,
        val userEdited: MutableState<Boolean>,
        val launched: MutableList<AppModel>,
    ) : AutoCloseable {
        override fun close() = scenario.close()
    }

    /**
     * The auto-launch effect in isolation, hosted in a ComposeView on the test
     * activity (this rule launches no activity of its own). Query, filtered
     * list and the user-edited gate are snapshot state owned by the test, so it
     * can restore a non-empty query into a fresh composition, type into it
     * afterwards, or simulate the async arrival of the app list.
     */
    private fun launchHarness(
        initialQuery: String,
        initialFiltered: List<AppModel>,
    ): Harness {
        val query = mutableStateOf(TextFieldValue(initialQuery))
        val filtered = mutableStateOf(initialFiltered)
        val userEdited = mutableStateOf(false)
        val launched = mutableListOf<AppModel>()
        val scenario = ActivityScenario.launch(TestMainActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContentView(
                ComposeView(activity).apply {
                    setContent {
                        autoLaunchOnSingleResult(
                            filtered = filtered.value,
                            query = query.value,
                            userEditedQuery = userEdited.value,
                            flag = Constants.FLAG_LAUNCH_APP,
                            callbacks = drawerCallbacks { launched += it },
                        )
                    }
                },
            )
        }
        composeRule.waitForIdle()
        return Harness(scenario, query, filtered, userEdited, launched)
    }

    private fun drawerCallbacks(onAppClick: (AppModel) -> Unit): DrawerCallbacks =
        DrawerCallbacks(
            onAppClick = onAppClick,
            onAppInfo = {},
            onAppDelete = {},
            onAppHide = {},
            onAppRename = { _, _ -> },
            onRenameHomeSlot = { false },
            onPrivateSpaceToggle = {},
            onPrivateSpaceSettings = {},
            onExitDrawer = {},
            onBangSearch = {},
            onOpenSearch = {},
            isCjkKeyboard = { false },
        )

    // A non-empty query at the first composition can only come from
    // rememberSaveable restore (rotation / process death); it must not launch.
    @Test
    fun `a restored query does not auto-launch on the first run`() {
        launchHarness(initialQuery = "map", initialFiltered = fakeApps("Maps")).use { harness ->
            assertTrue(
                "a restored query must not auto-launch, launched=${harness.launched}",
                harness.launched.isEmpty(),
            )
        }
    }

    @Test
    fun `typing a sole match after an empty initial query auto-launches`() {
        launchHarness(initialQuery = "", initialFiltered = emptyList()).use { harness ->
            composeRule.runOnIdle {
                harness.userEdited.value = true
                harness.query.value = TextFieldValue("maps")
                harness.filtered.value = fakeApps("Maps")
            }
            composeRule.waitForIdle()

            assertEquals(listOf("Maps"), harness.launched.map { it.appLabel })
        }
    }

    @Test
    fun `a list arriving after restore does not auto-launch`() {
        launchHarness(initialQuery = "map", initialFiltered = emptyList()).use { harness ->
            // Process death: the first composition sees an empty list (the app list
            // loads asynchronously) and only then the sole match arrives.
            composeRule.runOnIdle { harness.filtered.value = fakeApps("Maps") }
            composeRule.waitForIdle()

            assertTrue(
                "an asynchronously arriving list must not auto-launch a restored query, launched=${harness.launched}",
                harness.launched.isEmpty(),
            )
        }
    }

    @Test
    fun `narrowing a restored query to one result does not auto-launch`() {
        launchHarness(initialQuery = "map", initialFiltered = fakeApps("Maps", "Mail")).use { harness ->
            assertTrue(
                "the restored query's first run must not launch, launched=${harness.launched}",
                harness.launched.isEmpty(),
            )

            composeRule.runOnIdle { harness.filtered.value = fakeApps("Maps") }
            composeRule.waitForIdle()

            // A package-update-like list change must not launch a restored query:
            // the old View-based SearchView lost the query on rotation, so there
            // is no old behavior to preserve here, and launching would re-open
            // the process-death hole.
            assertTrue(
                "a restored query must not launch on a later list change, launched=${harness.launched}",
                harness.launched.isEmpty(),
            )
        }
    }

    // Typing after a restore re-enables auto-launch: the gate is the user's
    // edit during this composition, not the restored text. The narrowed list is
    // what makes the effect run again (it keys on the filtered list).
    @Test
    fun `typing after restore re-enables auto-launch`() {
        launchHarness(initialQuery = "map", initialFiltered = fakeApps("Maps", "Mail")).use { harness ->
            composeRule.runOnIdle {
                harness.userEdited.value = true
                harness.query.value = TextFieldValue("maps")
                harness.filtered.value = fakeApps("Maps")
            }
            composeRule.waitForIdle()

            assertEquals(listOf("Maps"), harness.launched.map { it.appLabel })
        }
    }
}
