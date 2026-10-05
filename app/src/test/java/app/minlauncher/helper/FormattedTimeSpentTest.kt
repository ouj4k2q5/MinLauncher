package app.minlauncher.helper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class FormattedTimeSpentTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `zero time is shown as 0m`() {
        assertEquals("0m", context.formattedTimeSpent(0L))
    }

    @Test
    fun `under one minute is shown as less than 1m`() {
        assertEquals("<1m", context.formattedTimeSpent(59_999L))
    }

    @Test
    fun `minutes only are shown with the min string resource`() {
        // Robolectric resolves time_spent_min without the <b> markup it wraps the value in.
        assertEquals("5m", context.formattedTimeSpent(5 * 60 * 1000L))
    }

    @Test
    fun `hours and minutes are shown with the hour string resource`() {
        assertEquals("2h 5m", context.formattedTimeSpent((2 * 60 + 5) * 60 * 1000L))
    }
}
