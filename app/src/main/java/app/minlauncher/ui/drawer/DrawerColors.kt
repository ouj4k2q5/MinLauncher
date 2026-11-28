package app.minlauncher.ui.drawer

import android.app.Activity
import app.minlauncher.R
import app.minlauncher.ui.theme.requireColor
import app.minlauncher.ui.theme.requireString

/**
 * Resolves [DrawerColors] from the activity theme. Lives next to the screen so
 * the color resolution can move with the rest of the drawer UI; the Fragment
 * only calls it.
 */
internal fun Activity.resolveDrawerColors(): DrawerColors {
    // Resolve once, from the activity theme, so the day/night variant is the
    // one the (about to be displayed) screen uses. Theme switches recreate.
    val typedArray = obtainStyledAttributes(R.styleable.DrawerColors)
    return try {
        DrawerColors(
            primaryColor =
                typedArray.requireColor(R.styleable.DrawerColors_primaryColor, "primaryColor"),
            shadeDark =
                typedArray.requireColor(
                    R.styleable.DrawerColors_primaryShadeDarkColor,
                    "primaryShadeDarkColor",
                ),
            colorTrans50 =
                typedArray.requireColor(R.styleable.DrawerColors_primaryColorTrans50, "primaryColorTrans50"),
            primaryColorTrans80 =
                typedArray.requireColor(
                    R.styleable.DrawerColors_primaryColorTrans80,
                    "primaryColorTrans80",
                ),
            textShadow =
                typedArray.requireColor(
                    R.styleable.DrawerColors_primaryTextShadowColor,
                    "primaryTextShadowColor",
                ),
            fontFamily =
                drawerFontFamily(
                    typedArray.requireString(
                        R.styleable.DrawerColors_mainFontFamily,
                        "mainFontFamily",
                    ),
                ),
        )
    } finally {
        typedArray.recycle()
    }
}
