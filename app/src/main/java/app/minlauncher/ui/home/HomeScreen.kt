package app.minlauncher.ui.home

import android.content.res.Configuration
import android.view.Gravity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.minlauncher.R
import app.minlauncher.data.Constants
import java.time.Clock

/**
 * The home screen: the layout of the old fragment_home.xml (portrait and
 * landscape variants merged — Compose re-lays out on configuration changes),
 * with the background gesture detector on the root box and each interactive
 * child consuming its own downs so the background skips those gestures.
 */
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    colors: HomeColors,
    clock: Clock,
    callbacks: HomeCallbacks,
) {
    val palette = homePalette(colors)
    val lockActionLabel = stringResource(R.string.lock_screen_action)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Box(
        Modifier
            .fillMaxSize()
            .homeBackgroundGestures(callbacks)
            // TalkBack has no pointer gestures, so the old invisible 1dp lock
            // view is replaced by this click action on the background. The
            // callback is the same one the touch double tap uses (and is
            // gated on lock mode in HomeFragment).
            .testTag("home.root")
            .semantics {
                onClick(label = lockActionLabel) {
                    callbacks.onBackgroundDoubleTap()
                    true
                }
            },
    ) {
        if (state.dateTimeVisibility != Constants.DateTime.OFF) {
            HomeDateTime(state, palette, clock, callbacks, isLandscape)
        }
        if (state.screenTime != null) {
            HomeScreenTime(state, palette, callbacks, isLandscape)
        }
        HomeAppSlots(state, palette, callbacks, isLandscape)
        if (state.showFirstRunTips) {
            HomeFirstRunTips(palette, isLandscape)
        }
        if (state.showSetDefaultLauncher) {
            HomeSetDefaultLauncher(palette, callbacks, isLandscape)
        }
    }
}

/** The clock and date column at the top, like dateTimeLayout. */
@Composable
private fun BoxScope.HomeDateTime(
    state: HomeUiState,
    palette: HomePalette,
    clock: Clock,
    callbacks: HomeCallbacks,
    isLandscape: Boolean,
) {
    Column(
        modifier =
            Modifier
                .align(
                    when {
                        isLandscape -> Alignment.TopCenter
                        state.homeAppAlignment == Gravity.END -> Alignment.TopEnd
                        else -> Alignment.TopStart
                    },
                ).then(
                    if (isLandscape) {
                        Modifier.padding(top = 48.dp)
                    } else {
                        Modifier.padding(top = 56.dp, start = 24.dp, end = 24.dp)
                    },
                ).testTag("home.dateTime"),
    ) {
        // One refresh source for both lines (one broadcast receiver, one
        // recomposition per minute tick).
        val now = rememberCurrentInstant(clock)
        if (Constants.DateTime.isTimeVisible(state.dateTimeVisibility)) {
            val interactionSource = remember { MutableInteractionSource() }
            Text(
                text = homeClockText(now),
                style = palette.clock,
                modifier =
                    Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { callbacks.onClockTap() }
                        .testTag("home.clock"),
            )
        }
        if (Constants.DateTime.isDateVisible(state.dateTimeVisibility)) {
            val interactionSource = remember { MutableInteractionSource() }
            Text(
                text = homeDateText(now, includeBattery = !state.showStatusBar),
                style = palette.date,
                modifier =
                    Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { callbacks.onDateTap() }
                        .padding(horizontal = 3.dp),
            )
        }
    }
}

/**
 * The screen-time line at the top corner opposite the home apps, with the
 * orientation- and date-only-dependent margins populateScreenTime computed.
 */
@Composable
private fun BoxScope.HomeScreenTime(
    state: HomeUiState,
    palette: HomePalette,
    callbacks: HomeCallbacks,
    isLandscape: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val dateOnly = state.dateTimeVisibility == Constants.DateTime.DATE_ONLY
    val marginTop =
        if (dateOnly) {
            if (isLandscape) {
                36.dp
            } else {
                45.dp
            }
        } else if (isLandscape) {
            56.dp
        } else {
            72.dp
        }
    val horizontalMargin = if (isLandscape) 64.dp else 10.dp
    Text(
        text = state.screenTime.orEmpty(),
        style = palette.small,
        modifier =
            Modifier
                .align(if (state.homeAppAlignment == Gravity.END) Alignment.TopStart else Alignment.TopEnd)
                .padding(top = marginTop, start = horizontalMargin, end = horizontalMargin)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) { callbacks.onScreenTimeTap() }
                .padding(10.dp),
    )
}

