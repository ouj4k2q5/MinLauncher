package app.minlauncher.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable

/**
 * Material Expressive theme for the launcher, built on Material3 1.5.0
 * (Compose BOM 2026.11.01).
 *
 * Deliberately plain: the light scheme is [expressiveLightColorScheme] and the
 * dark scheme stays the Material3 [darkColorScheme] default, because 1.5.0 ships
 * no dark expressive variant and the plan calls only for the light one. Motion
 * uses [MaterialExpressiveTheme]'s default expressive scheme (no override). The
 * app's black/white XML palette is not mirrored into Compose (user decision,
 * 2026-11-23); when screens migrate (8a+) they can accept the defaults or
 * customize specific slots, referencing colors from colors.xml rather than
 * duplicating hex values here.
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
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) darkColorScheme() else expressiveLightColorScheme(),
        content = content,
    )
}
