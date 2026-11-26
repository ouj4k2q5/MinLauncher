package app.minlauncher.ui.home

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import app.minlauncher.R

/**
 * The home screen's text styles. Every entry keeps the old XML style's size,
 * color, shadow and family: slots are TextLarge (mainFontFamily, pressed
 * state at primaryColorTrans80 like the old text_colors_default selector),
 * the set-as-default line is TextMedium plus the old <u> underline, screen
 * time and tips are TextSmall, and the clock fixes sans-serif-light like
 * fragment_home.xml's TextClock did (independent of the bold-font overlay).
 */
@Immutable
internal data class HomePalette(
    val slot: TextStyle,
    val slotPressed: TextStyle,
    val slotHint: TextStyle,
    val medium: TextStyle,
    val mediumUnderline: TextStyle,
    val small: TextStyle,
    val clock: TextStyle,
    val date: TextStyle,
)

@Composable
internal fun homePalette(colors: HomeColors): HomePalette {
    val density = LocalDensity.current
    val largeSize = with(density) { dimensionResource(R.dimen.text_large).toSp() }.value.sp
    val smallSize = with(density) { dimensionResource(R.dimen.text_small).toSp() }.value.sp
    val timeSize = with(density) { dimensionResource(R.dimen.time_size).toSp() }.value.sp
    val dateSize = with(density) { dimensionResource(R.dimen.date_size).toSp() }.value.sp
    val shadow =
        Shadow(
            color = Color(colors.textShadow),
            offset = Offset(0f, 2f),
            blurRadius = 4f,
        )
    val primary = Color(colors.primaryColor)
    val pressed = Color(colors.primaryColorTrans80)
    val slot =
        TextStyle(
            fontSize = largeSize,
            color = primary,
            shadow = shadow,
            fontFamily = colors.fontFamily,
        )
    val medium = TextStyle(fontSize = 20.sp, color = primary, shadow = shadow)
    val small = TextStyle(fontSize = smallSize, color = primary, shadow = shadow)
    val clock =
        TextStyle(
            fontSize = timeSize,
            color = primary,
            shadow = shadow,
            fontFamily = FontFamily(Typeface.create("sans-serif-light", Typeface.NORMAL)),
        )
    return HomePalette(
        slot = slot,
        slotPressed = slot.copy(color = pressed),
        slotHint = slot.copy(color = pressed),
        medium = medium,
        mediumUnderline = medium.copy(textDecoration = TextDecoration.Underline),
        small = small,
        clock = clock,
        date = TextStyle(fontSize = dateSize, color = primary, shadow = shadow),
    )
}