/** The centered (or bottom-aligned) column of home-app slots, like homeAppsLayout. */
@Composable
private fun BoxScope.HomeAppSlots(
    state: HomeUiState,
    palette: HomePalette,
    callbacks: HomeCallbacks,
    isLandscape: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .then(
                    if (isLandscape) {
                        Modifier
                            .padding(horizontal = 74.dp)
                            .padding(vertical = dimensionResource(R.dimen.home_app_padding_vertical))
                    } else {
                        Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 112.dp, bottom = 48.dp)
                    },
                ),
        verticalArrangement = if (state.homeBottomAlignment) Arrangement.Bottom else Arrangement.Center,
        horizontalAlignment = if (state.homeAppAlignment == Gravity.END) Alignment.End else Alignment.Start,
    ) {
        state.slots.forEach { slot -> HomeSlotText(slot, palette, callbacks) }
    }
}

/**
 * One home-app slot: the custom gesture detector (tap, long press, fling)
 * plus semantics for TalkBack, since the detector itself exposes none. The
 * pressed flag drives the text color like the old isPressed handling, and
 * an unset slot shows the "App" hint at the old hint color.
 *
 * The old TextViews were focusable, so a d-pad/keyboard could focus a slot
 * and launch it with center/Enter; [focusable] plus [onKeyEvent] restore
 * that. Key events fire the tap only — there is no key long press, unlike
 * the touch long-press above (View.performClick has the same asymmetry).
 */
@Composable
private fun HomeSlotText(
    slot: HomeSlotState,
    palette: HomePalette,
    callbacks: HomeCallbacks,
) {
    var pressed by remember(slot.index) { mutableStateOf(false) }
    val unset = slot.label.isEmpty()
    Text(
        text = if (unset) stringResource(R.string.app) else slot.label,
        style =
            when {
                unset -> palette.slotHint
                pressed -> palette.slotPressed
                else -> palette.slot
            },
        modifier =
            Modifier
                .homeSlotGestures(slot.index, callbacks) { pressed = it }
                .padding(vertical = dimensionResource(R.dimen.home_app_padding_vertical))
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key.isSlotActivationKey()) {
                        callbacks.onSlotTap(slot.index)
                        true
                    } else {
                        false
                    }
                }.semantics {
                    onClick {
                        callbacks.onSlotTap(slot.index)
                        true
                    }
                    onLongClick {
                        callbacks.onSlotLongPress(slot.index)
                        true
                    }
                },
    )
}

/** The d-pad center and Enter keys, which a View treated as a click. */
private fun Key.isSlotActivationKey(): Boolean =
    this == Key.DirectionCenter || this == Key.Enter || this == Key.NumPadEnter

/** The first-run hint at the bottom start (portrait) or bottom center (landscape). */
@Composable
private fun BoxScope.HomeFirstRunTips(
    palette: HomePalette,
    isLandscape: Boolean,
) {
    Text(
        text = stringResource(R.string.swipe_up_for_apps),
        style = palette.small,
        modifier =
            Modifier
                .align(if (isLandscape) Alignment.BottomCenter else Alignment.BottomStart)
                .then(
                    if (isLandscape) {
                        Modifier.padding(start = 8.dp, end = 8.dp, bottom = 56.dp)
                    } else {
                        Modifier.padding(start = 24.dp, end = 8.dp, bottom = 56.dp)
                    },
                ),
    )
}

/**
 * The underlined "Set as default launcher" line at the bottom. The old string
 * carried the underline as literal <u> tags; the tag-free string plus this
 * TextStyle renders it as a real underline.
 */
@Composable
private fun BoxScope.HomeSetDefaultLauncher(
    palette: HomePalette,
    callbacks: HomeCallbacks,
    isLandscape: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = stringResource(R.string.set_as_default_launcher),
        style = palette.mediumUnderline,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .then(
                    if (isLandscape) {
                        Modifier.padding(start = 24.dp, end = 24.dp)
                    } else {
                        Modifier.padding(start = 20.dp, end = 20.dp)
                    },
                ).padding(bottom = 56.dp)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = callbacks.onSetDefaultLauncherTap,
                    onLongClick = callbacks.onSetDefaultLauncherLongPress,
                ),
    )
}
