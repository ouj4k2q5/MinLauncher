package app.minlauncher.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.minlauncher.R
import app.minlauncher.data.AppModel
import app.minlauncher.data.Constants
import app.minlauncher.helper.isSystemApp

/**
 * Consume taps on an overlay row so they never reach the title hiding
 * underneath — the old menu/rename layouts were clickable for exactly this.
 */
@Composable
internal fun Modifier.swallowTaps(): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return clickable(interactionSource = interaction, indication = null) {}
}

/**
 * The long-press action menu laid over the row: Delete / Rename (hidden in
 * the hidden-apps list) / Hide (or Show) / Info / Close, with the old dimmed
 * variants — delete dimmed for system apps, hide dimmed for pinned shortcuts.
 */
@Composable
internal fun MenuRow(
    app: AppModel,
    rows: DrawerRowState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                // The old menu layout carried a 1dp vertical margin outside the
                // shaded background, so the shade never touched the row edges.
                // Taps are swallowed outside the padding too, so the band never
                // reaches the app row underneath.
                .swallowTaps()
                .padding(vertical = 1.dp)
                .background(Color(rows.shadeDark)),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        menuActions(app, rows).forEach { action ->
            MenuButton(action, rows, Modifier.weight(1f))
        }
    }
}

@Composable
private fun menuActions(
    app: AppModel,
    rows: DrawerRowState,
): List<DrawerMenuAction> {
    val context = LocalContext.current
    val isPinned = app is AppModel.PinnedShortcut
    val deleteEnabled =
        remember(app.appPackage, app.user) {
            isPinned || !context.isSystemApp(app.appPackage, app.user)
        }
    val hiddenList = rows.flag == Constants.FLAG_HIDDEN_APPS
    return listOfNotNull(
        DrawerMenuAction(
            label = stringResource(R.string.delete),
            icon = R.drawable.ic_delete,
            alpha = if (deleteEnabled) 1f else 0.5f,
            testTag = "drawer.appDelete",
        ) {
            rows.callbacks.onAppDelete(app)
            rows.closeMenu()
        },
        if (!hiddenList) {
            DrawerMenuAction(
                label = stringResource(R.string.rename),
                icon = R.drawable.ic_rename,
                alpha = 1f,
                testTag = "drawer.appRename",
            ) { rows.openRename(app) }
        } else {
            null
        },
        DrawerMenuAction(
            label = stringResource(if (hiddenList) R.string.adapter_show else R.string.adapter_hide),
            icon = R.drawable.ic_hide,
            alpha = if (isPinned) 0.5f else 1f,
            testTag = "drawer.appHide",
        ) {
            rows.callbacks.onAppHide(app)
            rows.closeMenu()
        },
        DrawerMenuAction(
            label = stringResource(R.string.info),
            icon = R.drawable.ic_info,
            alpha = 1f,
            testTag = "drawer.appInfo",
        ) {
            rows.callbacks.onAppInfo(app)
            rows.closeMenu()
        },
        DrawerMenuAction(
            label = stringResource(R.string.close),
            icon = R.drawable.ic_close,
            alpha = 1f,
            testTag = "drawer.menuClose",
        ) { rows.closeMenu() },
    )
}

@Composable
internal fun MenuButton(
    action: DrawerMenuAction,
    rows: DrawerRowState,
    modifier: Modifier = Modifier,
) {
    val style = rows.palette.menuLabel
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val textColor = if (pressed) Color(rows.primaryColorTrans80) else style.color
    Column(
        modifier =
            modifier
                .alpha(action.alpha)
                .clickable(
                    // The old menu buttons had no ripple either.
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = action.onClick,
                ).padding(horizontal = 8.dp)
                .testTag(action.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(action.icon),
            contentDescription = null,
            tint = style.color,
        )
        Text(action.label, style = style.copy(color = textColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
