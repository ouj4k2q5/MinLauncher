package app.minlauncher.helper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListFilterTest {
    private val selfPackage = "app.minlauncher"
    private val userKey = "UserHandle{0}"

    @Test
    fun `self package is excluded even when both include flags are true`() {
        assertFalse(
            shouldIncludeApp(
                selfPackage = selfPackage,
                appPackage = selfPackage,
                userKey = userKey,
                hiddenApps = emptySet(),
                includeRegularApps = true,
                includeHiddenApps = true,
            ),
        )
    }

    @Test
    fun `self package is excluded even when listed as hidden`() {
        assertFalse(
            shouldIncludeApp(
                selfPackage = selfPackage,
                appPackage = selfPackage,
                userKey = userKey,
                hiddenApps = setOf("$selfPackage|$userKey"),
                includeRegularApps = true,
                includeHiddenApps = true,
            ),
        )
    }

    @Test
    fun `hidden app is excluded when includeHiddenApps is false`() {
        assertFalse(
            shouldIncludeApp(
                selfPackage = selfPackage,
                appPackage = "org.example.app",
                userKey = userKey,
                hiddenApps = setOf("org.example.app|$userKey"),
                includeRegularApps = true,
                includeHiddenApps = false,
            ),
        )
    }

    @Test
    fun `hidden app is included when includeHiddenApps is true`() {
        assertTrue(
            shouldIncludeApp(
                selfPackage = selfPackage,
                appPackage = "org.example.app",
                userKey = userKey,
                hiddenApps = setOf("org.example.app|$userKey"),
                includeRegularApps = false,
                includeHiddenApps = true,
            ),
        )
    }

    @Test
    fun `regular app is included when includeRegularApps is true`() {
        assertTrue(
            shouldIncludeApp(
                selfPackage = selfPackage,
                appPackage = "org.example.app",
                userKey = userKey,
                hiddenApps = emptySet(),
                includeRegularApps = true,
                includeHiddenApps = false,
            ),
        )
    }

    @Test
    fun `regular app is excluded when includeRegularApps is false`() {
        assertFalse(
            shouldIncludeApp(
                selfPackage = selfPackage,
                appPackage = "org.example.app",
                userKey = userKey,
                hiddenApps = emptySet(),
                includeRegularApps = false,
                includeHiddenApps = false,
            ),
        )
    }

    @Test
    fun `hidden key is scoped to the user profile`() {
        val hiddenUnderOtherUser = setOf("org.example.app|UserHandle{10}")
        assertTrue(
            shouldIncludeApp(
                selfPackage = selfPackage,
                appPackage = "org.example.app",
                userKey = userKey,
                hiddenApps = hiddenUnderOtherUser,
                includeRegularApps = true,
                includeHiddenApps = false,
            ),
        )
    }
}
