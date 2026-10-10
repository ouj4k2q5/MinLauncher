package app.minlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// SDK 36 requires Java 21 in Robolectric, so pin the highest SDK that runs on JDK 17.
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class AppSettingsStoreConcurrencyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val key = stringPreferencesKey("APP_NAME_1")

    // Distinct file name per test class so this store never collides with the
    // real LauncherApp's default-file store (see AppSettingsStoreMigrationTest).
    private fun newStore() = AppSettingsStore(context, dataStoreFileName = "concurrency-test")

    @Test
    fun `sequential same-key double write settles on the newest value`() {
        val store = newStore()
        store.warmUp()

        store.set(key, "older")
        store.set(key, "newer")

        // The optimistic snapshot reflects the second write immediately.
        assertEquals("newer", store[key])

        // The durable state must also settle on the newest value, never on the
        // older one: with FIFO edit ordering the older write is always
        // committed before the newer one.
        runBlocking {
            withTimeout(5_000) {
                while (store.dataStore.data.first()[key] != "newer") {
                    delay(10)
                }
            }
        }
    }

    @Test
    fun `multi-key write burst converges with the durable state`() {
        val store = newStore()
        store.warmUp()

        val keys = (1..20).map { stringPreferencesKey("BURST_KEY_$it") }
        keys.forEachIndexed { i, k -> store.set(k, "value-$i") }

        // Optimistic snapshot has every value immediately after the burst.
        keys.forEachIndexed { i, k -> assertEquals("value-$i", store[k]) }

        // Once the edit queue drains, the durable state has every value too.
        runBlocking {
            withTimeout(5_000) {
                while (keys.any { store.dataStore.data.first()[it] == null }) {
                    delay(10)
                }
            }
        }

        // The snapshot converges with the durable state: whether the collector
        // applies the final committed state or the optimistic value survives
        // suppression, both paths end at the same values. Poll in case the
        // collector is still draining an older emission.
        runBlocking {
            withTimeout(5_000) {
                while (keys.any { store.dataStore.data.first()[it] != store[it] }) {
                    delay(10)
                }
            }
            keys.forEachIndexed { i, k ->
                assertEquals("value-$i", store.dataStore.data.first()[k])
                assertEquals("value-$i", store[k])
            }
        }
    }

    @Test
    fun `concurrent writers from multiple threads converge on one written value`() {
        val store = newStore()
        store.warmUp()

        val threads = 4
        val writesPerThread = 50
        val written = Collections.synchronizedList(mutableListOf<String>())
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        repeat(threads) { t ->
            Thread {
                start.await()
                repeat(writesPerThread) { i ->
                    val value = "t$t-$i"
                    store.set(key, value)
                    written.add(value)
                }
                done.countDown()
            }.apply { start() }
        }
        start.countDown()
        assertTrue(done.await(10, TimeUnit.SECONDS))

        // After all writers finish, poll until the snapshot and the durable
        // state agree; the transient intermediate windows (collector applying
        // an older emission between two queue items) self-heal.
        runBlocking {
            withTimeout(5_000) {
                var durable: String?
                do {
                    durable = store.dataStore.data.first()[key]
                    if (durable == null || durable != store[key]) delay(10)
                } while (durable == null || durable != store[key])
            }
        }

        // The converged value must be one of the fully-written values: no
        // torn or fabricated state ever became visible.
        assertTrue(written.contains(store[key]))
    }
}
