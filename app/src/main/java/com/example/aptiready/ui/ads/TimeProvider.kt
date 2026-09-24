package com.example.aptiready.ui.ads

import android.os.SystemClock

interface TimeProvider {
    fun elapsedRealtimeMs(): Long
    fun currentTimeMillis(): Long
    fun bootId(): String = "unknown"
}

class SystemTimeProvider(private val context: android.content.Context? = null) : TimeProvider {
    override fun bootId(): String = context?.let { android.provider.Settings.Global.getInt(it.contentResolver, android.provider.Settings.Global.BOOT_COUNT, -1).toString() } ?: "unknown"
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

class FakeTimeProvider(
    private var initialElapsedRealtimeMs: Long = 1_000_000L,
    private var initialCurrentTimeMillis: Long = 1_700_000_000_000L
) : TimeProvider {

    private var currentElapsedRealtimeMs = initialElapsedRealtimeMs
    private var currentWallTimeMs = initialCurrentTimeMillis

    override fun elapsedRealtimeMs(): Long = currentElapsedRealtimeMs
    override fun currentTimeMillis(): Long = currentWallTimeMs

    override fun bootId(): String = "test-boot"

    fun advanceTimeMs(ms: Long) {
        currentElapsedRealtimeMs += ms
        currentWallTimeMs += ms
    }

    fun setWallTimeMs(timeMs: Long) {
        currentWallTimeMs = timeMs
    }

    fun setElapsedRealtimeMs(timeMs: Long) {
        currentElapsedRealtimeMs = timeMs
    }
}
