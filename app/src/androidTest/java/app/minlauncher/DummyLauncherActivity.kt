package app.minlauncher

import android.app.Activity

/**
 * A launchable placeholder inside the androidTest APK. The aosp-atd system
 * image used by the Gradle Managed Device smoke tests strips every user app
 * (Launcher3, Settings, Dialer, ...), so the app drawer — which never lists
 * the launcher itself — has zero entries there and the drawer smoke test
 * times out. Registering this activity's MAIN/LAUNCHER intent filter gives
 * the drawer one deterministic entry on any image. Never meant to be opened.
 */
class DummyLauncherActivity : Activity()
