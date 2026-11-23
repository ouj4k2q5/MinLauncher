package app.minlauncher.ui.settings

import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.minlauncher.R
import app.minlauncher.data.Constants

internal data class SettingsTextStyles(
    val large: TextStyle,
    val small: TextStyle,
    val smallBold: TextStyle,
)

internal data class SettingsPalette(
    val colors: SettingsColors,
    val text: SettingsTextStyles,
)

/** M3 defaults are accepted; only the sizes from dimens and the XML text shadow are applied. */
@Composable
internal fun settingsPalette(colors: SettingsColors): SettingsPalette {
    val density = LocalDensity.current
    val largeSize = with(density) { dimensionResource(R.dimen.text_large).toSp() }.value.sp
    val smallSize = with(density) { dimensionResource(R.dimen.text_small).toSp() }.value.sp
    val shadow =
        Shadow(
            color = Color(colors.textShadow),
            offset = with(density) { Offset(0f, 2.dp.toPx()) },
            blurRadius = with(density) { 4.dp.toPx() },
        )
    val small = TextStyle(fontSize = smallSize, color = Color(colors.primaryColor), shadow = shadow)
    return SettingsPalette(
        colors = colors,
        text =
            SettingsTextStyles(
                large = TextStyle(fontSize = largeSize, color = Color(colors.primaryColor), shadow = shadow),
                small = small,
                smallBold = small.copy(fontWeight = FontWeight.Bold),
            ),
    )
}

@Composable
internal fun dateTimeLabel(value: Int): String =
    when (value) {
        Constants.DateTime.DATE_ONLY -> stringResource(R.string.date)
        Constants.DateTime.ON -> stringResource(R.string.on)
        else -> stringResource(R.string.off)
    }

@Composable
internal fun alignmentLabel(value: Int): String =
    when (value) {
        Gravity.START -> stringResource(R.string.left)
        Gravity.CENTER -> stringResource(R.string.center)
        Gravity.END -> stringResource(R.string.right)
        else -> stringResource(R.string.left)
    }

@Composable
internal fun themeLabel(appTheme: Int): String =
    when (appTheme) {
        AppCompatDelegate.MODE_NIGHT_YES -> stringResource(R.string.dark)
        AppCompatDelegate.MODE_NIGHT_NO -> stringResource(R.string.light)
        else -> stringResource(R.string.system_default)
    }
