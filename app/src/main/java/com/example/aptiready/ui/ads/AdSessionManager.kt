package com.example.aptiready.ui.ads

import com.example.aptiready.data.local.AdPreferencesRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/** Small synchronous policy snapshot; all production calls are confined to the main thread. */
class AdSessionManager(
    val preferencesRepository: AdPreferencesRepository,
    val timeProvider: TimeProvider = SystemTimeProvider()
) {
    var isLoaded = false; private set
    var isFirstSession = true; private set
    var sessionInterstitialCount = 0; private set
    var dailyInterstitialCount = 0; private set
    var currentDateKey = ""; private set
    var isForeground = false; private set
    private var usage = 0L
    private var segmentStart = 0L
    private var lastMono = 0L
    private var lastWall = 0L
    private var lastZone = ""
    private var boot = ""
    private var backgroundMono = -1L
    private var lastAdMono = -1L
    private var dayChangedAtMono = 0L
    private var suppressDaysUntilMono = 0L
    private var cooldownUntilMono = 0L

    fun init() {
        if (isLoaded) return
        val now = timeProvider.elapsedRealtimeMs()
        val parts = preferencesRepository.readState()?.split('|')
        if (parts != null && parts.size == 14) {
            try {
                isFirstSession = parts[0].toBooleanStrict()
                sessionInterstitialCount = parts[1].toInt()
                dailyInterstitialCount = parts[2].toInt()
                currentDateKey = parts[3]
                usage = parts[4].toLong()
                backgroundMono = parts[5].toLong()
                lastAdMono = parts[6].toLong()
                lastMono = parts[7].toLong()
                lastWall = parts[8].toLong()
                boot = parts[9]
                lastZone = parts[10]
                dayChangedAtMono = parts[11].toLong()
                suppressDaysUntilMono = parts[12].toLong()
                cooldownUntilMono = parts[13].toLong()
                if (boot == "unknown" || boot != timeProvider.bootId() || now < lastMono) {
                    // Reboot/unknown interval: preserve caps; require a fresh cooldown and background interval.
                    backgroundMono = -1
                    lastAdMono = -1
                    usage = 0
                    cooldownUntilMono = now + AdConfig.FrequencyPolicy.MIN_INTERSTITIAL_COOLDOWN_MS
                    suppressDaysUntilMono = now + DAY_MS
                    dayChangedAtMono = now
                    lastMono = now
                    lastWall = timeProvider.currentTimeMillis()
                }
            } catch (_: Exception) {
                // Corrupted state never means a fresh unrestricted day.
                dailyInterstitialCount = AdConfig.FrequencyPolicy.MAX_INTERSTITIALS_PER_DAY
                sessionInterstitialCount = AdConfig.FrequencyPolicy.MAX_INTERSTITIALS_PER_SESSION
                currentDateKey = formatDateKey(timeProvider.currentTimeMillis())
                suppressDaysUntilMono = now + DAY_MS
            }
        } else {
            currentDateKey = preferencesRepository.getDailyDateKey() ?: formatDateKey(timeProvider.currentTimeMillis())
            dailyInterstitialCount = preferencesRepository.getDailyCount()
            sessionInterstitialCount = preferencesRepository.getSessionCount()
            isFirstSession = preferencesRepository.isFirstSession()
            if (preferencesRepository.getLastAdShowWallTimeMs() > 0)
                cooldownUntilMono = now + AdConfig.FrequencyPolicy.MIN_INTERSTITIAL_COOLDOWN_MS
            lastMono = now
            lastWall = timeProvider.currentTimeMillis()
            dayChangedAtMono = now
        }
        boot = timeProvider.bootId()
        if (lastZone.isBlank()) lastZone = TimeZone.getDefault().id
        isLoaded = true
        persist()
    }
    fun onAppForeground() {
        init()
        if (isForeground) return
        val now = timeProvider.elapsedRealtimeMs()
        refreshDay()
        if (backgroundMono >= 0 && now - backgroundMono >= AdConfig.FrequencyPolicy.SESSION_TIMEOUT_MS) {
            isFirstSession = false
            sessionInterstitialCount = 0
            usage = 0
        }
        backgroundMono = -1
        isForeground = true
        segmentStart = now
        persist()
    }
    fun onAppBackground() {
        if (!isForeground) return
        checkpoint()
        isForeground = false
        backgroundMono = timeProvider.elapsedRealtimeMs()
        persist()
    }
    fun checkpoint() {
        init()
        val now = timeProvider.elapsedRealtimeMs()
        if (isForeground) { usage += (now - segmentStart).coerceAtLeast(0); segmentStart = now }
        refreshDay()
        persist()
    }
    private fun refreshDay() {
        val now = timeProvider.elapsedRealtimeMs()
        val wall = timeProvider.currentTimeMillis()
        val zone = TimeZone.getDefault().id
        val clockJump = abs((wall - lastWall) - (now - lastMono)) > 120_000L
        if (clockJump || zone != lastZone) suppressDaysUntilMono = now + DAY_MS
        val today = formatDateKey(wall)
        // Normal midnight rollover is immediate. Clock/timezone changes require 24 steady hours.
        if (today > currentDateKey && now >= suppressDaysUntilMono) {
            currentDateKey = today
            dailyInterstitialCount = 0
            dayChangedAtMono = now
        }
        lastMono = now; lastWall = wall; lastZone = zone
    }
    private fun persist() {
        preferencesRepository.writeState(listOf(isFirstSession, sessionInterstitialCount,
            dailyInterstitialCount, currentDateKey, usage, backgroundMono, lastAdMono,
            lastMono, lastWall, boot, lastZone, dayChangedAtMono, suppressDaysUntilMono,
            cooldownUntilMono).joinToString("|"))
    }
    fun getAccumulatedForegroundUsageMs(): Long = usage +
        if (isForeground) (timeProvider.elapsedRealtimeMs() - segmentStart).coerceAtLeast(0) else 0
    fun getTimeSinceLastAdMs(): Long {
        val now = timeProvider.elapsedRealtimeMs()
        if (now < cooldownUntilMono) return 0
        return if (lastAdMono < 0) Long.MAX_VALUE else (now - lastAdMono).coerceAtLeast(0)
    }
    fun recordAdShow() {
        lastAdMono = timeProvider.elapsedRealtimeMs()
        cooldownUntilMono = lastAdMono + AdConfig.FrequencyPolicy.MIN_INTERSTITIAL_COOLDOWN_MS
        checkpoint()
    }
    fun incrementInterstitialCount() {
        checkpoint()
        sessionInterstitialCount++; dailyInterstitialCount++
        persist()
    }
    fun isAttemptConsumed(id: String) = id.isBlank() || preferencesRepository.isAttemptConsumed(id)
    fun markAttemptConsumed(id: String) { if (id.isNotBlank()) preferencesRepository.markAttemptConsumed(id) }
    fun formatDateKey(ms: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ms))
    companion object { private const val DAY_MS = 86_400_000L }
}
