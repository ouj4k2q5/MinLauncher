package app.minlauncher.ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.SystemClock
import android.os.UserHandle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import androidx.core.os.bundleOf
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.helper.FakeAppListProvider
import app.minlauncher.testing.TestMainActivity
import org.hamcrest.Matcher
import org.robolectric.Shadows.shadowOf

private const val TEST_PACKAGE_PREFIX = "app.minlauncher.ui.fixture"

/** A regular app in the fake five-apps world. */
fun fakeApp(label: String): AppModel.App =
    AppModel.App(
        appLabel = label,
        key = null,
        appPackage = "$TEST_PACKAGE_PREFIX.${label.lowercase()}",
        activityClassName = "$TEST_PACKAGE_PREFIX.${label.lowercase()}.MainActivity",
        user = Process.myUserHandle(),
    )

fun fakeApps(vararg labels: String): List<AppModel.App> = labels.map { fakeApp(it) }

/**
 * Launches [TestMainActivity] with the given apps as the whole world:
 * the fake provider feeds the ViewModel, and LauncherApps is seeded so that
 * `isPackageInstalled` passes for the fake packages (HomeFragment clears home
 * slots whose package does not resolve).
 *
 * The returned scenario must be closed by the caller, e.g. with `use { }`.
 */
fun launchLauncher(
    apps: List<AppModel.App> = emptyList(),
    hidden: List<AppModel.App> = emptyList(),
    seed: Prefs.() -> Unit = {},
): ActivityScenario<TestMainActivity> {
    TestMainActivity.testAppListProvider =
        FakeAppListProvider(regularApps = apps, hiddenApps = hidden)
    val context = ApplicationProvider.getApplicationContext<Context>()
    seedLauncherApps(context, apps)
    disableSystemAnimations(context)
    Prefs(context).apply(seed)
    return ActivityScenario.launch(TestMainActivity::class.java)
}

/** Navigates from the home screen to the app drawer, as the real gestures do. */
fun ActivityScenario<TestMainActivity>.openDrawer(
    flag: Int = Constants.FLAG_LAUNCH_APP,
    canRename: Boolean = false,
) {
    onActivity { activity ->
        val navHost =
            activity.supportFragmentManager.findFragmentById(android.R.id.content)
                as androidx.navigation.fragment.NavHostFragment
        navHost.navController.navigate(
            app.minlauncher.R.id.appListFragment,
            bundleOf(
                Constants.Key.FLAG to flag,
                Constants.Key.RENAME to canRename,
            ),
        )
    }
}

/** The app under test, for asserting persisted state from within a test. */
fun testPrefs(): Prefs = Prefs(ApplicationProvider.getApplicationContext())

/**
 * Performs a long press that stays down long enough for
 * [app.minlauncher.listener.ViewSwipeTouchListener], which fires its
 * onLongClick only after an additional LONG_PRESS_DELAY_MS posted on top of
 * the system long-press timeout. Espresso's built-in longClick() holds only
 * the system timeout, so the listener's delayed task would be cancelled by
 * the ACTION_UP before it runs.
 */
fun holdLongPress(durationMs: Long = Constants.LONG_PRESS_DELAY_MS + 500L): ViewAction =
    object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()

        override fun getDescription(): String = "hold a long press for $durationMs ms"

        override fun perform(
            uiController: UiController,
            view: View,
        ) {
            val coords = GeneralLocation.CENTER.calculateCoordinates(view)
            val downTime = SystemClock.uptimeMillis()
            uiController.injectMotionEvent(
                MotionEvent.obtain(
                    downTime,
                    downTime,
                    MotionEvent.ACTION_DOWN,
                    coords[0],
                    coords[1],
                    1f,
                    1f,
                    0,
                    1f,
                    1f,
                    0,
                    0,
                ),
            )
            uiController.loopMainThreadForAtLeast(durationMs)
            uiController.injectMotionEvent(
                MotionEvent.obtain(
                    downTime,
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP,
                    coords[0],
                    coords[1],
                    1f,
                    1f,
                    0,
                    1f,
                    1f,
                    0,
                    0,
                ),
            )
        }
    }

private fun seedLauncherApps(
    context: Context,
    apps: List<AppModel.App>,
) {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val shadow = shadowOf(launcherApps)
    apps.forEach { app -> shadow.addActivity(app.user, newLauncherActivityInfo(context, app)) }
}

/**
 * Builds a real [android.content.pm.LauncherActivityInfo] for a fake package.
 * Both constructors needed here are hidden API, hence the reflection.
 */
private fun newLauncherActivityInfo(
    context: Context,
    app: AppModel.App,
): android.content.pm.LauncherActivityInfo {
    val activityInfo =
        ActivityInfo().apply {
            packageName = app.appPackage
            name = app.activityClassName
        }
    val internalClass = Class.forName("android.content.pm.LauncherActivityInfoInternal")
    val internalCtor =
        internalClass
            .getDeclaredConstructor(
                ActivityInfo::class.java,
                Class.forName("android.content.pm.IncrementalStatesInfo"),
                UserHandle::class.java,
            ).apply { isAccessible = true }
    val internal = internalCtor.newInstance(activityInfo, null, app.user)
    return android.content.pm.LauncherActivityInfo::class.java
        .getDeclaredConstructor(Context::class.java, internalClass)
        .apply { isAccessible = true }
        .newInstance(context, internal)
}

/**
 * Zero animation scales keep RecyclerView's layout animation (and fragment
 * transaction animations) out of the way under Robolectric, matching how the
 * app itself behaves on animation-disabled devices.
 */
private fun disableSystemAnimations(context: Context) {
    Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    Settings.Global.putFloat(context.contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
    Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
}
