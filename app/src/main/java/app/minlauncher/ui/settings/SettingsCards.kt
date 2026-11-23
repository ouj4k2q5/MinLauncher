package app.minlauncher.ui.settings

import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.minlauncher.R
import app.minlauncher.data.Constants

@Composable
internal fun AppInfoCard(
    state: SettingsUiState,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
) {
    SettingsCard(Modifier, palette) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = palette.text.large,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .testTag("settings.hiddenApps")
                        .clickable {
                            controller.close()
                            callbacks.onOpenHiddenApps()
                        },
            )
            Box(
                modifier =
                    Modifier
                        .padding(4.dp)
                        .testTag("settings.appInfo")
                        .clickable {
                            controller.close()
                            callbacks.onOpenAppInfo()
                        },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.app_info),
                    tint = Color(palette.colors.primaryColor),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            text =
                stringResource(
                    if (state.isDefaultLauncher) {
                        R.string.change_default_launcher
                    } else {
                        R.string.set_as_default_launcher
                    },
                ),
            style = palette.text.small,
            modifier =
                Modifier
                    .padding(start = 8.dp, top = 18.dp, bottom = 8.dp)
                    .testTag("settings.setLauncher")
                    .clickable {
                        controller.close()
                        callbacks.onRequestResetLauncher()
                    },
        )
    }
}

