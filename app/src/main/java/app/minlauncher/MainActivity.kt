package app.minlauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.minlauncher.data.Constants
import app.minlauncher.data.Prefs
import app.minlauncher.helper.getColorFromAttr
import app.minlauncher.helper.hasBeenHours
import app.minlauncher.helper.isDefaultLauncher
import app.minlauncher.helper.isEinkDisplay
import app.minlauncher.helper.isTablet
import app.minlauncher.helper.resetLauncherViaFakeActivity
import app.minlauncher.helper.showLauncherSelector
import app.minlauncher.ui.AppRoot
import app.minlauncher.ui.AppRootHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private val repository by lazy { (application as LauncherApp).launcherRepository }
    private var appRootHandle: AppRootHandle? = null
    private var timerJob: Job? = null
    private var isResumed = false
    private var profileReceiver: BroadcastReceiver? = null
    private var launcherAppsCallback: LauncherApps.Callback? = null

    override fun attachBaseContext(context: Context) {
        val newConfig = Configuration(context.resources.configuration)
        newConfig.fontScale = Prefs(context).textSizeScale
        super.attachBaseContext(context.createConfigurationContext(newConfig))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        if (isEinkDisplay()) prefs.appTheme = AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(prefs.appTheme)
        super.onCreate(savedInstanceState)
        if (prefs.boldFont) theme.applyStyle(R.style.BoldFontOverlay, true)
        // The theme is read once here: theme changes go through prefs +
        // Activity.recreate(), so AppRoot gets a fresh value per activity.
        setContent {
            AppRoot(
                repository = repository,
                appTheme = prefs.appTheme,
                onHandleReady = { appRootHandle = it },
            )
        }

        if (prefs.firstOpen) {
            prefs.firstOpen = false
            prefs.firstOpenTime = System.currentTimeMillis()
            repository.requestResetLauncher()
        }

        observeLauncherResets()
        repository.getAppList()
        registerShortcutCallback()
        setupOrientation()

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            profileReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?,
                    ) {
                        repository.isPrivateSpaceToggling = false
                        repository.getPrivateSpaceAppList()
                    }
                }
            val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_PROFILE_AVAILABLE)
                    addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
                }
            registerReceiver(profileReceiver, filter)
        }
    }

    override fun onStart() {
        super.onStart()
        restartLauncherOrCheckTheme()
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        repository.isPrivateSpaceToggling = false
        repository.getAppList()
    }

    private fun registerShortcutCallback() {
        val launcherApps = getSystemService(LauncherApps::class.java)
        launcherAppsCallback =
            object : LauncherApps.Callback() {
                override fun onPackageRemoved(
                    packageName: String,
                    user: android.os.UserHandle,
                ) = Unit

                override fun onPackageAdded(
                    packageName: String,
                    user: android.os.UserHandle,
                ) = Unit

                override fun onPackageChanged(
                    packageName: String,
                    user: android.os.UserHandle,
                ) = Unit

                override fun onPackagesAvailable(
                    packageNames: Array<out String>,
                    user: android.os.UserHandle,
                    replacing: Boolean,
                ) = Unit

                override fun onPackagesUnavailable(
                    packageNames: Array<out String>,
                    user: android.os.UserHandle,
                    replacing: Boolean,
                ) = Unit

                override fun onShortcutsChanged(
                    packageName: String,
                    shortcuts: MutableList<ShortcutInfo>,
                    user: android.os.UserHandle,
                ) {
                    repository.getAppList()
                }
            }
        launcherApps.registerCallback(launcherAppsCallback!!)
    }

    override fun onStop() {
        isResumed = false
        backToHomeScreen()
        super.onStop()
    }

    override fun onUserLeaveHint() {
        backToHomeScreen()
        super.onUserLeaveHint()
    }

    override fun onNewIntent(intent: Intent) {
        // Home button for recents feature disabled
        // val alreadyHome = navController.currentDestination?.id == R.id.mainFragment
        backToHomeScreen()
        // if (alreadyHome && isResumed && prefs.homeButtonShowRecents)
        //     viewModel.showRecentApps.call()
        super.onNewIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppCompatDelegate.setDefaultNightMode(prefs.appTheme)
        if (prefs.solidWallpaper &&
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ) {
            recreate()
        }
    }

    /**
     * Resets the launcher (fake home activity or the system selector) while the
     * activity is at least STARTED; the dialog signals now belong to [AppRoot].
     */
    private fun observeLauncherResets() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.resetLauncher.collect {
                    if (isDefaultLauncher()) {
                        resetLauncherViaFakeActivity()
                    } else {
                        showLauncherSelector(Constants.REQUEST_CODE_LAUNCHER_SELECTOR)
                    }
                }
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupOrientation() {
        if (isTablet(this)) return
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun backToHomeScreen() {
        if (repository.isPrivateSpaceToggling) return
        // Main thread, so mutating the Nav3 back stack (SnapshotStateList) is safe.
        appRootHandle?.collapseToHome()
    }

    private fun restartLauncherOrCheckTheme(forceRestart: Boolean = false) {
        if (forceRestart || prefs.launcherRestartTimestamp.hasBeenHours(4)) {
            prefs.launcherRestartTimestamp = System.currentTimeMillis()
            cacheDir.deleteRecursively()
            recreate()
        } else {
            checkTheme()
        }
    }

    private fun checkTheme() {
        timerJob?.cancel()
        timerJob =
            lifecycleScope.launch {
                delay(200.milliseconds)
                if ((
                        prefs.appTheme == AppCompatDelegate.MODE_NIGHT_YES &&
                            getColorFromAttr(R.attr.primaryColor) != getColor(R.color.white)
                    ) ||
                    (
                        prefs.appTheme == AppCompatDelegate.MODE_NIGHT_NO &&
                            getColorFromAttr(R.attr.primaryColor) != getColor(R.color.black)
                    )
                ) {
                    restartLauncherOrCheckTheme(true)
                }
            }
    }

    override fun onDestroy() {
        launcherAppsCallback?.let {
            getSystemService(LauncherApps::class.java).unregisterCallback(it)
        }
        profileReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CODE_LAUNCHER_SELECTOR && resultCode == Activity.RESULT_OK) {
            resetLauncherViaFakeActivity()
        }
    }
}
