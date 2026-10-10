package app.minlauncher.ui

import android.graphics.drawable.ColorDrawable
import androidx.appcompat.R.id.search_src_text
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import app.minlauncher.R
import app.minlauncher.data.Prefs
import app.minlauncher.helper.getColorFromAttr
import app.minlauncher.testing.TestMainActivity
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
// Pixel4: the default Robolectric screen (320x470dp) is too short for eight app slots.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel4)
@RunWith(RobolectricTestRunner::class)
class ScreenShotTest {
    private val homeApps = fakeApps("Browser", "Camera", "Mail", "Chat", "Maps", "Music", "Notes", "Files")

    // The TextClock renders Robolectric's frozen SystemClock in the JVM default
    // timezone, and the date line follows the wall clock. Pin both, or baselines
    // differ between machines and recording days.
    private val fixedInstant = Instant.parse("2026-10-06T00:00:00Z") // Tue, Oct 6 in Asia/Tokyo

    private lateinit var originalTimeZone: TimeZone
    private lateinit var originalLocale: Locale

    @Before
    fun pinRenderingEnvironment() {
        originalTimeZone = TimeZone.getDefault()
        originalLocale = Locale.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        Locale.setDefault(Locale.US)
        HomeFragment.dateProvider = { Date(fixedInstant.toEpochMilli()) }
    }

    @After
    fun restoreRenderingEnvironment() {
        TimeZone.setDefault(originalTimeZone)
        Locale.setDefault(originalLocale)
        HomeFragment.dateProvider = { Date() }
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
            scenario.capture("roborazzi/app_drawer.png")
        }
    }

    @Test
    fun `app drawer narrowed by a search query`() {
        launchHome().use { scenario ->
            scenario.openDrawer()

            onView(withId(search_src_text)).perform(typeText("ma"))

            scenario.capture("roborazzi/app_drawer_search.png")
        }
    }

    @Test
    fun `settings screen`() {
        launchHome().use { scenario ->
            // Long press on the left edge of the home background opens settings.
            onView(withId(R.id.mainLayout)).perform(
                holdLongPress(
                    at = { view ->
                        floatArrayOf(view.width * 0.02f, view.height * 0.5f)
                    },
                ),
            )

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
            // The launcher window is transparent so the system wallpaper shows through,
            // but Robolectric has no wallpaper layer. Paint the solid backdrop the app
            // itself uses for this theme (see Utils.setPlainWallpaperByTheme) so the
            // capture matches a real device with a solid wallpaper enabled.
            activity.window.setBackgroundDrawable(
                ColorDrawable(activity.getColorFromAttr(R.attr.primaryInverseColor)),
            )
            activity.window.decorView.captureRoboImage(name)
        }
    }
}
