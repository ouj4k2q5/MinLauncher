package app.minlauncher.ui

import android.content.Intent
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.minlauncher.R
import app.minlauncher.data.Constants

/**
 * Renders the single message dialog a posted [Constants.Dialog] value asks for,
 * replacing the activity-wide `messageLayout` overlay for the Compose
 * [AppRoot]. The same four signals the legacy collector handled; a value with
 * no branch (the old `when` had no else) renders nothing.
 *
 * [onDismiss] is the only way out: it backs both the confirm button and
 * [AlertDialog]'s own dismiss request (outside tap / back), matching the old
 * overlay's single close affordance.
 */
@Composable
internal fun AppDialog(
    dialog: String,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        Constants.Dialog.ABOUT ->
            MessageDialog(R.string.app_name, R.string.welcome_to_minlauncher_settings, onDismiss)

        Constants.Dialog.HIDDEN ->
            MessageDialog(R.string.hidden_apps, R.string.hidden_apps_message, onDismiss)

        Constants.Dialog.KEYBOARD ->
            MessageDialog(R.string.app_name, R.string.keyboard_message, onDismiss)

        Constants.Dialog.DIGITAL_WELLBEING -> {
            val context = LocalContext.current
            MessageDialog(
                title = R.string.screen_time,
                message = R.string.app_usage_message,
                onDismiss = onDismiss,
                action = R.string.permission,
                // The legacy overlay opened usage access before hiding itself.
                onAction = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            )
        }
    }
}

@Composable
private fun MessageDialog(
    @StringRes title: Int,
    @StringRes message: Int,
    onDismiss: () -> Unit,
    @StringRes action: Int = R.string.okay,
    onAction: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onAction()
                    onDismiss()
                },
            ) {
                Text(stringResource(action))
            }
        },
    )
}
