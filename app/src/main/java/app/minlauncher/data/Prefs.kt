package app.minlauncher.data

import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.minlauncher.LauncherApp

/**
 * Settings facade over a snapshot cache of the Preferences DataStore (see
 * [AppSettingsStore]): synchronous reads come from a @Volatile snapshot
 * seeded by LauncherApp.onCreate warmUp and kept fresh by a background
 * collector; writes update the snapshot optimistically and persist
 * asynchronously via the store. Only [lockModeOn] still touches
 * SharedPreferences directly (see below).
 */
class Prefs(
    context: Context,
    val settings: AppSettingsStore? = null,
) {
    // Lazy so the :serviceProcess (which only writes lockModeOn via
    // SharedPreferences) never instantiates the DataStore: one file must
    // have at most one DataStore instance per process, and the service
    // process must not contend with the main process's store.
    private val store by lazy {
        settings ?: (context.applicationContext as LauncherApp).appSettings
    }

    // lockModeOn stays on SharedPreferences because MyAccessibilityService,
    // running in :serviceProcess, writes it cross-process and must never
    // touch the DataStore.
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    private companion object {
        private const val PREFS_FILENAME = "app.minlauncher"

        // Typed DataStore keys; the key NAME strings must match the legacy
        // SharedPreferences keys byte-for-byte so the one-time migration maps
        // them 1:1.
        private val FIRST_OPEN = booleanPreferencesKey("FIRST_OPEN")
        private val FIRST_OPEN_TIME = longPreferencesKey("FIRST_OPEN_TIME")
        private val FIRST_SETTINGS_OPEN = booleanPreferencesKey("FIRST_SETTINGS_OPEN")
        private val FIRST_HIDE = booleanPreferencesKey("FIRST_HIDE")
        private val AUTO_SHOW_KEYBOARD = booleanPreferencesKey("AUTO_SHOW_KEYBOARD")
        private val KEYBOARD_MESSAGE = booleanPreferencesKey("KEYBOARD_MESSAGE")
        private val SOLID_WALLPAPER = booleanPreferencesKey("SOLID_WALLPAPER")
        private val HOME_APPS_NUM = intPreferencesKey("HOME_APPS_NUM")
        private val HOME_ALIGNMENT = intPreferencesKey("HOME_ALIGNMENT")
        private val HOME_BOTTOM_ALIGNMENT = booleanPreferencesKey("HOME_BOTTOM_ALIGNMENT")
        private val APP_LABEL_ALIGNMENT = intPreferencesKey("APP_LABEL_ALIGNMENT")
        private val STATUS_BAR = booleanPreferencesKey("STATUS_BAR")
        private val DATE_TIME_VISIBILITY = intPreferencesKey("DATE_TIME_VISIBILITY")
        private val SWIPE_LEFT_ENABLED = booleanPreferencesKey("SWIPE_LEFT_ENABLED")
        private val SWIPE_RIGHT_ENABLED = booleanPreferencesKey("SWIPE_RIGHT_ENABLED")
        private val HIDDEN_APPS = stringSetPreferencesKey("HIDDEN_APPS")
        private val HIDDEN_APPS_UPDATED = booleanPreferencesKey("HIDDEN_APPS_UPDATED")
        private val APP_THEME = intPreferencesKey("APP_THEME")
        private val TEXT_SIZE_SCALE = floatPreferencesKey("TEXT_SIZE_SCALE")
        private val BOLD_FONT = booleanPreferencesKey("BOLD_FONT")
        private val HIDE_SET_DEFAULT_LAUNCHER = booleanPreferencesKey("HIDE_SET_DEFAULT_LAUNCHER")
        private val SCREEN_TIME_LAST_UPDATED = longPreferencesKey("SCREEN_TIME_LAST_UPDATED")
        private val SCREEN_TIME_ENABLED = booleanPreferencesKey("SCREEN_TIME_ENABLED")
        private val LAUNCHER_RESTART_TIMESTAMP = longPreferencesKey("LAUNCHER_RECREATE_TIMESTAMP")
        // Home button for recents feature disabled
        // private val HOME_BUTTON_SHOW_RECENTS = booleanPreferencesKey("HOME_BUTTON_SHOW_RECENTS")

        private val APP_NAME_1 = stringPreferencesKey("APP_NAME_1")
        private val APP_NAME_2 = stringPreferencesKey("APP_NAME_2")
        private val APP_NAME_3 = stringPreferencesKey("APP_NAME_3")
        private val APP_NAME_4 = stringPreferencesKey("APP_NAME_4")
        private val APP_NAME_5 = stringPreferencesKey("APP_NAME_5")
        private val APP_NAME_6 = stringPreferencesKey("APP_NAME_6")
        private val APP_NAME_7 = stringPreferencesKey("APP_NAME_7")
        private val APP_NAME_8 = stringPreferencesKey("APP_NAME_8")
        private val APP_PACKAGE_1 = stringPreferencesKey("APP_PACKAGE_1")
        private val APP_PACKAGE_2 = stringPreferencesKey("APP_PACKAGE_2")
        private val APP_PACKAGE_3 = stringPreferencesKey("APP_PACKAGE_3")
        private val APP_PACKAGE_4 = stringPreferencesKey("APP_PACKAGE_4")
        private val APP_PACKAGE_5 = stringPreferencesKey("APP_PACKAGE_5")
        private val APP_PACKAGE_6 = stringPreferencesKey("APP_PACKAGE_6")
        private val APP_PACKAGE_7 = stringPreferencesKey("APP_PACKAGE_7")
        private val APP_PACKAGE_8 = stringPreferencesKey("APP_PACKAGE_8")
        private val APP_ACTIVITY_CLASS_NAME_1 = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_1")
        private val APP_ACTIVITY_CLASS_NAME_2 = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_2")
        private val APP_ACTIVITY_CLASS_NAME_3 = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_3")
        private val APP_ACTIVITY_CLASS_NAME_4 = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_4")
        private val APP_ACTIVITY_CLASS_NAME_5 = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_5")
        private val APP_ACTIVITY_CLASS_NAME_6 = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_6")
        private val APP_ACTIVITY_CLASS_NAME_7 = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_7")
        private val APP_ACTIVITY_CLASS_NAME_8 = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_8")
        private val APP_USER_1 = stringPreferencesKey("APP_USER_1")
        private val APP_USER_2 = stringPreferencesKey("APP_USER_2")
        private val APP_USER_3 = stringPreferencesKey("APP_USER_3")
        private val APP_USER_4 = stringPreferencesKey("APP_USER_4")
        private val APP_USER_5 = stringPreferencesKey("APP_USER_5")
        private val APP_USER_6 = stringPreferencesKey("APP_USER_6")
        private val APP_USER_7 = stringPreferencesKey("APP_USER_7")
        private val APP_USER_8 = stringPreferencesKey("APP_USER_8")

        private val APP_NAME_SWIPE_LEFT = stringPreferencesKey("APP_NAME_SWIPE_LEFT")
        private val APP_NAME_SWIPE_RIGHT = stringPreferencesKey("APP_NAME_SWIPE_RIGHT")
        private val APP_PACKAGE_SWIPE_LEFT = stringPreferencesKey("APP_PACKAGE_SWIPE_LEFT")
        private val APP_PACKAGE_SWIPE_RIGHT = stringPreferencesKey("APP_PACKAGE_SWIPE_RIGHT")
        private val APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT")
        private val APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT")
        private val APP_USER_SWIPE_LEFT = stringPreferencesKey("APP_USER_SWIPE_LEFT")
        private val APP_USER_SWIPE_RIGHT = stringPreferencesKey("APP_USER_SWIPE_RIGHT")

        private val IS_SHORTCUT_1 = booleanPreferencesKey("IS_SHORTCUT_1")
        private val SHORTCUT_ID_1 = stringPreferencesKey("SHORTCUT_ID_1")
        private val IS_SHORTCUT_2 = booleanPreferencesKey("IS_SHORTCUT_2")
        private val SHORTCUT_ID_2 = stringPreferencesKey("SHORTCUT_ID_2")
        private val IS_SHORTCUT_3 = booleanPreferencesKey("IS_SHORTCUT_3")
        private val SHORTCUT_ID_3 = stringPreferencesKey("SHORTCUT_ID_3")
        private val IS_SHORTCUT_4 = booleanPreferencesKey("IS_SHORTCUT_4")
        private val SHORTCUT_ID_4 = stringPreferencesKey("SHORTCUT_ID_4")
        private val IS_SHORTCUT_5 = booleanPreferencesKey("IS_SHORTCUT_5")
        private val SHORTCUT_ID_5 = stringPreferencesKey("SHORTCUT_ID_5")
        private val IS_SHORTCUT_6 = booleanPreferencesKey("IS_SHORTCUT_6")
        private val SHORTCUT_ID_6 = stringPreferencesKey("SHORTCUT_ID_6")
        private val IS_SHORTCUT_7 = booleanPreferencesKey("IS_SHORTCUT_7")
        private val SHORTCUT_ID_7 = stringPreferencesKey("SHORTCUT_ID_7")
        private val IS_SHORTCUT_8 = booleanPreferencesKey("IS_SHORTCUT_8")
        private val SHORTCUT_ID_8 = stringPreferencesKey("SHORTCUT_ID_8")

        private val SHORTCUT_ID_SWIPE_LEFT = stringPreferencesKey("SHORTCUT_ID_SWIPE_LEFT")
        private val IS_SHORTCUT_SWIPE_LEFT = booleanPreferencesKey("IS_SHORTCUT_SWIPE_LEFT")
        private val SHORTCUT_ID_SWIPE_RIGHT = stringPreferencesKey("SHORTCUT_ID_SWIPE_RIGHT")
        private val IS_SHORTCUT_SWIPE_RIGHT = booleanPreferencesKey("IS_SHORTCUT_SWIPE_RIGHT")
    }

    var firstOpen: Boolean
        get() = store[FIRST_OPEN] ?: true
        set(value) = store.set(FIRST_OPEN, value)

    var firstOpenTime: Long
        get() = store[FIRST_OPEN_TIME] ?: 0L
        set(value) = store.set(FIRST_OPEN_TIME, value)

    var firstSettingsOpen: Boolean
        get() = store[FIRST_SETTINGS_OPEN] ?: true
        set(value) = store.set(FIRST_SETTINGS_OPEN, value)

    var firstHide: Boolean
        get() = store[FIRST_HIDE] ?: true
        set(value) = store.set(FIRST_HIDE, value)

    var lockModeOn: Boolean
        get() = sharedPrefs.getBoolean(LockModePreservingMigration.LOCK_MODE, false)
        set(value) = sharedPrefs.edit { putBoolean(LockModePreservingMigration.LOCK_MODE, value).apply() }

    var autoShowKeyboard: Boolean
        get() = store[AUTO_SHOW_KEYBOARD] ?: true
        set(value) = store.set(AUTO_SHOW_KEYBOARD, value)

    var keyboardMessageShown: Boolean
        get() = store[KEYBOARD_MESSAGE] ?: false
        set(value) = store.set(KEYBOARD_MESSAGE, value)

    /** When on, the app paints a solid wallpaper matching the current theme. */
    var solidWallpaper: Boolean
        get() = store[SOLID_WALLPAPER] ?: false
        set(value) = store.set(SOLID_WALLPAPER, value)

    var homeAppsNum: Int
        get() = store[HOME_APPS_NUM] ?: 4
        set(value) = store.set(HOME_APPS_NUM, value)

    var homeAlignment: Int
        get() = store[HOME_ALIGNMENT] ?: Gravity.START
        set(value) = store.set(HOME_ALIGNMENT, value)

    var homeBottomAlignment: Boolean
        get() = store[HOME_BOTTOM_ALIGNMENT] ?: false
        set(value) = store.set(HOME_BOTTOM_ALIGNMENT, value)

    var appLabelAlignment: Int
        get() = store[APP_LABEL_ALIGNMENT] ?: Gravity.START
        set(value) = store.set(APP_LABEL_ALIGNMENT, value)

    var showStatusBar: Boolean
        get() = store[STATUS_BAR] ?: false
        set(value) = store.set(STATUS_BAR, value)

    var dateTimeVisibility: Int
        get() = store[DATE_TIME_VISIBILITY] ?: Constants.DateTime.ON
        set(value) = store.set(DATE_TIME_VISIBILITY, value)

    var swipeLeftEnabled: Boolean
        get() = store[SWIPE_LEFT_ENABLED] ?: true
        set(value) = store.set(SWIPE_LEFT_ENABLED, value)

    var swipeRightEnabled: Boolean
        get() = store[SWIPE_RIGHT_ENABLED] ?: true
        set(value) = store.set(SWIPE_RIGHT_ENABLED, value)

    var appTheme: Int
        get() = store[APP_THEME] ?: AppCompatDelegate.MODE_NIGHT_YES
        set(value) = store.set(APP_THEME, value)

    var textSizeScale: Float
        get() = store[TEXT_SIZE_SCALE] ?: 1.0f
        set(value) = store.set(TEXT_SIZE_SCALE, value)

    var boldFont: Boolean
        get() = store[BOLD_FONT] ?: false
        set(value) = store.set(BOLD_FONT, value)

    var hideSetDefaultLauncher: Boolean
        get() = store[HIDE_SET_DEFAULT_LAUNCHER] ?: false
        set(value) = store.set(HIDE_SET_DEFAULT_LAUNCHER, value)

    var screenTimeLastUpdated: Long
        get() = store[SCREEN_TIME_LAST_UPDATED] ?: 0L
        set(value) = store.set(SCREEN_TIME_LAST_UPDATED, value)

    /** Whether the screen time text is shown on the home screen (needs usage access). */
    var screenTimeEnabled: Boolean
        get() = store[SCREEN_TIME_ENABLED] ?: true
        set(value) = store.set(SCREEN_TIME_ENABLED, value)

    var launcherRestartTimestamp: Long
        get() = store[LAUNCHER_RESTART_TIMESTAMP] ?: 0L
        set(value) = store.set(LAUNCHER_RESTART_TIMESTAMP, value)

    // Home button for recents feature disabled
    // var homeButtonShowRecents: Boolean
    //     get() = store[HOME_BUTTON_SHOW_RECENTS] ?: false
    //     set(value) = store.set(HOME_BUTTON_SHOW_RECENTS, value)

    var hiddenApps: MutableSet<String>
        get() = store[HIDDEN_APPS]?.toMutableSet() ?: mutableSetOf()
        set(value) = store.set(HIDDEN_APPS, value)

    var hiddenAppsUpdated: Boolean
        get() = store[HIDDEN_APPS_UPDATED] ?: false
        set(value) = store.set(HIDDEN_APPS_UPDATED, value)

    var appName1: String
        get() = store[APP_NAME_1] ?: ""
        set(value) = store.set(APP_NAME_1, value)

    var appName2: String
        get() = store[APP_NAME_2] ?: ""
        set(value) = store.set(APP_NAME_2, value)

    var appName3: String
        get() = store[APP_NAME_3] ?: ""
        set(value) = store.set(APP_NAME_3, value)

    var appName4: String
        get() = store[APP_NAME_4] ?: ""
        set(value) = store.set(APP_NAME_4, value)

    var appName5: String
        get() = store[APP_NAME_5] ?: ""
        set(value) = store.set(APP_NAME_5, value)

    var appName6: String
        get() = store[APP_NAME_6] ?: ""
        set(value) = store.set(APP_NAME_6, value)

    var appName7: String
        get() = store[APP_NAME_7] ?: ""
        set(value) = store.set(APP_NAME_7, value)

    var appName8: String
        get() = store[APP_NAME_8] ?: ""
        set(value) = store.set(APP_NAME_8, value)

    var appPackage1: String
        get() = store[APP_PACKAGE_1] ?: ""
        set(value) = store.set(APP_PACKAGE_1, value)

    var appPackage2: String
        get() = store[APP_PACKAGE_2] ?: ""
        set(value) = store.set(APP_PACKAGE_2, value)

    var appPackage3: String
        get() = store[APP_PACKAGE_3] ?: ""
        set(value) = store.set(APP_PACKAGE_3, value)

    var appPackage4: String
        get() = store[APP_PACKAGE_4] ?: ""
        set(value) = store.set(APP_PACKAGE_4, value)

    var appPackage5: String
        get() = store[APP_PACKAGE_5] ?: ""
        set(value) = store.set(APP_PACKAGE_5, value)

    var appPackage6: String
        get() = store[APP_PACKAGE_6] ?: ""
        set(value) = store.set(APP_PACKAGE_6, value)

    var appPackage7: String
        get() = store[APP_PACKAGE_7] ?: ""
        set(value) = store.set(APP_PACKAGE_7, value)

    var appPackage8: String
        get() = store[APP_PACKAGE_8] ?: ""
        set(value) = store.set(APP_PACKAGE_8, value)

    var appActivityClassName1: String?
        get() = store[APP_ACTIVITY_CLASS_NAME_1] ?: ""
        set(value) {
            if (value == null) store.remove(APP_ACTIVITY_CLASS_NAME_1) else store.set(APP_ACTIVITY_CLASS_NAME_1, value)
        }

    var appActivityClassName2: String?
        get() = store[APP_ACTIVITY_CLASS_NAME_2] ?: ""
        set(value) {
            if (value == null) store.remove(APP_ACTIVITY_CLASS_NAME_2) else store.set(APP_ACTIVITY_CLASS_NAME_2, value)
        }

    var appActivityClassName3: String?
        get() = store[APP_ACTIVITY_CLASS_NAME_3] ?: ""
        set(value) {
            if (value == null) store.remove(APP_ACTIVITY_CLASS_NAME_3) else store.set(APP_ACTIVITY_CLASS_NAME_3, value)
        }

    var appActivityClassName4: String?
        get() = store[APP_ACTIVITY_CLASS_NAME_4] ?: ""
        set(value) {
            if (value == null) store.remove(APP_ACTIVITY_CLASS_NAME_4) else store.set(APP_ACTIVITY_CLASS_NAME_4, value)
        }

    var appActivityClassName5: String?
        get() = store[APP_ACTIVITY_CLASS_NAME_5] ?: ""
        set(value) {
            if (value == null) store.remove(APP_ACTIVITY_CLASS_NAME_5) else store.set(APP_ACTIVITY_CLASS_NAME_5, value)
        }

    var appActivityClassName6: String?
        get() = store[APP_ACTIVITY_CLASS_NAME_6] ?: ""
        set(value) {
            if (value == null) store.remove(APP_ACTIVITY_CLASS_NAME_6) else store.set(APP_ACTIVITY_CLASS_NAME_6, value)
        }

    var appActivityClassName7: String?
        get() = store[APP_ACTIVITY_CLASS_NAME_7] ?: ""
        set(value) {
            if (value == null) store.remove(APP_ACTIVITY_CLASS_NAME_7) else store.set(APP_ACTIVITY_CLASS_NAME_7, value)
        }

    var appActivityClassName8: String?
        get() = store[APP_ACTIVITY_CLASS_NAME_8] ?: ""
        set(value) {
            if (value == null) store.remove(APP_ACTIVITY_CLASS_NAME_8) else store.set(APP_ACTIVITY_CLASS_NAME_8, value)
        }

    var appUser1: String
        get() = store[APP_USER_1] ?: ""
        set(value) = store.set(APP_USER_1, value)

    var appUser2: String
        get() = store[APP_USER_2] ?: ""
        set(value) = store.set(APP_USER_2, value)

    var appUser3: String
        get() = store[APP_USER_3] ?: ""
        set(value) = store.set(APP_USER_3, value)

    var appUser4: String
        get() = store[APP_USER_4] ?: ""
        set(value) = store.set(APP_USER_4, value)

    var appUser5: String
        get() = store[APP_USER_5] ?: ""
        set(value) = store.set(APP_USER_5, value)

    var appUser6: String
        get() = store[APP_USER_6] ?: ""
        set(value) = store.set(APP_USER_6, value)

    var appUser7: String
        get() = store[APP_USER_7] ?: ""
        set(value) = store.set(APP_USER_7, value)

    var appUser8: String
        get() = store[APP_USER_8] ?: ""
        set(value) = store.set(APP_USER_8, value)

    var appNameSwipeLeft: String
        get() = store[APP_NAME_SWIPE_LEFT] ?: ""
        set(value) = store.set(APP_NAME_SWIPE_LEFT, value)

    var appNameSwipeRight: String
        get() = store[APP_NAME_SWIPE_RIGHT] ?: ""
        set(value) = store.set(APP_NAME_SWIPE_RIGHT, value)

    var appPackageSwipeLeft: String
        get() = store[APP_PACKAGE_SWIPE_LEFT] ?: ""
        set(value) = store.set(APP_PACKAGE_SWIPE_LEFT, value)

    var appActivityClassNameSwipeLeft: String?
        get() = store[APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT] ?: ""
        set(value) {
            if (value == null) {
                store.remove(APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT)
            } else {
                store.set(APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT, value)
            }
        }

    var appPackageSwipeRight: String
        get() = store[APP_PACKAGE_SWIPE_RIGHT] ?: ""
        set(value) = store.set(APP_PACKAGE_SWIPE_RIGHT, value)

    var appActivityClassNameRight: String?
        get() = store[APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT] ?: ""
        set(value) {
            if (value == null) {
                store.remove(APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT)
            } else {
                store.set(APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT, value)
            }
        }

    var appUserSwipeLeft: String
        get() = store[APP_USER_SWIPE_LEFT] ?: ""
        set(value) = store.set(APP_USER_SWIPE_LEFT, value)

    var appUserSwipeRight: String
        get() = store[APP_USER_SWIPE_RIGHT] ?: ""
        set(value) = store.set(APP_USER_SWIPE_RIGHT, value)

    var isShortcut1: Boolean
        get() = store[IS_SHORTCUT_1] ?: false
        set(value) = store.set(IS_SHORTCUT_1, value)

    var shortcutId1: String
        get() = store[SHORTCUT_ID_1] ?: ""
        set(value) = store.set(SHORTCUT_ID_1, value)

    var isShortcut2: Boolean
        get() = store[IS_SHORTCUT_2] ?: false
        set(value) = store.set(IS_SHORTCUT_2, value)

    var shortcutId2: String
        get() = store[SHORTCUT_ID_2] ?: ""
        set(value) = store.set(SHORTCUT_ID_2, value)

    var isShortcut3: Boolean
        get() = store[IS_SHORTCUT_3] ?: false
        set(value) = store.set(IS_SHORTCUT_3, value)

    var shortcutId3: String
        get() = store[SHORTCUT_ID_3] ?: ""
        set(value) = store.set(SHORTCUT_ID_3, value)

    var isShortcut4: Boolean
        get() = store[IS_SHORTCUT_4] ?: false
        set(value) = store.set(IS_SHORTCUT_4, value)

    var shortcutId4: String
        get() = store[SHORTCUT_ID_4] ?: ""
        set(value) = store.set(SHORTCUT_ID_4, value)

    var isShortcut5: Boolean
        get() = store[IS_SHORTCUT_5] ?: false
        set(value) = store.set(IS_SHORTCUT_5, value)

    var shortcutId5: String
        get() = store[SHORTCUT_ID_5] ?: ""
        set(value) = store.set(SHORTCUT_ID_5, value)

    var isShortcut6: Boolean
        get() = store[IS_SHORTCUT_6] ?: false
        set(value) = store.set(IS_SHORTCUT_6, value)

    var shortcutId6: String
        get() = store[SHORTCUT_ID_6] ?: ""
        set(value) = store.set(SHORTCUT_ID_6, value)

    var isShortcut7: Boolean
        get() = store[IS_SHORTCUT_7] ?: false
        set(value) = store.set(IS_SHORTCUT_7, value)

    var shortcutId7: String
        get() = store[SHORTCUT_ID_7] ?: ""
        set(value) = store.set(SHORTCUT_ID_7, value)

    var isShortcut8: Boolean
        get() = store[IS_SHORTCUT_8] ?: false
        set(value) = store.set(IS_SHORTCUT_8, value)

    var shortcutId8: String
        get() = store[SHORTCUT_ID_8] ?: ""
        set(value) = store.set(SHORTCUT_ID_8, value)

    var shortcutIdSwipeLeft: String
        get() = store[SHORTCUT_ID_SWIPE_LEFT] ?: ""
        set(value) = store.set(SHORTCUT_ID_SWIPE_LEFT, value)

    var isShortcutSwipeLeft: Boolean
        get() = store[IS_SHORTCUT_SWIPE_LEFT] ?: false
        set(value) = store.set(IS_SHORTCUT_SWIPE_LEFT, value)

    var shortcutIdSwipeRight: String
        get() = store[SHORTCUT_ID_SWIPE_RIGHT] ?: ""
        set(value) = store.set(SHORTCUT_ID_SWIPE_RIGHT, value)

    var isShortcutSwipeRight: Boolean
        get() = store[IS_SHORTCUT_SWIPE_RIGHT] ?: false
        set(value) = store.set(IS_SHORTCUT_SWIPE_RIGHT, value)

    fun getAppName(location: Int): String =
        when (location) {
            1 -> appName1
            2 -> appName2
            3 -> appName3
            4 -> appName4
            5 -> appName5
            6 -> appName6
            7 -> appName7
            8 -> appName8
            else -> ""
        }

    fun getAppPackage(location: Int): String =
        when (location) {
            1 -> appPackage1
            2 -> appPackage2
            3 -> appPackage3
            4 -> appPackage4
            5 -> appPackage5
            6 -> appPackage6
            7 -> appPackage7
            8 -> appPackage8
            else -> ""
        }

    fun getAppActivityClassName(location: Int): String =
        when (location) {
            1 -> appActivityClassName1.toString()
            2 -> appActivityClassName2.toString()
            3 -> appActivityClassName3.toString()
            4 -> appActivityClassName4.toString()
            5 -> appActivityClassName5.toString()
            6 -> appActivityClassName6.toString()
            7 -> appActivityClassName7.toString()
            8 -> appActivityClassName8.toString()
            else -> ""
        }

    fun getAppUser(location: Int): String =
        when (location) {
            1 -> appUser1
            2 -> appUser2
            3 -> appUser3
            4 -> appUser4
            5 -> appUser5
            6 -> appUser6
            7 -> appUser7
            8 -> appUser8
            else -> ""
        }

    fun getShortcutId(location: Int): String =
        when (location) {
            1 -> shortcutId1
            2 -> shortcutId2
            3 -> shortcutId3
            4 -> shortcutId4
            5 -> shortcutId5
            6 -> shortcutId6
            7 -> shortcutId7
            8 -> shortcutId8
            else -> ""
        }

    fun getIsShortcut(location: Int): Boolean =
        when (location) {
            1 -> isShortcut1
            2 -> isShortcut2
            3 -> isShortcut3
            4 -> isShortcut4
            5 -> isShortcut5
            6 -> isShortcut6
            7 -> isShortcut7
            8 -> isShortcut8
            else -> false
        }

    fun setAppActivityClassName(
        location: Int,
        activityClassName: String,
    ) {
        when (location) {
            1 -> appActivityClassName1 = activityClassName
            2 -> appActivityClassName2 = activityClassName
            3 -> appActivityClassName3 = activityClassName
            4 -> appActivityClassName4 = activityClassName
            5 -> appActivityClassName5 = activityClassName
            6 -> appActivityClassName6 = activityClassName
            7 -> appActivityClassName7 = activityClassName
            8 -> appActivityClassName8 = activityClassName
        }
    }

    fun updateAppActivityClassName(
        packageName: String,
        activityClassName: String,
    ) {
        for (i in 1..8) {
            if (getAppPackage(i) == packageName) setAppActivityClassName(i, activityClassName)
        }
        if (appPackageSwipeLeft == packageName) appActivityClassNameSwipeLeft = activityClassName
        if (appPackageSwipeRight == packageName) appActivityClassNameRight = activityClassName
    }

    fun getAppRenameLabel(appPackage: String): String = store[stringPreferencesKey(appPackage)] ?: ""

    fun setAppRenameLabel(
        appPackage: String,
        renameLabel: String,
    ) = store.set(stringPreferencesKey(appPackage), renameLabel)
}
