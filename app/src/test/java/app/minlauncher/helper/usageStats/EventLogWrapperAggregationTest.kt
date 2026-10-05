package app.minlauncher.helper.usageStats

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
class EventLogWrapperAggregationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val wrapper = EventLogWrapper(context)

    @Test
    fun `overlapping intervals of the same package are not double counted`() {
        val stats =
            listOf(
                ComponentForegroundStat(0L, 10_000L, "app.minlauncher.test.app"),
                ComponentForegroundStat(5_000L, 12_000L, "app.minlauncher.test.app"),
                ComponentForegroundStat(10_000L, 15_000L, "app.minlauncher.test.app"),
            )

        val result = wrapper.aggregateForegroundStats(stats)

        assertEquals(1, result.size)
        assertEquals("app.minlauncher.test.app", result[0].applicationId)
        assertEquals(15_000L, result[0].timeUsed)
    }

    @Test
    fun `disjoint intervals and distinct packages are aggregated independently`() {
        val stats =
            listOf(
                ComponentForegroundStat(0L, 60_000L, "app.minlauncher.test.a"),
                ComponentForegroundStat(120_000L, 180_000L, "app.minlauncher.test.a"),
                ComponentForegroundStat(30_000L, 90_000L, "app.minlauncher.test.b"),
            )

        val result = wrapper.aggregateForegroundStats(stats)

        assertEquals(2, result.size)
        val byApp = result.associateBy { it.applicationId }
        assertEquals(120_000L, byApp.getValue("app.minlauncher.test.a").timeUsed)
        assertEquals(60_000L, byApp.getValue("app.minlauncher.test.b").timeUsed)
        assertEquals(180_000L, wrapper.aggregateSimpleUsageStats(result))
    }
}
