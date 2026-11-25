package app.minlauncher.helper

import android.content.Context
import android.content.Intent

/**
 * The UI process's request to lock the screen.
 *
 * [MyAccessibilityService] runs in the separate `:serviceProcess`, so it cannot
 * be bound or called directly from the home screen. Instead the UI sends this
 * same-app broadcast, and the service receives it through a receiver it
 * registers while connected. The caller checks lock mode first, so this is only
 * sent when the double-tap-to-lock gesture is enabled.
 */
object LockRequest {
    const val ACTION_LOCK_SCREEN = "app.minlauncher.action.LOCK_SCREEN"

    /**
     * Sends the lock request. The explicit package keeps it inside the app and
     * satisfies the implicit-broadcast restrictions.
     */
    fun send(context: Context) {
        context.sendBroadcast(Intent(ACTION_LOCK_SCREEN).setPackage(context.packageName))
    }
}
