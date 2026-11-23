package app.minlauncher.ui.settings

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.minlauncher.R
import app.minlauncher.helper.isEinkDisplay
import app.minlauncher.helper.isSystemAnimationsDisabled

private val DialogShape = RoundedCornerShape(24.dp)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    colors: SettingsColors,
    callbacks: SettingsCallbacks,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val palette = settingsPalette(colors)

    var picker by rememberSaveable { mutableStateOf(SettingsPicker.NONE) }
    var systemThemeRevealed by rememberSaveable { mutableStateOf(false) }

    val controller =
        SettingsPickerController(
            current = picker,
            open = { target ->
                // Any row tap while the text-size preview is open commits it,
                // including re-tapping the text-size row (old onClick closed the
                // +/- layout and applied the scale before reopening it).
                if (picker == SettingsPicker.TEXT_SIZE) callbacks.onFinalizeTextSize()
                picker = target
            },
            close = {
                if (picker == SettingsPicker.TEXT_SIZE) callbacks.onFinalizeTextSize()
                picker = SettingsPicker.NONE
            },
        )
    val layout =
        SettingsLayout(
            isLandscape = isLandscape,
            systemThemeOption =
                SystemThemeOption(
                    revealed = systemThemeRevealed || isLandscape,
                    onReveal = { systemThemeRevealed = true },
                ),
        )

    // Finalizing on view destruction mirrors the old onDestroyView() call.
    DisposableEffect(callbacks) {
        onDispose { callbacks.onFinalizeTextSize() }
    }

    Box(Modifier.fillMaxSize()) {
        SettingsContent(state, palette, callbacks, controller, layout)
        if (state.accessibilityOverlayVisible) {
            AccessibilityOverlay(state, palette, callbacks)
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
    controller: SettingsPickerController,
    layout: SettingsLayout,
) {
    val context = LocalContext.current
    // Deliberately stricter than Utils.animateAlpha: e-ink OR system animations
    // disabled changes the disclosure scrim instantly; everyone else animates
    // over 200ms with linear easing.
    val instantAlpha = context.isEinkDisplay() || context.isSystemAnimationsDisabled()
    val scrollAlpha by animateFloatAsState(
        targetValue = if (state.accessibilityOverlayVisible) 0.5f else 1f,
        animationSpec = if (instantAlpha) snap() else tween(durationMillis = 200, easing = LinearEasing),
        label = "settingsScrollAlpha",
    )
    val verticalPadding = if (layout.isLandscape) 56.dp else 64.dp
    val horizontalPadding = if (layout.isLandscape) 100.dp else 0.dp

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .alpha(scrollAlpha)
                .pointerInput(controller.current) {
                    // Taps on rows are consumed by their own clickables; a tap on
                    // the background closes every expanded picker.
                    detectTapGestures { controller.close() }
                }.verticalScroll(rememberScrollState())
                .padding(top = 8.dp + verticalPadding, bottom = 8.dp + verticalPadding)
                .padding(horizontal = horizontalPadding),
    ) {
        AppInfoCard(state, palette, callbacks, controller)
        Spacer(Modifier.height(12.dp))
        HomeScreenCard(state, palette, callbacks, controller)
        Spacer(Modifier.height(12.dp))
        AppearanceCard(state, palette, callbacks, controller, layout.systemThemeOption)
        Spacer(Modifier.height(12.dp))
        GesturesCard(state, palette, callbacks, controller)
    }
}

@Composable
private fun AccessibilityOverlay(
    state: SettingsUiState,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(palette.colors.dialogShade))
                .clickable(interactionSource = interactionSource, indication = null) {},
    ) {
        AccessibilityPanel(state, palette, callbacks)
    }
}

@Composable
private fun BoxScope.AccessibilityPanel(
    state: SettingsUiState,
    palette: SettingsPalette,
    callbacks: SettingsCallbacks,
) {
    Column(
        modifier =
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(20.dp)
                .shadow(8.dp, DialogShape)
                .clip(DialogShape)
                .background(Color(palette.colors.inverseTrans90))
                .border(0.8.dp, Color(palette.colors.colorTrans50), DialogShape)
                .padding(20.dp),
    ) {
        Text(
            text = stringResource(R.string.gestures),
            style = palette.text.large,
            color = Color(palette.colors.primaryColor),
        )
        Text(
            text = stringResource(R.string.accessibility_disclosure),
            style = palette.text.small,
            color = Color(palette.colors.primaryColor),
            modifier = Modifier.padding(top = 16.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = stringResource(R.string.close),
                style = palette.text.smallBold,
                modifier =
                    Modifier
                        .padding(end = 20.dp, top = 4.dp, bottom = 4.dp)
                        .testTag("settings.accessibility.close")
                        .clickable(onClick = callbacks.onCloseAccessibility),
            )
            Text(
                text = stringResource(if (state.accessibilityServiceEnabled) R.string.disable else R.string.enable),
                style = palette.text.smallBold,
                modifier =
                    Modifier
                        .padding(end = 4.dp, top = 4.dp, bottom = 4.dp)
                        .testTag("settings.accessibility.action")
                        .clickable(onClick = callbacks.onOpenAccessibilitySettings),
            )
        }
    }
}
