package app.minlauncher.ui.drawer

import android.content.res.Configuration
import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.minlauncher.R
import app.minlauncher.data.AppModel
import app.minlauncher.helper.isEinkDisplay

/**
 * The app drawer: a search field above the (filtered) app list — a fixed 24dp
 * top gap, then weighted spacers around the search row, then the list — with
 * portrait as the canonical variant (landscape adds 56dp horizontal margins).
 */
@Composable
internal fun DrawerScreen(
    state: DrawerUiState,
    colors: DrawerColors,
    callbacks: DrawerCallbacks,
) {
    val palette = drawerPalette(colors)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val searchFocus = remember { FocusRequester() }
    val textAlign = alignmentToTextAlign(state.appLabelAlignment)
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    // Plain remember, not saveable: reset per composition so a restore is detectable (saveable reopens the hole).
    var userEditedQuery by remember { mutableStateOf(false) }
    val rows =
        remember {
            DrawerRowState(state.flag, palette, colors.shadeDark, colors.primaryColorTrans80, textAlign, callbacks)
        }

    val filtered = remember(state.apps, query.text) { filterApps(state.apps, query.text) }

    AutoLaunchOnSingleResult(filtered, query, userEditedQuery, state.flag, callbacks)
    ObserveScrollForKeyboard(listState, state.autoShowKeyboard, searchFocus, keyboard)
    ShowKeyboardOnStart(state, searchFocus, keyboard)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(colors.shadeDark))
                .nestedScroll(rememberPullDownExit(callbacks.onExitDrawer)),
    ) {
        Spacer(Modifier.height(24.dp))
        Spacer(
            Modifier
                .fillMaxWidth()
                .weight(0.1f),
        )
        DrawerSearchArea(
            query = query,
            onQueryChange = {
                query = it
                userEditedQuery = true
            },
            canRename = state.canRename,
            palette = palette,
            textAlign = textAlign,
            searchHint = state.searchHint,
            focusRequester = searchFocus,
            onSubmit = { submitQuery(query.text, filtered, callbacks) },
            onRenameHomeSlot = callbacks.onRenameHomeSlot,
            isLandscape = isLandscape,
        )
        Spacer(
            Modifier
                .fillMaxWidth()
                .weight(0.06f),
        )
        DrawerAppList(
            filtered = filtered,
            rows = rows,
            listState = listState,
            modifier = Modifier.weight(1f).then(if (isLandscape) Modifier.padding(horizontal = 56.dp) else Modifier),
        )
    }
}

/**
 * The search row: the query field and (while customizing a home slot with a
 * typed name) the top Rename button.
 */
@Suppress("LongParameterList") // one flat slice of the search row's inputs
@Composable
private fun DrawerSearchArea(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    canRename: Boolean,
    palette: DrawerPalette,
    textAlign: TextAlign,
    searchHint: String,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
    onRenameHomeSlot: (String) -> Boolean,
    isLandscape: Boolean,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (isLandscape) Modifier.padding(horizontal = 56.dp) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            style = palette.large.copy(textAlign = textAlign),
            hint = searchHint,
            focusRequester = focusRequester,
            onSubmit = onSubmit,
            modifier =
                Modifier
                    .weight(1f)
                    .then(
                        if (isLandscape) {
                            // 24dp matches the old SearchView's internal
                            // SearchAutoComplete inset on top of its padding.
                            Modifier.padding(horizontal = 24.dp)
                        } else {
                            Modifier.padding(start = 24.dp, end = 16.dp)
                        },
                    ),
        )
        if (canRename && query.text.isNotBlank()) {
            Text(
                text = stringResource(R.string.rename).uppercase(),
                style = palette.small,
                modifier =
                    Modifier
                        .testTag("drawer.rename")
                        .clickable(
                            // The old Rename TextView had no ripple.
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!onRenameHomeSlot(query.text.trim())) {
                                focusRequester.requestFocus()
                                keyboard?.show()
                            }
                        }.padding(end = if (isLandscape) 24.dp else 20.dp),
            )
        }
    }
}

/** The old SearchView: text_large, primary color, no cursor, flag-dependent hint, search IME action. */
@Suppress("LongParameterList") // plain wiring of the query field's inputs
@Composable
private fun SearchField(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    style: TextStyle,
    hint: String,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        // maxLength 200, like the AppSearchText style the SearchView used.
        onValueChange = { if (it.text.length <= 200) onQueryChange(it) },
        textStyle = style,
        cursorBrush = SolidColor(Color.Transparent),
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrect = false,
                imeAction = ImeAction.Search,
            ),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        modifier =
            modifier
                .focusRequester(focusRequester)
                .testTag("drawer.search"),
        decorationBox = { innerTextField ->
            Box {
                if (query.text.isEmpty()) {
                    // The old style set the hint to the same primary color.
                    Text(hint, style = style)
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun DrawerAppList(
    filtered: List<AppModel>,
    rows: DrawerRowState,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    // The old RecyclerView set OVER_SCROLL_NEVER only on e-ink displays;
    // regular devices keep the platform glow/stretch.
    val overscroll = rememberOverscrollEffect()
    val isEink = LocalContext.current.isEinkDisplay()
    LazyColumn(
        state = listState,
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("drawer.list"),
        // The old adapter appended one empty row for bottom breathing
        // room; contentPadding replaces it without a phantom list item.
        contentPadding = PaddingValues(bottom = 64.dp),
        overscrollEffect = if (isEink) null else overscroll,
    ) {
        items(filtered, key = ::drawerItemKey) { app ->
            when (app) {
                is AppModel.PrivateSpaceHeader -> PrivateSpaceHeaderRow(rows)
                else -> DrawerRowItem(app, rows)
            }
        }
    }
}

/**
 * The old onQueryTextSubmit: "!" goes to web search, an empty result set goes
 * to the system search, anything else launches the first non-header match.
 */
private fun submitQuery(
    query: String,
    filtered: List<AppModel>,
    callbacks: DrawerCallbacks,
) {
    if (query.startsWith("!")) {
        callbacks.onBangSearch(query)
        return
    }
    val first = filtered.firstOrNull { it !is AppModel.PrivateSpaceHeader }
    if (filtered.isEmpty()) {
        callbacks.onOpenSearch(query.trim())
    } else {
        first?.let(callbacks.onAppClick)
    }
}

private fun alignmentToTextAlign(gravity: Int): TextAlign =
    when (gravity) {
        Gravity.CENTER -> TextAlign.Center
        Gravity.END -> TextAlign.End
        else -> TextAlign.Start
    }

// A package can expose more than one launchable activity, so the activity
// class is part of the identity; without it LazyColumn throws on duplicate keys.
private fun drawerItemKey(item: AppModel): String =
    when (item) {
        is AppModel.App -> "app:${item.appPackage}:${item.activityClassName ?: ""}:${item.user}"
        is AppModel.PinnedShortcut -> "shortcut:${item.identity}"
        is AppModel.PrivateSpaceHeader -> "private-space-header"
    }
