package com.example.aptiready.ui.ads

/** Dismissal never closes this gate: an earned callback can arrive after dismissal. */
class RewardGrantGate {
    private var delivered = false
    @Synchronized fun grant(deliver: () -> Unit) {
        if (delivered) return
        delivered = true
        deliver()
    }
}
