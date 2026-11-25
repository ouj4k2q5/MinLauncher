package app.minlauncher.helper

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import app.minlauncher.data.Prefs

/**
 * Deliberately dumb bridge for the double-tap-to-lock gesture: the UI process
 * sends [LockRequest.ACTION_LOCK_SCREEN] as a same-app explicit broadcast, and
 * this service — which runs in `:serviceProcess` and cannot be called directly —
 * performs the global lock action while it is connected.
 */
class MyAccessibilityService : AccessibilityService() {
    private var lockReceiverRegistered = false

    private val lockReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action == LockRequest.ACTION_LOCK_SCREEN) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
        }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_STICKY

    // Public so the Robolectric service test can drive the registration hook
    // directly; the framework itself calls it the same way.
    public override fun onServiceConnected() {
        Prefs(applicationContext).lockModeOn = true
        if (!lockReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                lockReceiver,
                IntentFilter(LockRequest.ACTION_LOCK_SCREEN),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            lockReceiverRegistered = true
        }
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        if (lockReceiverRegistered) {
            unregisterReceiver(lockReceiver)
            lockReceiverRegistered = false
        }
        super.onDestroy()
    }
}
