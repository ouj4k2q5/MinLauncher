package app.minlauncher.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class PrefsDefaultTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefs = Prefs(context)

    @Test
    fun `default theme is dark`() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, prefs.appTheme)
    }

    @Test
    fun `first-run state is on first open`() {
        assertTrue(prefs.firstOpen)
        assertTrue(prefs.firstSettingsOpen)
    }
}
