package app.minlauncher.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Foundation-only Material3 theme for the launcher.
 *
 * Deliberately plain: the light/dark color schemes are the Material3
 * defaults. The app's black/white XML palette is not mirrored into Compose
 * (user decision, 2026-11-23); when screens migrate (8a+) they can accept
 * the defaults or customize specific slots, referencing colors from
 * colors.xml rather than duplicating hex values here.
 *
 * [appTheme] uses the same `AppCompatDelegate.MODE_NIGHT_*` values that
 * `Prefs.appTheme` stores and `MainActivity` hands to
 * `AppCompatDelegate.setDefaultNightMode`: [AppCompatDelegate.MODE_NIGHT_NO]
 * forces the light scheme, [AppCompatDelegate.MODE_NIGHT_YES] the dark one,
 * and [AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM] (or `null`, i.e. no
 * stored preference yet) defers to the system night mode via
 * [isSystemInDarkTheme] — a [androidx.compose.ui.platform.LocalConfiguration]
 * state read, so the theme recomposes when the system uiMode flips. That
 * matters because the manifest handles `uiMode` config changes without
 * recreating the activity.
 *
 * No screen is Compose yet; Stage 8a is the first consumer.
 */
@Composable
fun MinLauncherTheme(
    appTheme: Int?,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (appTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> false
            AppCompatDelegate.MODE_NIGHT_YES -> true
            else -> isSystemInDarkTheme()
        }
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