@Composable
internal fun HomeScreenCard(
    state: SettingsUiState,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
) {
    SettingsCard(Modifier, palette) {
        SectionTitle(stringResource(R.string.home_screen), palette)
        AppsNumRow(state.homeAppsNum, palette, callbacks, controller, Modifier.padding(top = 12.dp))
        DateTimeRow(state.dateTimeVisibility, palette, callbacks, controller, Modifier.padding(top = 12.dp))
        AlignmentRow(state, palette, callbacks, controller, Modifier.padding(top = 12.dp))
        ActionRow(
            content =
                SettingRowContent(
                    label = stringResource(R.string.screen_time),
                    value = stringResource(if (state.screenTimeOn) R.string.on else R.string.off),
                    tag = "settings.screenTime",
                ),
            palette = palette,
            onClick = {
                controller.close()
                callbacks.onToggleScreenTime()
            },
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun AppsNumRow(
    value: Int,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
    modifier: Modifier = Modifier,
) {
    PickerRow(
        spec =
            PickerRowSpec(
                content =
                    SettingRowContent(
                        label = stringResource(R.string.apps_on_home_screen),
                        value = value.toString(),
                        tag = "settings.homeAppsNum",
                    ),
                target = SettingsPicker.APPS_NUM,
            ),
        controller = controller,
        palette = palette,
        modifier = modifier,
    ) {
        Row(Modifier.fillMaxSize()) {
            for (number in 0..8) {
                WeightedPickerOption(
                    text = number.toString(),
                    tag = "settings.appsNum.$number",
                    style = palette.text.small,
                    onClick = {
                        controller.close()
                        callbacks.onSelectHomeAppsNum(number)
                    },
                )
            }
        }
    }
}

@Composable
private fun DateTimeRow(
    visibility: Int,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
    modifier: Modifier = Modifier,
) {
    PickerRow(
        spec =
            PickerRowSpec(
                content =
                    SettingRowContent(
                        label = stringResource(R.string.show_date_time),
                        value = dateTimeLabel(visibility),
                        tag = "settings.dateTime",
                    ),
                target = SettingsPicker.DATE_TIME,
            ),
        controller = controller,
        palette = palette,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            PlainPickerOption(
                text = stringResource(R.string.on),
                tag = "settings.dateTime.on",
                style = palette.text.smallBold,
                onClick = {
                    controller.close()
                    callbacks.onSelectDateTime(Constants.DateTime.ON)
                },
                modifier = Modifier.padding(end = 24.dp, top = 6.dp, bottom = 6.dp),
            )
            PlainPickerOption(
                text = stringResource(R.string.off),
                tag = "settings.dateTime.off",
                style = palette.text.smallBold,
                onClick = {
                    controller.close()
                    callbacks.onSelectDateTime(Constants.DateTime.OFF)
                },
                modifier = Modifier.padding(start = 4.dp, end = 24.dp, top = 6.dp, bottom = 6.dp),
            )
            PlainPickerOption(
                text = stringResource(R.string.date_only),
                tag = "settings.dateTime.dateOnly",
                style = palette.text.smallBold,
                onClick = {
                    controller.close()
                    callbacks.onSelectDateTime(Constants.DateTime.DATE_ONLY)
                },
                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun AlignmentRow(
    state: SettingsUiState,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
    modifier: Modifier = Modifier,
) {
    PickerRow(
        spec =
            PickerRowSpec(
                content =
                    SettingRowContent(
                        label = stringResource(R.string.home_layout_alignment),
                        value = alignmentLabel(state.homeAlignment),
                        tag = "settings.alignment",
                    ),
                target = SettingsPicker.ALIGNMENT,
                onLongClick = callbacks.onLongPressAlignment,
            ),
        controller = controller,
        palette = palette,
        modifier = modifier,
    ) {
        AlignmentPicker(state.homeBottomAlignment, palette, callbacks, controller)
    }
}

@Composable
private fun AlignmentPicker(
    bottom: Boolean,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
) {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainPickerOption(
            text = stringResource(if (bottom) R.string.bottom_on else R.string.bottom_off),
            tag = "settings.alignment.bottom",
            style = palette.text.smallBold,
            onClick = callbacks.onToggleBottomAlignment,
            modifier = Modifier.padding(end = 16.dp, top = 6.dp, bottom = 6.dp),
        )
        Box(
            Modifier
                .padding(end = 16.dp)
                .width(0.5.dp)
                .height(20.dp)
                .background(Color(palette.colors.colorTrans50)),
        )
        PlainPickerOption(
            text = stringResource(R.string.left),
            tag = "settings.alignment.left",
            style = palette.text.smallBold,
            onClick = {
                controller.close()
                callbacks.onSelectAlignment(Gravity.START)
            },
            modifier = Modifier.padding(end = 16.dp, top = 6.dp, bottom = 6.dp),
        )
        PlainPickerOption(
            text = stringResource(R.string.center),
            tag = "settings.alignment.center",
            style = palette.text.smallBold,
            onClick = {
                controller.close()
                callbacks.onSelectAlignment(Gravity.CENTER)
            },
            modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        )
        PlainPickerOption(
            text = stringResource(R.string.right),
            tag = "settings.alignment.right",
            style = palette.text.smallBold,
            onClick = {
                controller.close()
                callbacks.onSelectAlignment(Gravity.END)
            },
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
        )
    }
}

@Composable
internal fun AppearanceCard(
    state: SettingsUiState,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
    systemThemeOption: SystemThemeOption,
) {
    SettingsCard(Modifier, palette) {
        SectionTitle(stringResource(R.string.appearance), palette)
        AppearanceToggleRows(state, palette, callbacks, controller)
        if (state.showThemeRow) {
            Spacer(Modifier.height(12.dp))
            ThemeRow(
                appTheme = state.appTheme,
                palette = palette,
                callbacks = callbacks,
                controller = controller,
                systemThemeOption = systemThemeOption,
            )
        }
        ActionRow(
            content =
                SettingRowContent(
                    label = stringResource(R.string.bold_font),
                    value = stringResource(if (state.boldFont) R.string.on else R.string.off),
                    tag = "settings.boldFont",
                ),
            palette = palette,
            onClick = {
                controller.close()
                callbacks.onToggleBoldFont()
            },
            modifier = Modifier.padding(top = 12.dp),
        )
        TextSizeRow(state.textSizeLabel, palette, callbacks, controller, Modifier.padding(top = 12.dp, bottom = 4.dp))
    }
}

@Composable
private fun AppearanceToggleRows(
    state: SettingsUiState,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
) {
    ActionRow(
        content =
            SettingRowContent(
                label = stringResource(R.string.auto_show_keyboard),
                value = stringResource(if (state.autoShowKeyboard) R.string.on else R.string.off),
                tag = "settings.autoShowKeyboard",
            ),
        palette = palette,
        onClick = {
            controller.close()
            callbacks.onToggleAutoShowKeyboard()
        },
        modifier = Modifier.padding(top = 12.dp),
    )
    ActionRow(
        content =
            SettingRowContent(
                label = stringResource(R.string.solid_wallpaper),
                value = stringResource(if (state.solidWallpaper) R.string.on else R.string.off),
                tag = "settings.wallpaper",
            ),
        palette = palette,
        onClick = {
            controller.close()
            callbacks.onToggleSolidWallpaper()
        },
        modifier = Modifier.padding(top = 12.dp),
        style = ActionRowStyle(onLongClick = callbacks.onLongPressSolidWallpaper),
    )
    ActionRow(
        content =
            SettingRowContent(
                label = stringResource(R.string.notification_bar),
                value = stringResource(if (state.statusBarOn) R.string.on else R.string.off),
                tag = "settings.statusBar",
            ),
        palette = palette,
        onClick = {
            controller.close()
            callbacks.onToggleStatusBar()
        },
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun TextSizeRow(
    label: String,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
    modifier: Modifier = Modifier,
) {
    PickerRow(
        spec =
            PickerRowSpec(
                content =
                    SettingRowContent(
                        label = stringResource(R.string.text_size),
                        value = label,
                        tag = "settings.textSize",
                    ),
                target = SettingsPicker.TEXT_SIZE,
            ),
        controller = controller,
        palette = palette,
        modifier = modifier,
    ) {
        Row(Modifier.fillMaxSize()) {
            WeightedPickerOption(
                text = "\u2212",
                tag = "settings.textSize.minus",
                style = palette.text.smallBold,
                onClick = { callbacks.onAdjustTextSize(-0.1f) },
            )
            Text(
                text = label,
                style = palette.text.smallBold,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .testTag("settings.textSize.current"),
            )
            WeightedPickerOption(
                text = "+",
                tag = "settings.textSize.plus",
                style = palette.text.smallBold,
                onClick = { callbacks.onAdjustTextSize(0.1f) },
            )
        }
    }
}

@Composable
internal fun GesturesCard(
    state: SettingsUiState,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
) {
    SettingsCard(Modifier, palette) {
        SectionTitle(stringResource(R.string.gestures), palette)
        ActionRow(
            content =
                SettingRowContent(
                    label = stringResource(R.string.swipe_left_app),
                    value = state.swipeLeftAppName,
                    tag = "settings.swipeLeft",
                ),
            palette = palette,
            onClick = {
                controller.close()
                callbacks.onSwipeAppClick(true)
            },
            modifier = Modifier.padding(top = 12.dp),
            style =
                ActionRowStyle(
                    valueColor = swipeValueColor(state.swipeLeftEnabled, palette),
                    onLongClick = { callbacks.onSwipeAppLongPress(true) },
                ),
        )
        ActionRow(
            content =
                SettingRowContent(
                    label = stringResource(R.string.swipe_right_app),
                    value = state.swipeRightAppName,
                    tag = "settings.swipeRight",
                ),
            palette = palette,
            onClick = {
                controller.close()
                callbacks.onSwipeAppClick(false)
            },
            modifier = Modifier.padding(top = 12.dp),
            style =
                ActionRowStyle(
                    valueColor = swipeValueColor(state.swipeRightEnabled, palette),
                    onLongClick = { callbacks.onSwipeAppLongPress(false) },
                ),
        )
        ActionRow(
            content =
                SettingRowContent(
                    label = stringResource(R.string.double_tap_to_lock_screen),
                    value = stringResource(if (state.lockModeOn) R.string.on else R.string.off),
                    tag = "settings.toggleLock",
                ),
            palette = palette,
            onClick = {
                controller.close()
                callbacks.onToggleLockMode()
            },
            modifier = Modifier.padding(start = 8.dp, top = 12.dp),
            style = ActionRowStyle(onLongClick = callbacks.onLongPressLockMode),
        )
    }
}
