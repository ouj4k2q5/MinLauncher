package app.minlauncher.ui.drawer

import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import app.minlauncher.R
import app.minlauncher.ui.theme.requireString
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class DrawerFontFamilyTest {
    @Test
    fun `the drawer resolves mainFontFamily and follows the bold overlay`() {
        assertEquals("sans-serif-light", resolveFontName(bold = false))
        assertEquals("sans-serif", resolveFontName(bold = true))
    }

    /** Mirrors DrawerEntry's resolution, including MainActivity's overlay. */
    private fun resolveFontName(bold: Boolean): String {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.AppTheme).apply {
                if (bold) theme.applyStyle(R.style.BoldFontOverlay, true)
            }
        val typedArray = context.obtainStyledAttributes(R.styleable.DrawerColors)
        return try {
            typedArray.requireString(R.styleable.DrawerColors_mainFontFamily, "mainFontFamily")
        } finally {
            typedArray.recycle()
        }
    }
}
