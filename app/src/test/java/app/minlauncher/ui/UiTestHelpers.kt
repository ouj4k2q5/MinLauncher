package app.minlauncher.ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.helper.FakeAppListProvider
import app.minlauncher.testing.TestMainActivity
import app.minlauncher.ui.nav.DrawerKey
import app.minlauncher.ui.nav.SettingsKey
import org.robolectric.Shadows.shadowOf

private const val TEST_PACKAGE_PREFIX = "app.minlauncher.ui.fixture"

/** A regular app in the fake five-apps world. */
fun fakeApp(
    label: String,
    appPackage: String = "$TEST_PACKAGE_PREFIX.${label.lowercase()}",
    activityClassName: String = "$appPackage.MainActivity",
    user: UserHandle = Process.myUserHandle(),
): AppModel.App =
    AppModel.App(
        appLabel = label,
        key = null,
        appPackage = appPackage,
        activityClassName = activityClassName,
        user = user,
    )

fun fakeApps(vararg labels: String): List<AppModel.App> = labels.map { fakeApp(it) }

/** A user handle for a secondary profile (user id 10), for other-profile rows. */
fun otherProfileUser(): UserHandle = UserHandle.getUserHandleForUid(1_010_000)

/**
 * Launches [TestMainActivity] with the given apps as the whole world:
 * the fake provider feeds the ViewModel, and LauncherApps is seeded so that
 * `isPackageInstalled` passes for the fake packages (the home entry clears home
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
        activity.handle!!.backStack.add(DrawerKey(flag, canRename))
    }
}

/**
 * Opens Settings by pushing it onto the Nav3 back stack. The signature stays
 * scenario-free (twelve call sites) so the resumed test activity is found
 * through the instrumentation's lifecycle monitor.
 */
fun openSettings() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val activity =
            ActivityLifecycleMonitorRegistry
                .getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<TestMainActivity>()
                .single()
        activity.handle!!.backStack.add(SettingsKey)
    }
}

/** The app under test, for asserting persisted state from within a test. */
fun testPrefs(): Prefs = Prefs(ApplicationProvider.getApplicationContext())

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
    val incrementalStatesInfo = Class.forName("android.content.pm.IncrementalStatesInfo")
    // Android 16 (SDK 36) appended a `supportsMultiInstance` boolean to this
    // hidden constructor; older SDKs stop at UserHandle.
    val internalCtor =
        runCatching {
            internalClass.getDeclaredConstructor(
                ActivityInfo::class.java,
                incrementalStatesInfo,
                UserHandle::class.java,
                java.lang.Boolean.TYPE,
            )
        }.getOrElse {
            internalClass.getDeclaredConstructor(
                ActivityInfo::class.java,
                incrementalStatesInfo,
                UserHandle::class.java,
            )
        }.apply { isAccessible = true }
    val internalArgs =
        buildList {
            add(activityInfo)
            add(null)
            add(app.user)
            repeat(internalCtor.parameterCount - 3) { add(false) }
        }
    val internal = internalCtor.newInstance(*internalArgs.toTypedArray())
    return android.content.pm.LauncherActivityInfo::class.java
        .getDeclaredConstructor(Context::class.java, internalClass)
        .apply { isAccessible = true }
        .newInstance(context, internal)
}

/**
 * Zero animation scales keep RecyclerView's layout animation (and Navigation 3
 * transitions) out of the way under Robolectric, matching how the
 * app itself behaves on animation-disabled devices.
 */
private fun disableSystemAnimations(context: Context) {
    Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    Settings.Global.putFloat(context.contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
    Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
}
