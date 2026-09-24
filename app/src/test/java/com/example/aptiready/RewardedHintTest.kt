package com.example.aptiready
import com.example.aptiready.ui.ads.RewardGrantGate
import org.junit.Assert.*
import org.junit.Test

class RewardedHintTest {
    @Test fun duplicateSdkEarnedCallbacksDeliverOnce() {
        val gate = RewardGrantGate()
        var delivered = 0
        repeat(3) { gate.grant { delivered++ } }
        assertEquals(1, delivered)
    }
    @Test fun newAdHasIndependentRewardGate() {
        var delivered = 0
        RewardGrantGate().grant { delivered++ }
        RewardGrantGate().grant { delivered++ }
        assertEquals(2, delivered)
    }
}
