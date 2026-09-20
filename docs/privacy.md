# Privacy and permissions

MinLauncher collects no analytics, sends no telemetry, and does not declare the
`INTERNET` permission. It therefore cannot use network access. Screen-time data is
calculated on the device and never leaves it.

| Permission | Purpose |
|---|---|
| `QUERY_ALL_PACKAGES` | Lists installed apps for the launcher drawer |
| `SET_WALLPAPER` | Applies the local solid-colour wallpaper |
| `PACKAGE_USAGE_STATS` | Optionally shows today's screen time |
| `REQUEST_DELETE_PACKAGES` | Starts app uninstallation from the drawer |
| `ACCESS_HIDDEN_PROFILES` | Supports Private Space on Android 15 and later |
| `com.android.alarm.permission.SET_ALARM` | Opens the clock app when the clock is tapped |

The optional accessibility service is used only to lock the screen after the
double-tap gesture. It does not collect or transmit data.

Android's automatic backup is disabled: `dataExtractionRules` and
`fullBackupContent` exclude all app data from both cloud backup and
device-to-device transfer, so settings are not carried over when switching
devices.
