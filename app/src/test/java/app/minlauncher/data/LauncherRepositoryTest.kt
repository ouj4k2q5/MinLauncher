package app.minlauncher.data

import android.app.Application
import android.os.Process
import android.view.Gravity
import androidx.test.core.app.ApplicationProvider
import app.minlauncher.helper.FakeAppListProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class LauncherRepositoryTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val prefs = Prefs(application)
    private lateinit var repository: LauncherRepository

    private val appA =
        AppModel.App(
            appLabel = "App A",
            key = null,
            appPackage = "app.minlauncher.test.a",
            activityClassName = "app.minlauncher.test.a.MainActivity",
            user = Process.myUserHandle(),
        )

    private val appB =
        AppModel.App(
            appLabel = "App B",
            key = null,
            appPackage = "app.minlauncher.test.b",
            activityClassName = "app.minlauncher.test.b.MainActivity",
            user = Process.myUserHandle(),
        )

    private val appC =
        AppModel.App(
            appLabel = "App C",
            key = null,
            appPackage = "app.minlauncher.test.c",
            activityClassName = "app.minlauncher.test.c.MainActivity",
            user = Process.myUserHandle(),
        )

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

    private fun repositoryWith(provider: FakeAppListProvider): LauncherRepository =
        LauncherRepository(
            application,
            provider,
            CoroutineScope(Dispatchers.Unconfined),
        )

    @Before
    fun setUp() {
        repository = repositoryWith(FakeAppListProvider(regularApps = listOf(appA, appB, appC)))
    }

    /** Hides the given apps in the prefs, as the fake provider's world reads them. */
    private fun markHidden(vararg apps: AppModel.App) {
        prefs.hiddenApps = apps.mapTo(mutableSetOf()) { "${it.appPackage}|${it.user}" }
    }

    @Test
    fun `getAppList exposes the provider's regular apps`() {
        repository.getAppList()

        assertEquals(listOf(appA, appB, appC), repository.appList.value)
    }

    @Test
    fun `getHiddenApps exposes the provider's hidden apps`() {
        markHidden(appA)
        repository = repositoryWith(FakeAppListProvider(hiddenApps = listOf(appA)))

        repository.getHiddenApps()

        assertEquals(listOf(appA), repository.hiddenApps.value)
    }

    @Test
    fun `getAppList with includeHiddenApps combines regular and hidden apps`() {
        markHidden(appA, appC)
        repository =
            repositoryWith(
                FakeAppListProvider(regularApps = listOf(appB), hiddenApps = listOf(appA, appC)),
            )

        repository.getAppList(includeHiddenApps = true)

        assertEquals(listOf(appB, appA, appC), repository.appList.value)
    }

    @Test
    fun `getAppList without includeHiddenApps excludes hidden apps`() {
        markHidden(appA, appC)
        repository =
            repositoryWith(
                FakeAppListProvider(regularApps = listOf(appB), hiddenApps = listOf(appA, appC)),
            )

        repository.getAppList()

        assertEquals(listOf(appB), repository.appList.value)
    }

    @Test
    fun `getPrivateSpaceAppList reports no private space on Robolectric`() {
        repository.getPrivateSpaceAppList()

        assertEquals(false, repository.privateSpaceAvailable.value)
        assertEquals(true, repository.privateSpaceLocked.value)
        assertEquals(emptyList<AppModel>(), repository.privateSpaceApps.value)
    }

    @Test
    fun `updateHomeAlignment persists and exposes the new gravity`() {
        repository.updateHomeAlignment(Gravity.CENTER)

        assertEquals(Gravity.CENTER, prefs.homeAlignment)
        assertEquals(Gravity.CENTER, repository.homeAppAlignment.value)
    }

    @Test
    fun `refreshHome emits the given flag`() {
        repository.refreshHome(true)

        assertEquals(true, repository.refreshHome.value)
    }

    @Test
    fun `toggleDateTime emits a signal`() {
        repository.toggleDateTime()

        assertNotNull(repository.toggleDateTime.value)
    }

    @Test
    fun `updateSwipeApps emits a signal`() {
        repository.updateSwipeApps()

        assertNotNull(repository.updateSwipeApps.value)
    }

    @Test
    fun `togglePrivateSpaceLock is a no-op without a private space`() {
        repository.togglePrivateSpaceLock()

        assertFalse(repository.isPrivateSpaceToggling)
    }

    @Test
    fun `isMinLauncherDefault sets the flag`() {
        repository.isMinLauncherDefault()

        assertNotNull(repository.isMinLauncherDefault.value)
    }

    @Test
    fun `selectedApp saves app fields to home slot 1`() {
        repository.selectedApp(testApp, Constants.FLAG_SET_HOME_APP_1)

        assertEquals("Test App", prefs.appName1)
        assertEquals("app.minlauncher.test.target", prefs.appPackage1)
        assertEquals(Process.myUserHandle().toString(), prefs.appUser1)
        assertEquals("app.minlauncher.test.target.MainActivity", prefs.appActivityClassName1)
        assertFalse(prefs.isShortcut1)
        assertEquals("", prefs.shortcutId1)
    }

    @Test
    fun `selectedApp saves pinned shortcut to home slot 8`() {
        repository.selectedApp(testShortcut, Constants.FLAG_SET_HOME_APP_8)

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
            repository.selectedApp(testApp, slot)

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
            repository.selectedApp(testShortcut, slot)

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
        repository.selectedApp(testShortcut, Constants.FLAG_SET_HOME_APP_1)
        repository.selectedApp(testApp, Constants.FLAG_SET_HOME_APP_1)

        assertEquals("Test App", prefs.appName1)
        assertEquals("app.minlauncher.test.target.MainActivity", prefs.appActivityClassName1)
        assertFalse(prefs.isShortcut1)
        assertEquals("", prefs.shortcutId1)
    }

    @Test
    fun `selectedApp ignores private space header for home slots`() {
        repository.selectedApp(AppModel.PrivateSpaceHeader(isLocked = true), Constants.FLAG_SET_HOME_APP_1)

        assertEquals("", prefs.appName1)
        assertEquals("", prefs.appPackage1)
        assertEquals(false, repository.refreshHome.value)
    }

    @Test
    fun `selectedApp saves app to swipe left action`() {
        repository.selectedApp(testApp, Constants.FLAG_SET_SWIPE_LEFT_APP)

        assertEquals("Test App", prefs.appNameSwipeLeft)
        assertEquals("app.minlauncher.test.target", prefs.appPackageSwipeLeft)
        assertEquals("app.minlauncher.test.target.MainActivity", prefs.appActivityClassNameSwipeLeft)
        assertFalse(prefs.isShortcutSwipeLeft)
        assertEquals("", prefs.shortcutIdSwipeLeft)
        assertEquals(Unit, repository.updateSwipeApps.value)
    }

    @Test
    fun `selectedApp saves pinned shortcut to swipe right action`() {
        repository.selectedApp(testShortcut, Constants.FLAG_SET_SWIPE_RIGHT_APP)

        assertEquals("Test Shortcut", prefs.appNameSwipeRight)
        assertEquals("app.minlauncher.test.target", prefs.appPackageSwipeRight)
        assertEquals("", prefs.appActivityClassNameRight)
        assertTrue(prefs.isShortcutSwipeRight)
        assertEquals("shortcut-1", prefs.shortcutIdSwipeRight)
    }

    @Test
    fun `saving a pinned shortcut over an app clears the activity class name`() {
        repository.selectedApp(testApp, Constants.FLAG_SET_SWIPE_LEFT_APP)
        repository.selectedApp(testShortcut, Constants.FLAG_SET_SWIPE_LEFT_APP)

        assertEquals("Test Shortcut", prefs.appNameSwipeLeft)
        assertEquals("app.minlauncher.test.target", prefs.appPackageSwipeLeft)
        assertEquals(Process.myUserHandle().toString(), prefs.appUserSwipeLeft)
        assertEquals("", prefs.appActivityClassNameSwipeLeft)
        assertTrue(prefs.isShortcutSwipeLeft)
        assertEquals("shortcut-1", prefs.shortcutIdSwipeLeft)
    }

    @Test
    fun `saving a home app triggers a home refresh`() {
        repository.selectedApp(testApp, Constants.FLAG_SET_HOME_APP_2)

        assertEquals(false, repository.refreshHome.value)
    }
}
