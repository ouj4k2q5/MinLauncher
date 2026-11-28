package app.minlauncher.ui.drawer

import app.minlauncher.data.AppModel
import app.minlauncher.ui.fakeApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Regression protection for the old adapter's matching rules, extracted into
// filterApps verbatim.
// Pin the Robolectric SDK to targetSdk (36).
@Config(sdk = [36])
@RunWith(RobolectricTestRunner::class)
class DrawerFilterTest {
    private val apps =
        fakeApps("Browser", "Café", "Chat App", "Maps", "Music") +
            listOf(AppModel.PrivateSpaceHeader(isLocked = true))

    @Test
    fun `a blank query shows the whole list`() {
        assertEquals(apps, filterApps(apps, ""))
        assertEquals(apps, filterApps(apps, "   "))
    }

    @Test
    fun `plain matching is case-insensitive substring containment`() {
        val filtered = filterApps(apps, "MA")
        assertEquals(listOf("Maps"), filtered.map { it.appLabel })
    }

    @Test
    fun `diacritics are normalized away`() {
        val filtered = filterApps(apps, "cafe")
        assertEquals(listOf("Café"), filtered.map { it.appLabel })
    }

    @Test
    fun `separators are normalized away`() {
        val filtered = filterApps(apps, "chatapp")
        assertEquals(listOf("Chat App"), filtered.map { it.appLabel })
    }

    @Test
    fun `a non-matching query empties the list`() {
        assertTrue(filterApps(apps, "zzz").isEmpty())
    }

    @Test
    fun `the private space header never matches a query`() {
        val filtered = filterApps(apps, "private")
        assertTrue(filtered.none { it is AppModel.PrivateSpaceHeader })
    }

    @Test
    fun `a leading bang marks a web search, never an app filter`() {
        assertTrue(isBangSearch("!query"))
        assertFalse(isBangSearch("query"))
        // "!"-prefixed queries never match an app label, so the list empties.
        assertTrue(filterApps(apps, "!maps").none { it.appLabel == "Maps" })
    }

    @Test
    fun `auto-launch never fires for blank or leading-space queries`() {
        // A one-app drawer must not launch on open (blank query)...
        assertFalse(shouldAutoLaunch(""))
        // ...and a leading space is an explicit opt-out.
        assertFalse(shouldAutoLaunch(" maps"))
        assertTrue(shouldAutoLaunch("maps"))
    }
}
