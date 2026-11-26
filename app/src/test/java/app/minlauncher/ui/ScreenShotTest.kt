package app.minlauncher.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import app.minlauncher.data.Prefs
import app.minlauncher.testing.TestMainActivity
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
// Pixel4: the default Robolectric screen (320x470dp) is too short for eight app slots.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel4)
@RunWith(RobolectricTestRunner::class)
class ScreenShotTest {
    // The settings screen is Compose; this settles its composition before the
    // decorView is captured. Other screens have no Compose roots yet.
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val homeApps = fakeApps("Browser", "Camera", "Mail", "Chat", "Maps", "Music", "Notes", "Files")

    // Both the clock and the date line render from HomeFragment's injected
    // clock, so pin it (and the timezone it formats in), or baselines differ
    // between machines and recording days.
    private val fixedInstant = Instant.parse("2026-10-06T00:00:00Z") // Tue, Oct 6 in Asia/Tokyo

    private lateinit var originalTimeZone: TimeZone
    private lateinit var originalLocale: Locale

    @Before
    fun pinRenderingEnvironment() {
        originalTimeZone = TimeZone.getDefault()
        originalLocale = Locale.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        Locale.setDefault(Locale.US)
        HomeFragment.clock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Tokyo"))
    }

    @After
    fun restoreRenderingEnvironment() {
        TimeZone.setDefault(originalTimeZone)
        Locale.setDefault(originalLocale)
        HomeFragment.clock = Clock.systemDefaultZone()
        TestMainActivity.testAppListProvider = null
    }

    @Test
    fun `home screen dark`() {
        launchHome().use { it.capture("roborazzi/home_dark.png") }
    }

    @Test
    fun `home screen light`() {
        launchHome { appTheme = AppCompatDelegate.MODE_NIGHT_NO }.use {
            it.capture("roborazzi/home_light.png")
        }
    }

    @Test
    fun `app drawer`() {
        launchHome().use { scenario ->
            scenario.openDrawer()
            composeRule.waitForIdle()

            scenario.capture("roborazzi/app_drawer.png")
        }
    }

    @Test
    fun `app drawer narrowed by a search query`() {
        launchHome().use { scenario ->
            scenario.openDrawer()

            composeRule.onNodeWithTag("drawer.search").performTextInput("ma")
            composeRule.waitForIdle()

            scenario.capture("roborazzi/app_drawer_search.png")
        }
    }

    @Test
    fun `settings screen`() {
        launchHome().use { scenario ->
            openSettings()
            composeRule.waitForIdle()

            scenario.capture("roborazzi/settings.png")
        }
    }

    /** The settled home screen: eight named app slots, no tips. */
    private fun launchHome(seed: Prefs.() -> Unit = {}): ActivityScenario<TestMainActivity> =
        launchLauncher(homeApps) {
            firstOpen = false
            firstSettingsOpen = false
            solidWallpaper = true
            seedHomeSlots()
            seed()
        }

    private fun Prefs.seedHomeSlots() {
        val nameSlots =
            listOf(
                this::appName1,
                this::appName2,
                this::appName3,
                this::appName4,
                this::appName5,
                this::appName6,
                this::appName7,
                this::appName8,
            )
        val packageSlots =
            listOf(
                this::appPackage1,
                this::appPackage2,
                this::appPackage3,
                this::appPackage4,
                this::appPackage5,
                this::appPackage6,
                this::appPackage7,
                this::appPackage8,
            )
        homeAppsNum = homeApps.size
        homeApps.forEachIndexed { index, app ->
            nameSlots[index].set(app.appLabel)
            packageSlots[index].set(app.appPackage)
        }
    }

    private fun ActivityScenario<TestMainActivity>.capture(name: String) {
        onActivity { activity ->
            activity.window.decorView.captureRoboImage(name)
        }
    }
}
