package app.minlauncher.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.minlauncher.R

@Composable
internal fun ThemeRow(
    appTheme: Int,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
    systemThemeOption: SystemThemeOption,
) {
    PickerRow(
        spec =
            PickerRowSpec(
                content =
                    SettingRowContent(
                        label = stringResource(R.string.theme_mode),
                        value = themeLabel(appTheme),
                        tag = "settings.appTheme",
                    ),
                target = SettingsPicker.APP_THEME,
                onLongClick = {
                    systemThemeOption.onReveal()
                    controller.open(SettingsPicker.APP_THEME)
                },
            ),
        controller = controller,
        palette = palette,
    ) {
        ThemePickerOptions(systemThemeOption.revealed, palette, callbacks, controller)
    }
}

@Composable
private fun ThemePickerOptions(
    revealed: Boolean,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        if (revealed) {
            PlainPickerOption(
                text = stringResource(R.string.system_default),
                tag = "settings.theme.system",
                style = palette.text.smallBold,
                onClick = {
                    controller.close()
                    callbacks.onSelectTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                },
                modifier = Modifier.padding(end = 24.dp, top = 6.dp, bottom = 6.dp),
            )
        }
        PlainPickerOption(
            text = stringResource(R.string.light),
            tag = "settings.theme.light",
            style = palette.text.smallBold,
            onClick = {
                controller.close()
                callbacks.onSelectTheme(AppCompatDelegate.MODE_NIGHT_NO)
            },
            modifier = Modifier.padding(end = 24.dp, top = 6.dp, bottom = 6.dp),
        )
        PlainPickerOption(
            text = stringResource(R.string.dark),
            tag = "settings.theme.dark",
            style = palette.text.smallBold,
            onClick = {
                controller.close()
                callbacks.onSelectTheme(AppCompatDelegate.MODE_NIGHT_YES)
            },
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
        )
    }
}
