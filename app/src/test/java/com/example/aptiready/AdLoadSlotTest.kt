package com.example.aptiready

import com.example.aptiready.ui.ads.AdLoadSlot
import org.junit.Assert.*
import org.junit.Test

class AdLoadSlotTest {
    private var time = 0L
    private val disposed = mutableListOf<String>()
    private fun slot() = AdLoadSlot<String>({ time }) { disposed += it }

    @Test fun duplicateLoadsAndStaleCallbacksCannotReplaceAnAd() {
        val slot = slot()
        val old = slot.begin("unit")!!
        assertNull(slot.begin("unit"))
        slot.clear()
        val fresh = slot.begin("unit")!!
        assertFalse(slot.accept(old, "old"))
        assertTrue(slot.accept(fresh, "fresh"))
        assertEquals(listOf("old"), disposed)
        assertEquals("fresh", slot.take("unit"))
        assertNull(slot.take("unit"))
    }

    @Test fun expiredNativeAdIsDestroyedAndReloaded() {
        val slot = slot()
        slot.accept(slot.begin("unit")!!, "expired")
        time = 3_600_000L
        assertNull(slot.cached("unit"))
        assertEquals(listOf("expired"), disposed)
        assertNotNull(slot.begin("unit"))
    }

    @Test fun hungRequestBacksOffAndLateSuccessIsDestroyed() {
        val slot = slot()
        val old = slot.begin("unit")!!
        time = 60_000L
        assertNull(slot.begin("unit"))
        assertFalse(slot.accept(old, "late"))
        time = 120_000L
        assertNotNull(slot.begin("unit"))
        assertEquals(listOf("late"), disposed)
    }

    @Test fun temporaryFailuresRecoverAfterManyAttempts() {
        val slot = slot()
        repeat(10) {
            val token = slot.begin("unit")!!
            assertTrue(slot.fail(token, true))
            assertNull(slot.begin("unit"))
            time += 300_000L
        }
        assertTrue(slot.accept(slot.begin("unit")!!, "recovered"))
        assertEquals("recovered", slot.cached("unit"))
    }

    @Test fun configurationChangeDiscardsCacheAndResetsPermanentFailure() {
        val slot = slot()
        slot.accept(slot.begin("unit:0")!!, "old-config")
        val token = slot.begin("unit:1")!!
        assertEquals(listOf("old-config"), disposed)
        assertTrue(slot.fail(token, false))
        time += 999_999L
        assertNull(slot.begin("unit:1"))
        assertNotNull(slot.begin("unit:2"))
    }
}
