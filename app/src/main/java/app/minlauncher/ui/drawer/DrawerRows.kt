package app.minlauncher.ui.drawer

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.minlauncher.R
import app.minlauncher.data.AppModel

/**
 * A normal app row: the label (with the ✦ marker for new apps and a dot for
 * other-profile apps), long-press opens the action menu.
 */
@Composable
internal fun TitleRow(
    app: AppModel,
    rows: DrawerRowState,
    onOpenMenu: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    // The old rows had no background, hence no ripple.
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { rows.callbacks.onAppClick(app) },
                    onLongClick = onOpenMenu,
                ),
    ) {
        Text(
            text = app.appLabel + if (app.isNew) " ✦" else "",
            style =
                rows.palette.large.copy(
                    textAlign = rows.textAlign,
                    color = if (pressed) Color(rows.primaryColorTrans80) else rows.palette.large.color,
                ),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = dimensionResource(R.dimen.app_padding_vertical),
                    ),
        )
        // otherProfileIndicator: a 5dp dot in the row gutter, outside the
        // title's 24dp start padding, 10dp from the row edge and nudged 1dp
        // down like the old ImageView (marginStart=10dp, marginTop=1dp).
        if (app.user != myUserHandle()) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp, top = 1.dp)
                        .size(5.dp)
                        .background(rows.palette.large.color, CircleShape)
                        .testTag("drawer.otherProfileIndicator"),
            )
        }
    }
}

/**
 * One app row. The title always stays composed and defines the row height:
 * the old adapter hid appTitle with INVISIBLE while the menu/rename layout
 * filled the row at match_parent, so opening either one never shifted the
 * rows below. Compose has no INVISIBLE, so the title is made transparent and
 * dropped from the semantics tree instead, and the overlay fills its size.
 */
@Composable
internal fun DrawerRowItem(
    app: AppModel,
    rows: DrawerRowState,
) {
    // Reproduces the old adapter's bind() reset: when the list disposes this
    // row (scrolled out of the viewport, or removed by hide), close its menu
    // and rename so they do not reappear when the row comes back.
    DisposableEffect(app) {
        onDispose { rows.onRowDisposed(app) }
    }
    val menuOpen = rows.menuFor == app
    val renameOpen = rows.renameFor == app
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier =
                if (menuOpen || renameOpen) {
                    Modifier.alpha(0f).clearAndSetSemantics { }
                } else {
                    Modifier
                },
        ) {
            TitleRow(app, rows) { rows.openMenu(app) }
        }
        if (menuOpen) {
            MenuRow(app, rows, Modifier.matchParentSize())
        }
        if (renameOpen) {
            RenameRow(app, rows, Modifier.matchParentSize())
        }
    }
}

/**
 * The inline rename UI laid over the row: a text field seeded with the current
 * label (selected), the real app name as hint, and Save / Close actions.
 */
@Composable
internal fun RenameRow(
    app: AppModel,
    rows: DrawerRowState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val renameFocus = remember { FocusRequester() }
    val realName = remember(app.appPackage, app.user) { getAppName(context, app.appPackage, app.user) }
    var value by remember(app) {
        mutableStateOf(TextFieldValue(app.appLabel, TextRange(0, app.appLabel.length)))
    }

    fun save(label: String) {
        if (label.isNotBlank()) {
            rows.callbacks.onAppRename(app, label)
            rows.closeRename()
        }
    }

    Row(
        modifier = modifier.swallowTaps(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RenameTextField(
            value = value,
            onValueChange = { value = it },
            palette = rows.palette,
            hint = realName,
            focusRequester = renameFocus,
            onDone = { save(value.text.trim()) },
            modifier = Modifier.weight(1f),
        )
        MenuButton(
            action =
                DrawerMenuAction(
                    label = stringResource(R.string.rename),
                    icon = R.drawable.ic_check,
                    alpha = 1f,
                    testTag = "drawer.saveRename",
                ) {
                    keyboard?.hide()
                    val label = value.text.trim()
                    if (label.isNotBlank()) {
                        save(label)
                    } else {
                        // An empty field restores the real app name, like the old save button.
                        rows.callbacks.onAppRename(app, realName)
                        rows.closeRename()
                    }
                },
            rows = rows,
        )
        MenuButton(
            action =
                DrawerMenuAction(
                    label = stringResource(R.string.close),
                    icon = R.drawable.ic_close,
                    alpha = 1f,
                    testTag = "drawer.renameClose",
                ) { rows.closeRename() },
            rows = rows,
        )
    }

    // The old flow focused and showed the keyboard when the rename UI opened.
    LaunchedEffect(app) {
        renameFocus.requestFocus()
        keyboard?.show()
    }
}

@Suppress("LongParameterList") // plain wiring of the rename field's inputs
@Composable
private fun RenameTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    palette: DrawerPalette,
    hint: String,
    focusRequester: FocusRequester,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.text.length <= 30) onValueChange(it) },
        textStyle = palette.large,
        cursorBrush = SolidColor(palette.large.color),
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier =
            modifier
                // The old EditText carried the vertical padding inside the
                // field while the save/close buttons centered in the full
                // row height; keep that split.
                .padding(
                    start = 20.dp,
                    top = dimensionResource(R.dimen.app_padding_vertical),
                    end = 8.dp,
                    bottom = dimensionResource(R.dimen.app_padding_vertical),
                ).focusRequester(focusRequester)
                .testTag("drawer.renameInput"),
        decorationBox = { innerTextField ->
            Box {
                if (value.text.isEmpty()) Text(hint, style = palette.hint)
                innerTextField()
            }
        },
    )
}

/** The Private Space section header: tap toggles the lock, long-press opens its settings. */
@Composable
internal fun PrivateSpaceHeaderRow(rows: DrawerRowState) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val baseColor = if (pressed) Color(rows.primaryColorTrans80) else rows.palette.large.color
    Text(
        text = stringResource(R.string.private_space),
        style =
            rows.palette.large.copy(
                textAlign = rows.textAlign,
                // The old TextView applied alpha=0.8 on top of the pressed/normal selector color.
                color = baseColor.copy(alpha = baseColor.alpha * 0.8f),
            ),
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = rows.callbacks.onPrivateSpaceToggle,
                    onLongClick = rows.callbacks.onPrivateSpaceSettings,
                ).padding(horizontal = 20.dp, vertical = dimensionResource(R.dimen.app_padding_vertical))
                .testTag("drawer.privateSpace"),
    )
}

private fun myUserHandle(): UserHandle = android.os.Process.myUserHandle()

/** LauncherApps first, PackageManager fallback — verbatim from the old adapter. */
@Suppress("TooGenericExceptionCaught") // the old adapter fell back to "" on any lookup failure
private fun getAppName(
    context: Context,
    appPackage: String,
    user: UserHandle,
): String {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    return try {
        val activityList = launcherApps.getActivityList(appPackage, user)
        if (activityList.isNotEmpty()) {
            activityList.first().label.toString()
        } else {
            val packageManager = context.packageManager
            packageManager
                .getApplicationLabel(
                    packageManager.getApplicationInfo(appPackage, 0),
                ).toString()
        }
    } catch (_: Exception) {
        "" // As a fallback, display an empty string.
    }
}
