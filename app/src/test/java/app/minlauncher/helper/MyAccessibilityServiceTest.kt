package app.minlauncher.helper

import android.accessibilityservice.AccessibilityService
import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import app.minlauncher.data.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Covers the service half of double-tap-to-lock: the UI side (when the broadcast
 * is sent) is covered by LockRequestTest, while this proves [MyAccessibilityService]
 * performs the global lock action on receipt and stops listening after onDestroy.
 *
 * Pin the Robolectric SDK to targetSdk (36).
 */
@Config(sdk = [36])
@RunWith(RobolectricTestRunner::class)
class MyAccessibilityServiceTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `a lock request broadcast performs the global lock action while connected`() {
        val service = connectedService()

        LockRequest.send(application)
        // Robolectric delivers broadcasts to registered receivers through the
        // paused main looper; drain it so the receiver runs synchronously.
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(
            listOf(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN),
            shadowOf(service).getGlobalActionsPerformed(),
        )
    }

    @Test
    fun `onDestroy unregisters the lock receiver`() {
        val service = connectedService()

        service.onDestroy()
        LockRequest.send(application)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(shadowOf(service).getGlobalActionsPerformed().isEmpty())
    }

    @Test
    fun `onServiceConnected marks lock mode on in prefs`() {
        connectedService()

        assertTrue(Prefs(application).lockModeOn)
    }

    /** setupService only runs onCreate; onServiceConnected is the real registration hook. */
    private fun connectedService(): MyAccessibilityService =
        Robolectric.setupService(MyAccessibilityService::class.java).apply { onServiceConnected() }
}
