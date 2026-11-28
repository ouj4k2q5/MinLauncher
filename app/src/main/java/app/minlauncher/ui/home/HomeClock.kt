package app.minlauncher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.minlauncher.R
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.Locale

/**
 * The instant the home screen renders from: [clock] (the test seam, the wall
 * clock in production), refreshed on the same system
 * broadcasts TextClock listened to — minute ticks, manual time changes and
 * timezone changes. A fixed test clock stays fixed across those broadcasts,
 * so screenshot baselines are day-independent.
 */
@Composable
internal fun rememberCurrentInstant(clock: Clock): Instant {
    var now by remember(clock) { mutableStateOf(Instant.now(clock)) }
    val context = LocalContext.current
    DisposableEffect(clock) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    now = Instant.now(clock)
                }
            }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return now
}

/**
 * The clock line, matching TextClock's default format: the locale's best
 * skeleton for "hm" ("Hm" on 24-hour devices), re-read on every refresh so a
 * settings change picks up on the next tick like TextClock did.
 */
@Composable
internal fun homeClockText(now: Instant): String {
    val context = LocalContext.current
    return remember(now, LocalConfiguration.current) { formatClockText(context, now) }
}

internal fun formatClockText(
    context: Context,
    now: Instant,
): String {
    val locale = Locale.getDefault()
    val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return SimpleDateFormat(pattern, locale).format(Date.from(now))
}

/**
 * The date line, with the battery level appended when the status bar is
 * hidden, and the ".," to "," tidy-up the old populateDateTime applied.
 */
@Composable
internal fun homeDateText(
    now: Instant,
    includeBattery: Boolean,
): String {
    val context = LocalContext.current
    return remember(now, LocalConfiguration.current, includeBattery) {
        formatDateText(context, now, includeBattery)
    }
}

internal fun formatDateText(
    context: Context,
    now: Instant,
    includeBattery: Boolean,
): String {
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEMMMd")
    var text = SimpleDateFormat(pattern, locale).format(Date.from(now))
    if (includeBattery) {
        val battery =
            (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (battery > 0) {
            text = context.getString(R.string.day_battery, text, battery)
        }
    }
    return text.replace(".,", ",")
}
