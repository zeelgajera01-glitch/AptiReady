package com.example.aptiready
import com.example.aptiready.data.local.InMemoryAdPreferencesRepository
import com.example.aptiready.ui.ads.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class AdPolicyTest {
    @Test fun firstSessionEndsAfterRealBackgroundIntervalWithoutAnyAd() {
        val clock = FakeTimeProvider()
        val manager = AdSessionManager(InMemoryAdPreferencesRepository(), clock)
        manager.onAppForeground()
        assertTrue(manager.isFirstSession)
        manager.onAppBackground()
        clock.advanceTimeMs(1_800_000)
        manager.onAppForeground()
        assertFalse(manager.isFirstSession)
    }
    @Test fun restartPreservesUsageAndCapsAndExcludesBackground() {
        val clock = FakeTimeProvider()
        val store = InMemoryAdPreferencesRepository()
        val first = AdSessionManager(store, clock)
        first.onAppForeground()
        clock.advanceTimeMs(90_000)
        first.incrementInterstitialCount()
        first.onAppBackground()
        clock.advanceTimeMs(10_000)
        val restarted = AdSessionManager(store, clock)
        restarted.onAppForeground()
        assertEquals(90_000L, restarted.getAccumulatedForegroundUsageMs())
        assertEquals(1, restarted.sessionInterstitialCount)
        assertEquals(1, restarted.dailyInterstitialCount)
    }
    @Test fun wallClockRollbackDoesNotStartNewSessionOrResetCap() {
        val clock = FakeTimeProvider()
        val manager = AdSessionManager(InMemoryAdPreferencesRepository(), clock)
        manager.onAppForeground()
        manager.incrementInterstitialCount()
        manager.onAppBackground()
        clock.setWallTimeMs(clock.currentTimeMillis() - 86_400_000)
        clock.advanceTimeMs(1_000)
        manager.onAppForeground()
        assertTrue(manager.isFirstSession)
        assertEquals(1, manager.dailyInterstitialCount)
        assertEquals(1, manager.sessionInterstitialCount)
    }
    @Test fun normalMidnightResetsDailyCountButNotSessionCount() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val clock = FakeTimeProvider(initialCurrentTimeMillis = 1_699_919_990_000L)
            val manager = AdSessionManager(InMemoryAdPreferencesRepository(), clock)
            manager.onAppForeground()
            manager.incrementInterstitialCount()
            val oldDay = manager.currentDateKey
            clock.advanceTimeMs(86_400_000)
            manager.checkpoint()
            assertTrue(manager.currentDateKey > oldDay)
            assertEquals(0, manager.dailyInterstitialCount)
            assertEquals(1, manager.sessionInterstitialCount)
        } finally { TimeZone.setDefault(original) }
    }
    @Test fun clockJumpForwardDoesNotImmediatelyResetDailyLimit() {
        val clock = FakeTimeProvider()
        val manager = AdSessionManager(InMemoryAdPreferencesRepository(), clock)
        manager.onAppForeground()
        manager.incrementInterstitialCount()
        clock.setWallTimeMs(clock.currentTimeMillis() + 86_400_000)
        manager.checkpoint()
        assertEquals(1, manager.dailyInterstitialCount)
    }
    @Test fun cooldownSurvivesProcessRestart() {
        val clock = FakeTimeProvider()
        val store = InMemoryAdPreferencesRepository()
        val first = AdSessionManager(store, clock)
        first.onAppForeground()
        first.recordAdShow()
        clock.advanceTimeMs(20_000)
        val restarted = AdSessionManager(store, clock)
        restarted.init()
        assertTrue(restarted.getTimeSinceLastAdMs() < 180_000)
    }
    @Test fun consumedAttemptIsNotEvictedByNewerAttempts() {
        val manager = AdSessionManager(InMemoryAdPreferencesRepository(), FakeTimeProvider())
        manager.init()
        repeat(150) { manager.markAttemptConsumed("practice:$it") }
        assertTrue(manager.isAttemptConsumed("practice:0"))
    }
}
