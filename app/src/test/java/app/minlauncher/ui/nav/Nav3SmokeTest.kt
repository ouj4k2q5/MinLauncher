package app.minlauncher.ui.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 0 spike: proves the Navigation 3 artifacts and the kotlinx-serialization
 * plugin resolve and compile against the pinned Kotlin 2.2.10 toolchain. Nothing
 * in the app navigates through Nav3 yet.
 */
@Serializable
data object SmokeKey : NavKey

class Nav3SmokeTest {
    // The instance check is constant by design: its job is to force Nav3's
    // NavKey to resolve and compile under the pinned Kotlin toolchain.
    @Test
    @Suppress("USELESS_IS_CHECK")
    fun smokeKeyIsNavKey() {
        assertTrue(SmokeKey is NavKey)
    }
}
