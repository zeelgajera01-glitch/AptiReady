package com.example.aptiready

import com.example.aptiready.ui.ads.AdRetryPolicy
import org.junit.Assert.*
import org.junit.Test

class AdRetryPolicyTest {
    @Test fun noFillUsesBackoffAndContinuesAtCappedDelay() {
        val retry = AdRetryPolicy()
        assertTrue(retry.canAttempt(1_000))
        retry.recordFailure(1_000, true)
        assertFalse(retry.canAttempt(60_999))
        assertTrue(retry.canAttempt(61_000))
        retry.recordFailure(61_000, true)
        assertFalse(retry.canAttempt(180_999))
        assertTrue(retry.canAttempt(181_000))
        retry.recordFailure(181_000, true)
        assertFalse(retry.canAttempt(420_999))
        assertTrue(retry.canAttempt(421_000))
        retry.recordFailure(421_000, true)
        assertFalse(retry.canAttempt(720_999))
        assertTrue(retry.canAttempt(721_000))
        assertEquals(300_000L, retry.delayUntilAttempt(421_000))
        retry.recordFailure(721_000, true)
        assertEquals(300_000L, retry.delayUntilAttempt(721_000))
    }

    @Test fun permanentFailuresStopAndSuccessResetAllowsNewRequest() {
        val retry = AdRetryPolicy()
        retry.recordFailure(1_000, false)
        assertFalse(retry.canAttempt(Long.MAX_VALUE))
        retry.reset()
        assertTrue(retry.canAttempt(2_000))
        assertEquals(0L, retry.delayUntilAttempt(2_000))
    }
}
