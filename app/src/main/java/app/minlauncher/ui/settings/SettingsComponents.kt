package app.minlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val CardShape = RoundedCornerShape(32.dp)
private val PickerShape = RoundedCornerShape(10.dp)

@Composable
internal fun SettingsCard(
    modifier: Modifier,
    palette: SettingsPalette,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(CardShape)
                .background(Color(palette.colors.shadeDark), CardShape)
                .border(0.5.dp, Color(palette.colors.colorTrans50), CardShape)
                .padding(horizontal = 12.dp, vertical = 16.dp),
        content = content,
    )
}

@Composable
internal fun SectionTitle(
    text: String,
    palette: SettingsPalette,
) {
    Text(
        text = text,
        style = palette.text.large,
        color = Color(palette.colors.primaryColor),
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
internal fun ActionRow(
    content: SettingRowContent,
    palette: SettingsPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ActionRowStyle = ActionRowStyle(),
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(content.tag)
                .combinedClickable(onClick = onClick, onLongClick = style.onLongClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = content.label,
            style = palette.text.small,
            color = Color(palette.colors.primaryColor),
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        Text(
            text = content.value,
            style = palette.text.smallBold,
            color = style.valueColor ?: Color(palette.colors.primaryColor),
            maxLines = 1,
            modifier = Modifier.padding(start = 24.dp, end = 4.dp),
        )
    }
}

@Composable
internal fun PickerRow(
    spec: PickerRowSpec,
    controller: SettingsPickerController,
    palette: SettingsPalette,
    modifier: Modifier = Modifier,
    pickerContent: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(spec.content.tag)
                    .combinedClickable(
                        onClick = { controller.open(spec.target) },
                        onLongClick = spec.onLongClick,
                    ).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = spec.content.label,
                style = palette.text.small,
                color = Color(palette.colors.primaryColor),
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Text(
                text = spec.content.value,
                style = palette.text.smallBold,
                color = Color(palette.colors.primaryColor),
                modifier = Modifier.padding(start = 24.dp, end = 4.dp),
            )
        }
        if (controller.current == spec.target) {
            PickerSurface(palette, pickerContent)
        }
    }
}

/** Inline picker: rounded surface with the XML's top-to-bottom inverse gradient. */
@Composable
private fun BoxScope.PickerSurface(
    palette: SettingsPalette,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .matchParentSize()
                .shadow(8.dp, PickerShape)
                .clip(PickerShape)
                .background(
                    Brush.verticalGradient(
                        0f to Color(palette.colors.inverse),
                        0.5f to Color(palette.colors.inverse),
                        1f to Color(palette.colors.inverseTrans80),
                    ),
                ),
        content = content,
    )
}

@Composable
internal fun RowScope.WeightedPickerOption(
    text: String,
    tag: String,
    style: TextStyle,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = style,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .testTag(tag)
                .clickable(onClick = onClick),
    )
}

@Composable
internal fun PlainPickerOption(
    text: String,
    tag: String,
    style: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = style,
        modifier =
            modifier
                .wrapContentWidth()
                .testTag(tag)
                .clickable(onClick = onClick),
    )
}

@Composable
internal fun swipeValueColor(
    enabled: Boolean,
    palette: SettingsPalette,
): Color = Color(if (enabled) palette.colors.primaryColor else palette.colors.colorTrans50)
