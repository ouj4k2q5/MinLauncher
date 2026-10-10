package app.minlauncher

import android.app.Application
import android.os.Process
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import app.minlauncher.data.AppModel
import app.minlauncher.data.LauncherRepository
import app.minlauncher.data.Prefs
import app.minlauncher.helper.FakeAppListProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class MainViewModelAppListTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var viewModel: MainViewModel

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

    @Before
    fun setUp() {
        viewModel =
            MainViewModel(
                application,
                LauncherRepository(
                    application,
                    FakeAppListProvider(regularApps = listOf(appA, appB, appC)),
                ),
            )
    }

    /** Hides the given apps in the prefs, as the fake provider's world reads them. */
    private fun markHidden(vararg apps: AppModel.App) {
        Prefs(application).hiddenApps = apps.mapTo(mutableSetOf()) { "${it.appPackage}|${it.user}" }
    }

    @Test
    fun `getAppList exposes the provider's regular apps`() {
        viewModel.getAppList()

        assertEquals(listOf(appA, appB, appC), viewModel.appList.value)
    }

    @Test
    fun `getHiddenApps exposes the provider's hidden apps`() {
        markHidden(appA)
        viewModel =
            MainViewModel(application, LauncherRepository(application, FakeAppListProvider(hiddenApps = listOf(appA))))

        viewModel.getHiddenApps()

        assertEquals(listOf(appA), viewModel.hiddenApps.value)
    }

    @Test
    fun `getAppList with includeHiddenApps combines regular and hidden apps`() {
        markHidden(appA, appC)
        viewModel =
            MainViewModel(
                application,
                LauncherRepository(
                    application,
                    FakeAppListProvider(regularApps = listOf(appB), hiddenApps = listOf(appA, appC)),
                ),
            )

        viewModel.getAppList(includeHiddenApps = true)

        assertEquals(listOf(appB, appA, appC), viewModel.appList.value)
    }

    @Test
    fun `getAppList without includeHiddenApps excludes hidden apps`() {
        markHidden(appA, appC)
        viewModel =
            MainViewModel(
                application,
                LauncherRepository(
                    application,
                    FakeAppListProvider(regularApps = listOf(appB), hiddenApps = listOf(appA, appC)),
                ),
            )

        viewModel.getAppList()

        assertEquals(listOf(appB), viewModel.appList.value)
    }
}
