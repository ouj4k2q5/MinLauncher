package app.minlauncher.ui.settings

import android.app.Activity
import app.minlauncher.R
import app.minlauncher.ui.theme.requireColor

/**
 * Resolves [SettingsColors] from the activity theme. Lives next to the screen so
 * the color resolution can move with the rest of the settings UI; SettingEntry
 * only calls it.
 */
internal fun Activity.resolveSettingsColors(): SettingsColors {
    // Resolve once, from the activity theme, so the day/night variant is the
    // one the (about to be displayed) screen uses. Theme switches recreate.
    val typedArray = obtainStyledAttributes(R.styleable.SettingsColors)
    return try {
        SettingsColors(
            primaryColor = typedArray.requireColor(R.styleable.SettingsColors_primaryColor, "primaryColor"),
            shadeDark =
                typedArray.requireColor(R.styleable.SettingsColors_primaryShadeDarkColor, "primaryShadeDarkColor"),
            colorTrans50 =
                typedArray.requireColor(R.styleable.SettingsColors_primaryColorTrans50, "primaryColorTrans50"),
            inverse =
                typedArray.requireColor(R.styleable.SettingsColors_primaryInverseColor, "primaryInverseColor"),
            inverseTrans80 =
                typedArray.requireColor(
                    R.styleable.SettingsColors_primaryColorInverseTrans80,
                    "primaryColorInverseTrans80",
                ),
            inverseTrans90 =
                typedArray.requireColor(
                    R.styleable.SettingsColors_primaryColorInverseTrans90,
                    "primaryColorInverseTrans90",
                ),
            dialogShade =
                typedArray.requireColor(R.styleable.SettingsColors_dialogShadeColor, "dialogShadeColor"),
            textShadow =
                typedArray.requireColor(
                    R.styleable.SettingsColors_primaryTextShadowColor,
                    "primaryTextShadowColor",
                ),
        )
    } finally {
        typedArray.recycle()
    }
}
