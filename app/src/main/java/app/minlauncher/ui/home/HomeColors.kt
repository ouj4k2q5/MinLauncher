package app.minlauncher.ui.home

import android.app.Activity
import app.minlauncher.R
import app.minlauncher.ui.drawer.drawerFontFamily
import app.minlauncher.ui.theme.requireColor
import app.minlauncher.ui.theme.requireString

/**
 * Resolves [HomeColors] from the activity theme. Lives next to the screen so the
 * color resolution can move with the rest of the home UI; the Fragment only
 * calls it.
 */
internal fun Activity.resolveHomeColors(): HomeColors {
    // Resolve once, from the activity theme, so the day/night variant is the
    // one the (about to be displayed) screen uses. Theme switches recreate.
    val typedArray = obtainStyledAttributes(R.styleable.HomeColors)
    return try {
        HomeColors(
            primaryColor =
                typedArray.requireColor(R.styleable.HomeColors_primaryColor, "primaryColor"),
            primaryColorTrans80 =
                typedArray.requireColor(R.styleable.HomeColors_primaryColorTrans80, "primaryColorTrans80"),
            textShadow =
                typedArray.requireColor(R.styleable.HomeColors_primaryTextShadowColor, "primaryTextShadowColor"),
            fontFamily =
                drawerFontFamily(
                    typedArray.requireString(R.styleable.HomeColors_mainFontFamily, "mainFontFamily"),
                ),
        )
    } finally {
        typedArray.recycle()
    }
}
