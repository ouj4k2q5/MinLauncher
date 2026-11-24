package app.minlauncher.ui.drawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import app.minlauncher.R

/**
 * Maps a `mainFontFamily` theme value (an Android font-family name such as
 * "sans-serif-light" or "sans-serif") to a Compose [FontFamily].
 */
internal fun drawerFontFamily(name: String): FontFamily =
    FontFamily(android.graphics.Typeface.create(name, android.graphics.Typeface.NORMAL))

internal data class DrawerPalette(
    val large: TextStyle,
    val small: TextStyle,
    /** 12sp like the old menu labels, which overrode TextSmall's size. */
    val menuLabel: TextStyle,
    /** The rename field's hint, at the old primaryColorTrans50. */
    val hint: TextStyle,
)

/** M3 defaults are accepted; only the sizes, family and XML text shadow are applied. */
@Composable
internal fun drawerPalette(colors: DrawerColors): DrawerPalette {
    val density = LocalDensity.current
    val largeSize = with(density) { dimensionResource(R.dimen.text_large).toSp() }.value.sp
    val smallSize = with(density) { dimensionResource(R.dimen.text_small).toSp() }.value.sp
    val shadow =
        Shadow(
            color = Color(colors.textShadow),
            offset = Offset(0f, 2f),
            blurRadius = 4f,
        )
    val large =
        TextStyle(
            fontSize = largeSize,
            color = Color(colors.primaryColor),
            shadow = shadow,
            fontFamily = colors.fontFamily,
        )
    return DrawerPalette(
        large = large,
        small = TextStyle(fontSize = smallSize, color = Color(colors.primaryColor), shadow = shadow),
        menuLabel = TextStyle(fontSize = 12.sp, color = Color(colors.primaryColor)),
        hint = large.copy(color = Color(colors.colorTrans50), shadow = null),
    )
}
