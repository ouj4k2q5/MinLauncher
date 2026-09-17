package app.minlauncher.data

import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

class Prefs(
    context: Context,
) {
    private companion object {
        private const val PREFS_FILENAME = "app.minlauncher"

        private const val FIRST_OPEN = "FIRST_OPEN"
        private const val FIRST_OPEN_TIME = "FIRST_OPEN_TIME"
        private const val FIRST_SETTINGS_OPEN = "FIRST_SETTINGS_OPEN"
        private const val FIRST_HIDE = "FIRST_HIDE"
        private const val LOCK_MODE = "LOCK_MODE"
        private const val HOME_APPS_NUM = "HOME_APPS_NUM"
        private const val AUTO_SHOW_KEYBOARD = "AUTO_SHOW_KEYBOARD"
        private const val KEYBOARD_MESSAGE = "KEYBOARD_MESSAGE"
        private const val SOLID_WALLPAPER = "SOLID_WALLPAPER"
        private const val HOME_ALIGNMENT = "HOME_ALIGNMENT"
        private const val HOME_BOTTOM_ALIGNMENT = "HOME_BOTTOM_ALIGNMENT"
        private const val APP_LABEL_ALIGNMENT = "APP_LABEL_ALIGNMENT"
        private const val STATUS_BAR = "STATUS_BAR"
        private const val DATE_TIME_VISIBILITY = "DATE_TIME_VISIBILITY"
        private const val SWIPE_LEFT_ENABLED = "SWIPE_LEFT_ENABLED"
        private const val SWIPE_RIGHT_ENABLED = "SWIPE_RIGHT_ENABLED"
        private const val HIDDEN_APPS = "HIDDEN_APPS"
        private const val HIDDEN_APPS_UPDATED = "HIDDEN_APPS_UPDATED"
        private const val APP_THEME = "APP_THEME"
        private const val SWIPE_DOWN_ACTION = "SWIPE_DOWN_ACTION"
        private const val TEXT_SIZE_SCALE = "TEXT_SIZE_SCALE"
        private const val BOLD_FONT = "BOLD_FONT"
        private const val HIDE_SET_DEFAULT_LAUNCHER = "HIDE_SET_DEFAULT_LAUNCHER"
        private const val SCREEN_TIME_LAST_UPDATED = "SCREEN_TIME_LAST_UPDATED"
        private const val LAUNCHER_RESTART_TIMESTAMP = "LAUNCHER_RECREATE_TIMESTAMP"
        // Home button for recents feature disabled
        // private val HOME_BUTTON_SHOW_RECENTS = "HOME_BUTTON_SHOW_RECENTS"

        private const val APP_NAME_1 = "APP_NAME_1"
        private const val APP_NAME_2 = "APP_NAME_2"
        private const val APP_NAME_3 = "APP_NAME_3"
        private const val APP_NAME_4 = "APP_NAME_4"
        private const val APP_NAME_5 = "APP_NAME_5"
        private const val APP_NAME_6 = "APP_NAME_6"
        private const val APP_NAME_7 = "APP_NAME_7"
        private const val APP_NAME_8 = "APP_NAME_8"
        private const val APP_PACKAGE_1 = "APP_PACKAGE_1"
        private const val APP_PACKAGE_2 = "APP_PACKAGE_2"
        private const val APP_PACKAGE_3 = "APP_PACKAGE_3"
        private const val APP_PACKAGE_4 = "APP_PACKAGE_4"
        private const val APP_PACKAGE_5 = "APP_PACKAGE_5"
        private const val APP_PACKAGE_6 = "APP_PACKAGE_6"
        private const val APP_PACKAGE_7 = "APP_PACKAGE_7"
        private const val APP_PACKAGE_8 = "APP_PACKAGE_8"
        private const val APP_ACTIVITY_CLASS_NAME_1 = "APP_ACTIVITY_CLASS_NAME_1"
        private const val APP_ACTIVITY_CLASS_NAME_2 = "APP_ACTIVITY_CLASS_NAME_2"
        private const val APP_ACTIVITY_CLASS_NAME_3 = "APP_ACTIVITY_CLASS_NAME_3"
        private const val APP_ACTIVITY_CLASS_NAME_4 = "APP_ACTIVITY_CLASS_NAME_4"
        private const val APP_ACTIVITY_CLASS_NAME_5 = "APP_ACTIVITY_CLASS_NAME_5"
        private const val APP_ACTIVITY_CLASS_NAME_6 = "APP_ACTIVITY_CLASS_NAME_6"
        private const val APP_ACTIVITY_CLASS_NAME_7 = "APP_ACTIVITY_CLASS_NAME_7"
        private const val APP_ACTIVITY_CLASS_NAME_8 = "APP_ACTIVITY_CLASS_NAME_8"
        private const val APP_USER_1 = "APP_USER_1"
        private const val APP_USER_2 = "APP_USER_2"
        private const val APP_USER_3 = "APP_USER_3"
        private const val APP_USER_4 = "APP_USER_4"
        private const val APP_USER_5 = "APP_USER_5"
        private const val APP_USER_6 = "APP_USER_6"
        private const val APP_USER_7 = "APP_USER_7"
        private const val APP_USER_8 = "APP_USER_8"

        private const val APP_NAME_SWIPE_LEFT = "APP_NAME_SWIPE_LEFT"
        private const val APP_NAME_SWIPE_RIGHT = "APP_NAME_SWIPE_RIGHT"
        private const val APP_PACKAGE_SWIPE_LEFT = "APP_PACKAGE_SWIPE_LEFT"
        private const val APP_PACKAGE_SWIPE_RIGHT = "APP_PACKAGE_SWIPE_RIGHT"
        private const val APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT = "APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT"
        private const val APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT = "APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT"
        private const val APP_USER_SWIPE_LEFT = "APP_USER_SWIPE_LEFT"
        private const val APP_USER_SWIPE_RIGHT = "APP_USER_SWIPE_RIGHT"
        private const val CLOCK_APP_PACKAGE = "CLOCK_APP_PACKAGE"
        private const val CLOCK_APP_USER = "CLOCK_APP_USER"
        private const val CLOCK_APP_CLASS_NAME = "CLOCK_APP_CLASS_NAME"
        private const val CALENDAR_APP_PACKAGE = "CALENDAR_APP_PACKAGE"
        private const val CALENDAR_APP_USER = "CALENDAR_APP_USER"
        private const val CALENDAR_APP_CLASS_NAME = "CALENDAR_APP_CLASS_NAME"
        private const val SCREEN_TIME_APP_PACKAGE = "SCREEN_TIME_APP_PACKAGE"
        private const val SCREEN_TIME_APP_USER = "SCREEN_TIME_APP_USER"
        private const val SCREEN_TIME_APP_CLASS_NAME = "SCREEN_TIME_APP_CLASS_NAME"

        private const val IS_SHORTCUT_1 = "IS_SHORTCUT_1"
        private const val SHORTCUT_ID_1 = "SHORTCUT_ID_1"
        private const val IS_SHORTCUT_2 = "IS_SHORTCUT_2"
        private const val SHORTCUT_ID_2 = "SHORTCUT_ID_2"
        private const val IS_SHORTCUT_3 = "IS_SHORTCUT_3"
        private const val SHORTCUT_ID_3 = "SHORTCUT_ID_3"
        private const val IS_SHORTCUT_4 = "IS_SHORTCUT_4"
        private const val SHORTCUT_ID_4 = "SHORTCUT_ID_4"
        private const val IS_SHORTCUT_5 = "IS_SHORTCUT_5"
        private const val SHORTCUT_ID_5 = "SHORTCUT_ID_5"
        private const val IS_SHORTCUT_6 = "IS_SHORTCUT_6"
        private const val SHORTCUT_ID_6 = "SHORTCUT_ID_6"
        private const val IS_SHORTCUT_7 = "IS_SHORTCUT_7"
        private const val SHORTCUT_ID_7 = "SHORTCUT_ID_7"
        private const val IS_SHORTCUT_8 = "IS_SHORTCUT_8"
        private const val SHORTCUT_ID_8 = "SHORTCUT_ID_8"

        private const val SHORTCUT_ID_SWIPE_LEFT = "SHORTCUT_ID_SWIPE_LEFT"
        private const val IS_SHORTCUT_SWIPE_LEFT = "IS_SHORTCUT_SWIPE_LEFT"
        private const val SHORTCUT_ID_SWIPE_RIGHT = "SHORTCUT_ID_SWIPE_RIGHT"
        private const val IS_SHORTCUT_SWIPE_RIGHT = "IS_SHORTCUT_SWIPE_RIGHT"
    }

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    var firstOpen: Boolean
        get() = sharedPrefs.getBoolean(FIRST_OPEN, true)
        set(value) = sharedPrefs.edit { putBoolean(FIRST_OPEN, value).apply() }

    var firstOpenTime: Long
        get() = sharedPrefs.getLong(FIRST_OPEN_TIME, 0L)
        set(value) = sharedPrefs.edit { putLong(FIRST_OPEN_TIME, value).apply() }

    var firstSettingsOpen: Boolean
        get() = sharedPrefs.getBoolean(FIRST_SETTINGS_OPEN, true)
        set(value) = sharedPrefs.edit { putBoolean(FIRST_SETTINGS_OPEN, value).apply() }

    var firstHide: Boolean
        get() = sharedPrefs.getBoolean(FIRST_HIDE, true)
        set(value) = sharedPrefs.edit { putBoolean(FIRST_HIDE, value).apply() }

    var lockModeOn: Boolean
        get() = sharedPrefs.getBoolean(LOCK_MODE, false)
        set(value) = sharedPrefs.edit { putBoolean(LOCK_MODE, value).apply() }

    var autoShowKeyboard: Boolean
        get() = sharedPrefs.getBoolean(AUTO_SHOW_KEYBOARD, true)
        set(value) = sharedPrefs.edit { putBoolean(AUTO_SHOW_KEYBOARD, value).apply() }

    var keyboardMessageShown: Boolean
        get() = sharedPrefs.getBoolean(KEYBOARD_MESSAGE, false)
        set(value) = sharedPrefs.edit { putBoolean(KEYBOARD_MESSAGE, value).apply() }

    /** When on, the app paints a solid wallpaper matching the current theme. */
    var solidWallpaper: Boolean
        get() = sharedPrefs.getBoolean(SOLID_WALLPAPER, false)
        set(value) = sharedPrefs.edit { putBoolean(SOLID_WALLPAPER, value).apply() }

    var homeAppsNum: Int
        get() = sharedPrefs.getInt(HOME_APPS_NUM, 4)
        set(value) = sharedPrefs.edit { putInt(HOME_APPS_NUM, value).apply() }

    var homeAlignment: Int
        get() = sharedPrefs.getInt(HOME_ALIGNMENT, Gravity.START)
        set(value) = sharedPrefs.edit { putInt(HOME_ALIGNMENT, value).apply() }

    var homeBottomAlignment: Boolean
        get() = sharedPrefs.getBoolean(HOME_BOTTOM_ALIGNMENT, false)
        set(value) = sharedPrefs.edit { putBoolean(HOME_BOTTOM_ALIGNMENT, value).apply() }

    var appLabelAlignment: Int
        get() = sharedPrefs.getInt(APP_LABEL_ALIGNMENT, Gravity.START)
        set(value) = sharedPrefs.edit { putInt(APP_LABEL_ALIGNMENT, value).apply() }

    var showStatusBar: Boolean
        get() = sharedPrefs.getBoolean(STATUS_BAR, false)
        set(value) = sharedPrefs.edit { putBoolean(STATUS_BAR, value).apply() }

    var dateTimeVisibility: Int
        get() = sharedPrefs.getInt(DATE_TIME_VISIBILITY, Constants.DateTime.ON)
        set(value) = sharedPrefs.edit { putInt(DATE_TIME_VISIBILITY, value).apply() }

    var swipeLeftEnabled: Boolean
        get() = sharedPrefs.getBoolean(SWIPE_LEFT_ENABLED, true)
        set(value) = sharedPrefs.edit { putBoolean(SWIPE_LEFT_ENABLED, value).apply() }

    var swipeRightEnabled: Boolean
        get() = sharedPrefs.getBoolean(SWIPE_RIGHT_ENABLED, true)
        set(value) = sharedPrefs.edit { putBoolean(SWIPE_RIGHT_ENABLED, value).apply() }

    var appTheme: Int
        get() = sharedPrefs.getInt(APP_THEME, AppCompatDelegate.MODE_NIGHT_YES)
        set(value) = sharedPrefs.edit { putInt(APP_THEME, value).apply() }

    var textSizeScale: Float
        get() = sharedPrefs.getFloat(TEXT_SIZE_SCALE, 1.0f)
        set(value) = sharedPrefs.edit { putFloat(TEXT_SIZE_SCALE, value).apply() }

    var boldFont: Boolean
        get() = sharedPrefs.getBoolean(BOLD_FONT, false)
        set(value) = sharedPrefs.edit { putBoolean(BOLD_FONT, value).apply() }

    var hideSetDefaultLauncher: Boolean
        get() = sharedPrefs.getBoolean(HIDE_SET_DEFAULT_LAUNCHER, false)
        set(value) = sharedPrefs.edit { putBoolean(HIDE_SET_DEFAULT_LAUNCHER, value).apply() }

    var screenTimeLastUpdated: Long
        get() = sharedPrefs.getLong(SCREEN_TIME_LAST_UPDATED, 0L)
        set(value) = sharedPrefs.edit { putLong(SCREEN_TIME_LAST_UPDATED, value).apply() }

    var launcherRestartTimestamp: Long
        get() = sharedPrefs.getLong(LAUNCHER_RESTART_TIMESTAMP, 0L)
        set(value) = sharedPrefs.edit { putLong(LAUNCHER_RESTART_TIMESTAMP, value).apply() }

    // Home button for recents feature disabled
    // var homeButtonShowRecents: Boolean
    //     get() = sharedPrefs.getBoolean(HOME_BUTTON_SHOW_RECENTS, false)
    //     set(value) = sharedPrefs.edit { putBoolean(HOME_BUTTON_SHOW_RECENTS, value).apply() }

    var hiddenApps: MutableSet<String>
        get() = sharedPrefs.getStringSet(HIDDEN_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = sharedPrefs.edit { putStringSet(HIDDEN_APPS, value).apply() }

    var hiddenAppsUpdated: Boolean
        get() = sharedPrefs.getBoolean(HIDDEN_APPS_UPDATED, false)
        set(value) = sharedPrefs.edit { putBoolean(HIDDEN_APPS_UPDATED, value).apply() }

    var swipeDownAction: Int
        get() = sharedPrefs.getInt(SWIPE_DOWN_ACTION, Constants.SwipeDownAction.NOTIFICATIONS)
        set(value) = sharedPrefs.edit { putInt(SWIPE_DOWN_ACTION, value).apply() }

    var appName1: String
        get() = sharedPrefs.getString(APP_NAME_1, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_NAME_1, value).apply() }

    var appName2: String
        get() = sharedPrefs.getString(APP_NAME_2, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_NAME_2, value).apply() }

    var appName3: String
        get() = sharedPrefs.getString(APP_NAME_3, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_NAME_3, value).apply() }

    var appName4: String
        get() = sharedPrefs.getString(APP_NAME_4, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_NAME_4, value).apply() }

    var appName5: String
        get() = sharedPrefs.getString(APP_NAME_5, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_NAME_5, value).apply() }

    var appName6: String
        get() = sharedPrefs.getString(APP_NAME_6, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_NAME_6, value).apply() }

    var appName7: String
        get() = sharedPrefs.getString(APP_NAME_7, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_NAME_7, value).apply() }

    var appName8: String
        get() = sharedPrefs.getString(APP_NAME_8, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_NAME_8, value).apply() }

    var appPackage1: String
        get() = sharedPrefs.getString(APP_PACKAGE_1, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_PACKAGE_1, value).apply() }

    var appPackage2: String
        get() = sharedPrefs.getString(APP_PACKAGE_2, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_PACKAGE_2, value).apply() }

    var appPackage3: String
        get() = sharedPrefs.getString(APP_PACKAGE_3, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_PACKAGE_3, value).apply() }

    var appPackage4: String
        get() = sharedPrefs.getString(APP_PACKAGE_4, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_PACKAGE_4, value).apply() }

    var appPackage5: String
        get() = sharedPrefs.getString(APP_PACKAGE_5, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_PACKAGE_5, value).apply() }

    var appPackage6: String
        get() = sharedPrefs.getString(APP_PACKAGE_6, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_PACKAGE_6, value).apply() }

    var appPackage7: String
        get() = sharedPrefs.getString(APP_PACKAGE_7, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_PACKAGE_7, value).apply() }

    var appPackage8: String
        get() = sharedPrefs.getString(APP_PACKAGE_8, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_PACKAGE_8, value).apply() }

    var appActivityClassName1: String?
        get() = sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_1, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_ACTIVITY_CLASS_NAME_1, value).apply() }

    var appActivityClassName2: String?
        get() = sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_2, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_ACTIVITY_CLASS_NAME_2, value).apply() }

    var appActivityClassName3: String?
        get() = sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_3, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_ACTIVITY_CLASS_NAME_3, value).apply() }

    var appActivityClassName4: String?
        get() = sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_4, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_ACTIVITY_CLASS_NAME_4, value).apply() }

    var appActivityClassName5: String?
        get() = sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_5, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_ACTIVITY_CLASS_NAME_5, value).apply() }

    var appActivityClassName6: String?
        get() = sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_6, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_ACTIVITY_CLASS_NAME_6, value).apply() }

    var appActivityClassName7: String?
        get() = sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_7, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_ACTIVITY_CLASS_NAME_7, value).apply() }

    var appActivityClassName8: String?
        get() = sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_8, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_ACTIVITY_CLASS_NAME_8, value).apply() }

    var appUser1: String
        get() = sharedPrefs.getString(APP_USER_1, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_USER_1, value).apply() }

    var appUser2: String
        get() = sharedPrefs.getString(APP_USER_2, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_USER_2, value).apply() }

    var appUser3: String
        get() = sharedPrefs.getString(APP_USER_3, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_USER_3, value).apply() }

    var appUser4: String
        get() = sharedPrefs.getString(APP_USER_4, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_USER_4, value).apply() }

    var appUser5: String
        get() = sharedPrefs.getString(APP_USER_5, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_USER_5, value).apply() }

    var appUser6: String
        get() = sharedPrefs.getString(APP_USER_6, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_USER_6, value).apply() }

    var appUser7: String
        get() = sharedPrefs.getString(APP_USER_7, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_USER_7, value).apply() }

    var appUser8: String
        get() = sharedPrefs.getString(APP_USER_8, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_USER_8, value).apply() }

    var appNameSwipeLeft: String
        get() = sharedPrefs.getString(APP_NAME_SWIPE_LEFT, "Camera").toString()
        set(value) = sharedPrefs.edit { putString(APP_NAME_SWIPE_LEFT, value).apply() }

    var appNameSwipeRight: String
        get() = sharedPrefs.getString(APP_NAME_SWIPE_RIGHT, "Phone").toString()
        set(value) = sharedPrefs.edit { putString(APP_NAME_SWIPE_RIGHT, value).apply() }

    var appPackageSwipeLeft: String
        get() = sharedPrefs.getString(APP_PACKAGE_SWIPE_LEFT, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_PACKAGE_SWIPE_LEFT, value).apply() }

    var appActivityClassNameSwipeLeft: String?
        get() = sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT, value).apply() }

    var appPackageSwipeRight: String
        get() = sharedPrefs.getString(APP_PACKAGE_SWIPE_RIGHT, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_PACKAGE_SWIPE_RIGHT, value).apply() }

    var appActivityClassNameRight: String?
        get() = sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT, value).apply() }

    var appUserSwipeLeft: String
        get() = sharedPrefs.getString(APP_USER_SWIPE_LEFT, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_USER_SWIPE_LEFT, value).apply() }

    var appUserSwipeRight: String
        get() = sharedPrefs.getString(APP_USER_SWIPE_RIGHT, "").toString()
        set(value) = sharedPrefs.edit { putString(APP_USER_SWIPE_RIGHT, value).apply() }

    var clockAppPackage: String
        get() = sharedPrefs.getString(CLOCK_APP_PACKAGE, "").toString()
        set(value) = sharedPrefs.edit { putString(CLOCK_APP_PACKAGE, value).apply() }

    var clockAppUser: String
        get() = sharedPrefs.getString(CLOCK_APP_USER, "").toString()
        set(value) = sharedPrefs.edit { putString(CLOCK_APP_USER, value).apply() }

    var clockAppClassName: String?
        get() = sharedPrefs.getString(CLOCK_APP_CLASS_NAME, "").toString()
        set(value) = sharedPrefs.edit { putString(CLOCK_APP_CLASS_NAME, value).apply() }

    var calendarAppPackage: String
        get() = sharedPrefs.getString(CALENDAR_APP_PACKAGE, "").toString()
        set(value) = sharedPrefs.edit { putString(CALENDAR_APP_PACKAGE, value).apply() }

    var calendarAppUser: String
        get() = sharedPrefs.getString(CALENDAR_APP_USER, "").toString()
        set(value) = sharedPrefs.edit { putString(CALENDAR_APP_USER, value).apply() }

    var calendarAppClassName: String?
        get() = sharedPrefs.getString(CALENDAR_APP_CLASS_NAME, "").toString()
        set(value) = sharedPrefs.edit { putString(CALENDAR_APP_CLASS_NAME, value).apply() }

    var screenTimeAppPackage: String
        get() = sharedPrefs.getString(SCREEN_TIME_APP_PACKAGE, "").toString()
        set(value) = sharedPrefs.edit { putString(SCREEN_TIME_APP_PACKAGE, value).apply() }

    var screenTimeAppUser: String
        get() = sharedPrefs.getString(SCREEN_TIME_APP_USER, "").toString()
        set(value) = sharedPrefs.edit { putString(SCREEN_TIME_APP_USER, value).apply() }

    var screenTimeAppClassName: String?
        get() = sharedPrefs.getString(SCREEN_TIME_APP_CLASS_NAME, "").toString()
        set(value) = sharedPrefs.edit { putString(SCREEN_TIME_APP_CLASS_NAME, value).apply() }

    var isShortcut1: Boolean
        get() = sharedPrefs.getBoolean(IS_SHORTCUT_1, false)
        set(value) = sharedPrefs.edit { putBoolean(IS_SHORTCUT_1, value) }

    var shortcutId1: String
        get() = sharedPrefs.getString(SHORTCUT_ID_1, "").toString()
        set(value) = sharedPrefs.edit { putString(SHORTCUT_ID_1, value) }

    var isShortcut2: Boolean
        get() = sharedPrefs.getBoolean(IS_SHORTCUT_2, false)
        set(value) = sharedPrefs.edit { putBoolean(IS_SHORTCUT_2, value) }

    var shortcutId2: String
        get() = sharedPrefs.getString(SHORTCUT_ID_2, "").toString()
        set(value) = sharedPrefs.edit { putString(SHORTCUT_ID_2, value) }

    var isShortcut3: Boolean
        get() = sharedPrefs.getBoolean(IS_SHORTCUT_3, false)
        set(value) = sharedPrefs.edit { putBoolean(IS_SHORTCUT_3, value) }

    var shortcutId3: String
        get() = sharedPrefs.getString(SHORTCUT_ID_3, "").toString()
        set(value) = sharedPrefs.edit { putString(SHORTCUT_ID_3, value) }

    var isShortcut4: Boolean
        get() = sharedPrefs.getBoolean(IS_SHORTCUT_4, false)
        set(value) = sharedPrefs.edit { putBoolean(IS_SHORTCUT_4, value) }

    var shortcutId4: String
        get() = sharedPrefs.getString(SHORTCUT_ID_4, "").toString()
        set(value) = sharedPrefs.edit { putString(SHORTCUT_ID_4, value) }

    var isShortcut5: Boolean
        get() = sharedPrefs.getBoolean(IS_SHORTCUT_5, false)
        set(value) = sharedPrefs.edit { putBoolean(IS_SHORTCUT_5, value) }

    var shortcutId5: String
        get() = sharedPrefs.getString(SHORTCUT_ID_5, "").toString()
        set(value) = sharedPrefs.edit { putString(SHORTCUT_ID_5, value) }

    var isShortcut6: Boolean
        get() = sharedPrefs.getBoolean(IS_SHORTCUT_6, false)
        set(value) = sharedPrefs.edit { putBoolean(IS_SHORTCUT_6, value) }

    var shortcutId6: String
        get() = sharedPrefs.getString(SHORTCUT_ID_6, "").toString()
        set(value) = sharedPrefs.edit { putString(SHORTCUT_ID_6, value) }

    var isShortcut7: Boolean
        get() = sharedPrefs.getBoolean(IS_SHORTCUT_7, false)
        set(value) = sharedPrefs.edit { putBoolean(IS_SHORTCUT_7, value) }

    var shortcutId7: String
        get() = sharedPrefs.getString(SHORTCUT_ID_7, "").toString()
        set(value) = sharedPrefs.edit { putString(SHORTCUT_ID_7, value) }

    var isShortcut8: Boolean
        get() = sharedPrefs.getBoolean(IS_SHORTCUT_8, false)
        set(value) = sharedPrefs.edit { putBoolean(IS_SHORTCUT_8, value) }

    var shortcutId8: String
        get() = sharedPrefs.getString(SHORTCUT_ID_8, "").toString()
        set(value) = sharedPrefs.edit { putString(SHORTCUT_ID_8, value) }

    var shortcutIdSwipeLeft: String
        get() = sharedPrefs.getString(SHORTCUT_ID_SWIPE_LEFT, "").toString()
        set(value) = sharedPrefs.edit { putString(SHORTCUT_ID_SWIPE_LEFT, value) }

    var isShortcutSwipeLeft: Boolean
        get() = sharedPrefs.getBoolean(IS_SHORTCUT_SWIPE_LEFT, false)
        set(value) = sharedPrefs.edit { putBoolean(IS_SHORTCUT_SWIPE_LEFT, value) }

    var shortcutIdSwipeRight: String
        get() = sharedPrefs.getString(SHORTCUT_ID_SWIPE_RIGHT, "").toString()
        set(value) = sharedPrefs.edit { putString(SHORTCUT_ID_SWIPE_RIGHT, value) }

    var isShortcutSwipeRight: Boolean
        get() = sharedPrefs.getBoolean(IS_SHORTCUT_SWIPE_RIGHT, false)
        set(value) = sharedPrefs.edit { putBoolean(IS_SHORTCUT_SWIPE_RIGHT, value) }

    fun getAppName(location: Int): String =
        when (location) {
            1 -> sharedPrefs.getString(APP_NAME_1, "").toString()
            2 -> sharedPrefs.getString(APP_NAME_2, "").toString()
            3 -> sharedPrefs.getString(APP_NAME_3, "").toString()
            4 -> sharedPrefs.getString(APP_NAME_4, "").toString()
            5 -> sharedPrefs.getString(APP_NAME_5, "").toString()
            6 -> sharedPrefs.getString(APP_NAME_6, "").toString()
            7 -> sharedPrefs.getString(APP_NAME_7, "").toString()
            8 -> sharedPrefs.getString(APP_NAME_8, "").toString()
            else -> ""
        }

    fun getAppPackage(location: Int): String =
        when (location) {
            1 -> sharedPrefs.getString(APP_PACKAGE_1, "").toString()
            2 -> sharedPrefs.getString(APP_PACKAGE_2, "").toString()
            3 -> sharedPrefs.getString(APP_PACKAGE_3, "").toString()
            4 -> sharedPrefs.getString(APP_PACKAGE_4, "").toString()
            5 -> sharedPrefs.getString(APP_PACKAGE_5, "").toString()
            6 -> sharedPrefs.getString(APP_PACKAGE_6, "").toString()
            7 -> sharedPrefs.getString(APP_PACKAGE_7, "").toString()
            8 -> sharedPrefs.getString(APP_PACKAGE_8, "").toString()
            else -> ""
        }

    fun getAppActivityClassName(location: Int): String =
        when (location) {
            1 -> sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_1, "").toString()
            2 -> sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_2, "").toString()
            3 -> sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_3, "").toString()
            4 -> sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_4, "").toString()
            5 -> sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_5, "").toString()
            6 -> sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_6, "").toString()
            7 -> sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_7, "").toString()
            8 -> sharedPrefs.getString(APP_ACTIVITY_CLASS_NAME_8, "").toString()
            else -> ""
        }

    fun getAppUser(location: Int): String =
        when (location) {
            1 -> sharedPrefs.getString(APP_USER_1, "").toString()
            2 -> sharedPrefs.getString(APP_USER_2, "").toString()
            3 -> sharedPrefs.getString(APP_USER_3, "").toString()
            4 -> sharedPrefs.getString(APP_USER_4, "").toString()
            5 -> sharedPrefs.getString(APP_USER_5, "").toString()
            6 -> sharedPrefs.getString(APP_USER_6, "").toString()
            7 -> sharedPrefs.getString(APP_USER_7, "").toString()
            8 -> sharedPrefs.getString(APP_USER_8, "").toString()
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
        if (clockAppPackage == packageName) clockAppClassName = activityClassName
        if (calendarAppPackage == packageName) calendarAppClassName = activityClassName
        if (screenTimeAppPackage == packageName) screenTimeAppClassName = activityClassName
        if (appPackageSwipeLeft == packageName) appActivityClassNameSwipeLeft = activityClassName
        if (appPackageSwipeRight == packageName) appActivityClassNameRight = activityClassName
    }

    fun getAppRenameLabel(appPackage: String): String = sharedPrefs.getString(appPackage, "").toString()

    fun setAppRenameLabel(
        appPackage: String,
        renameLabel: String,
    ) = sharedPrefs.edit { putString(appPackage, renameLabel) }
}
