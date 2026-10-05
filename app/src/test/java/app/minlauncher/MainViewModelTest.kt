package app.minlauncher

import android.app.Application
import android.os.Process
import android.view.Gravity
import androidx.activity.ComponentActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    private val testApp =
        AppModel.App(
            appLabel = "Test App",
            key = null,
            appPackage = "app.minlauncher.test.target",
            activityClassName = "app.minlauncher.test.target.MainActivity",
            user = Process.myUserHandle(),
        )

    private val testShortcut =
        AppModel.PinnedShortcut(
            appLabel = "Test Shortcut",
            key = null,
            appPackage = "app.minlauncher.test.target",
            shortcutId = "shortcut-1",
            user = Process.myUserHandle(),
        )

    @Before
    fun setUp() {
        prefs = Prefs(application)
        viewModel = MainViewModel(application)
    }

    @Test
    fun `selectedApp saves app fields to home slot 1`() {
        viewModel.selectedApp(testApp, Constants.FLAG_SET_HOME_APP_1)

        assertEquals("Test App", prefs.appName1)
        assertEquals("app.minlauncher.test.target", prefs.appPackage1)
        assertEquals(Process.myUserHandle().toString(), prefs.appUser1)
        assertEquals("app.minlauncher.test.target.MainActivity", prefs.appActivityClassName1)
        assertFalse(prefs.isShortcut1)
        assertEquals("", prefs.shortcutId1)
    }

    @Test
    fun `selectedApp saves pinned shortcut to home slot 8`() {
        viewModel.selectedApp(testShortcut, Constants.FLAG_SET_HOME_APP_8)

        assertEquals("Test Shortcut", prefs.appName8)
        assertEquals("app.minlauncher.test.target", prefs.appPackage8)
        assertEquals(Process.myUserHandle().toString(), prefs.appUser8)
        assertEquals("", prefs.appActivityClassName8)
        assertTrue(prefs.isShortcut8)
        assertEquals("shortcut-1", prefs.shortcutId8)
    }

    @Test
    fun `selectedApp saves an app to every home slot`() {
        // FLAG_SET_HOME_APP_n == n, so the slot number doubles as the flag.
        for (slot in 1..8) {
            viewModel.selectedApp(testApp, slot)

            assertEquals("Test App", prefs.getAppName(slot))
            assertEquals("app.minlauncher.test.target", prefs.getAppPackage(slot))
            assertEquals(Process.myUserHandle().toString(), prefs.getAppUser(slot))
            assertEquals("app.minlauncher.test.target.MainActivity", prefs.getAppActivityClassName(slot))
            assertFalse(prefs.getIsShortcut(slot))
            assertEquals("", prefs.getShortcutId(slot))
        }
    }

    @Test
    fun `selectedApp saves a pinned shortcut to every home slot`() {
        for (slot in 1..8) {
            viewModel.selectedApp(testShortcut, slot)

            assertEquals("Test Shortcut", prefs.getAppName(slot))
            assertEquals("app.minlauncher.test.target", prefs.getAppPackage(slot))
            assertEquals(Process.myUserHandle().toString(), prefs.getAppUser(slot))
            assertEquals("", prefs.getAppActivityClassName(slot))
            assertTrue(prefs.getIsShortcut(slot))
            assertEquals("shortcut-1", prefs.getShortcutId(slot))
        }
    }

    @Test
    fun `saving an app over a pinned shortcut clears the shortcut fields`() {
        viewModel.selectedApp(testShortcut, Constants.FLAG_SET_HOME_APP_1)
        viewModel.selectedApp(testApp, Constants.FLAG_SET_HOME_APP_1)

        assertEquals("Test App", prefs.appName1)
        assertEquals("app.minlauncher.test.target.MainActivity", prefs.appActivityClassName1)
        assertFalse(prefs.isShortcut1)
        assertEquals("", prefs.shortcutId1)
    }

    @Test
    fun `selectedApp ignores private space header for home slots`() {
        viewModel.selectedApp(AppModel.PrivateSpaceHeader(isLocked = true), Constants.FLAG_SET_HOME_APP_1)

        assertEquals("", prefs.appName1)
        assertEquals("", prefs.appPackage1)
        assertNull(viewModel.refreshHome.value)
    }

    @Test
    fun `selectedApp saves app to swipe left action`() {
        viewModel.selectedApp(testApp, Constants.FLAG_SET_SWIPE_LEFT_APP)

        assertEquals("Test App", prefs.appNameSwipeLeft)
        assertEquals("app.minlauncher.test.target", prefs.appPackageSwipeLeft)
        assertEquals("app.minlauncher.test.target.MainActivity", prefs.appActivityClassNameSwipeLeft)
        assertFalse(prefs.isShortcutSwipeLeft)
        assertEquals("", prefs.shortcutIdSwipeLeft)
        assertEquals(Unit, viewModel.updateSwipeApps.value)
    }

    @Test
    fun `selectedApp saves pinned shortcut to swipe right action`() {
        viewModel.selectedApp(testShortcut, Constants.FLAG_SET_SWIPE_RIGHT_APP)

        assertEquals("Test Shortcut", prefs.appNameSwipeRight)
        assertEquals("app.minlauncher.test.target", prefs.appPackageSwipeRight)
        assertEquals("", prefs.appActivityClassNameRight)
        assertTrue(prefs.isShortcutSwipeRight)
        assertEquals("shortcut-1", prefs.shortcutIdSwipeRight)
    }

    @Test
    fun `saving a pinned shortcut over an app clears the activity class name`() {
        viewModel.selectedApp(testApp, Constants.FLAG_SET_SWIPE_LEFT_APP)
        viewModel.selectedApp(testShortcut, Constants.FLAG_SET_SWIPE_LEFT_APP)

        assertEquals("Test Shortcut", prefs.appNameSwipeLeft)
        assertEquals("app.minlauncher.test.target", prefs.appPackageSwipeLeft)
        assertEquals(Process.myUserHandle().toString(), prefs.appUserSwipeLeft)
        assertEquals("", prefs.appActivityClassNameSwipeLeft)
        assertTrue(prefs.isShortcutSwipeLeft)
        assertEquals("shortcut-1", prefs.shortcutIdSwipeLeft)
    }

    @Test
    fun `updateHomeAlignment persists and exposes the new gravity`() {
        viewModel.updateHomeAlignment(Gravity.CENTER)

        assertEquals(Gravity.CENTER, prefs.homeAlignment)
        assertEquals(Gravity.CENTER, viewModel.homeAppAlignment.value)
    }

    @Test
    fun `firstOpen posts the given value`() {
        viewModel.firstOpen(false)

        assertEquals(false, viewModel.firstOpen.value)
    }

    @Test
    fun `default factory path constructs MainViewModel`() {
        // The (Application) secondary constructor must keep working because
        // ViewModelProvider's default factory (used by MainActivity and the
        // fragments) can only call that constructor.
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val vm = ViewModelProvider(activity)[MainViewModel::class.java]
        assertNotNull(vm)
    }

    @Test
    fun `saving a home app triggers a home refresh`() {
        viewModel.selectedApp(testApp, Constants.FLAG_SET_HOME_APP_2)

        assertEquals(false, viewModel.refreshHome.value)
    }
}
