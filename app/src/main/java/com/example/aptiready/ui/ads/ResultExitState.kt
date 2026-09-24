package com.example.aptiready.ui.ads
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/** SDK completion touches no Fragment. A recreated view observes pending navigation. */
class ResultExitState(private val saved: SavedStateHandle) : ViewModel() {
    val state = saved.getStateFlow("ad_exit", "IDLE")
    init {
        // A new ViewModel with BUSY restored means the process died during an exit.
        if (state.value == "BUSY") saved["ad_exit"] = "READY"
    }
    fun begin(): Boolean {
        if (state.value != "IDLE") return false
        saved["ad_exit"] = "BUSY"
        return true
    }
    fun complete() { if (state.value == "BUSY") saved["ad_exit"] = "READY" }
    fun consumed() { saved["ad_exit"] = "IDLE" }
}
