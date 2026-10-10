package app.minlauncher.data

import android.app.Application
import android.os.Process
import android.view.Gravity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import app.minlauncher.helper.FakeAppListProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class LauncherRepositoryTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()
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
        Prefs(application).hiddenApps = apps.mapTo(mutableSetOf()) { "${it.appPackage}|${it.user}" }
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
    fun `getPrivateSpaceAppList reports no private space on Robolectric`() {
        repository.getPrivateSpaceAppList()

        assertEquals(false, repository.privateSpaceAvailable.value)
        assertEquals(true, repository.privateSpaceLocked.value)
        assertEquals(emptyList<AppModel>(), repository.privateSpaceApps.value)
    }

    @Test
    fun `updateHomeAlignment persists and exposes the new gravity`() {
        repository.updateHomeAlignment(Gravity.CENTER)

        assertEquals(Gravity.CENTER, Prefs(application).homeAlignment)
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
}
