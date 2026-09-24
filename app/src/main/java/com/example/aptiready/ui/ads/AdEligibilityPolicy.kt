package com.example.aptiready.ui.ads

import com.example.aptiready.data.model.AdPlacement

enum class AdEligibilityReason {
    ELIGIBLE,
    ADS_DISABLED,
    PLACEMENT_DISABLED,
    CONSENT_INELIGIBLE,
    SDK_NOT_READY,
    POLICY_NOT_LOADED,
    FIRST_SESSION,
    INSUFFICIENT_FOREGROUND_USAGE,
    COOLDOWN_ACTIVE,
    SESSION_CAP_REACHED,
    DAILY_CAP_REACHED,
    ATTEMPT_ALREADY_CONSUMED,
    NO_LOADED_AD,
    ANOTHER_FULLSCREEN_AD_ACTIVE,
    HOST_NOT_SAFE_FOR_DISPLAY
}

data class AdEligibilityResult(
    val isEligible: Boolean,
    val reason: AdEligibilityReason
) {
    companion object {
        fun eligible(): AdEligibilityResult = AdEligibilityResult(true, AdEligibilityReason.ELIGIBLE)
        fun ineligible(reason: AdEligibilityReason): AdEligibilityResult = AdEligibilityResult(false, reason)
    }
}

object AdEligibilityPolicy {

    fun isNativeAdListEligible(placement: AdPlacement, itemCount: Int): Boolean {
        if (!AdConfig.isPlacementEnabled(placement)) return false
        return when (placement) {
            AdPlacement.TOPIC_LIST_NATIVE -> itemCount >= AdConfig.ListThresholds.MIN_TOPICS_FOR_NATIVE_AD
            AdPlacement.TEST_CATALOG_NATIVE -> itemCount >= AdConfig.ListThresholds.MIN_TESTS_FOR_NATIVE_AD
            AdPlacement.HISTORY_NATIVE -> itemCount >= AdConfig.ListThresholds.MIN_HISTORY_FOR_NATIVE_AD
            AdPlacement.BOOKMARKS_NATIVE -> itemCount >= AdConfig.ListThresholds.MIN_BOOKMARKS_FOR_NATIVE_AD
            else -> true
        }
    }

    fun canShowExitInterstitial(
        isFirstSession: Boolean,
        foregroundUsageMs: Long,
        timeSinceLastAdMs: Long,
        sessionInterstitialCount: Int,
        dailyInterstitialCount: Int
    ): Boolean {
        return evaluateExitInterstitial(
            isAdsEnabled = AdConfig.isAdsEnabled,
            isPlacementEnabled = true,
            canRequestAds = true,
            isPolicyLoaded = true,
            isFirstSession = isFirstSession,
            sessionForegroundUsageMs = foregroundUsageMs,
            timeSinceLastAdMs = timeSinceLastAdMs,
            sessionInterstitialCount = sessionInterstitialCount,
            dailyInterstitialCount = dailyInterstitialCount,
            isAttemptConsumed = false,
            hasLoadedAd = true,
            isFullScreenAdActive = false,
            isHostSafe = true
        ).isEligible
    }

    fun evaluateExitInterstitial(
        isAdsEnabled: Boolean,
        isPlacementEnabled: Boolean,
        canRequestAds: Boolean,
        isPolicyLoaded: Boolean,
        isFirstSession: Boolean,
        sessionForegroundUsageMs: Long,
        timeSinceLastAdMs: Long,
        sessionInterstitialCount: Int,
        dailyInterstitialCount: Int,
        isAttemptConsumed: Boolean,
        hasLoadedAd: Boolean,
        isFullScreenAdActive: Boolean,
        isHostSafe: Boolean
    ): AdEligibilityResult {
        if (!isAdsEnabled) return AdEligibilityResult.ineligible(AdEligibilityReason.ADS_DISABLED)
        if (!isPlacementEnabled) return AdEligibilityResult.ineligible(AdEligibilityReason.PLACEMENT_DISABLED)
        if (!canRequestAds) return AdEligibilityResult.ineligible(AdEligibilityReason.CONSENT_INELIGIBLE)
        if (!isPolicyLoaded) return AdEligibilityResult.ineligible(AdEligibilityReason.POLICY_NOT_LOADED)
        if (!isHostSafe) return AdEligibilityResult.ineligible(AdEligibilityReason.HOST_NOT_SAFE_FOR_DISPLAY)
        if (isFullScreenAdActive) return AdEligibilityResult.ineligible(AdEligibilityReason.ANOTHER_FULLSCREEN_AD_ACTIVE)
        if (isFirstSession && !AdConfig.FrequencyPolicy.FIRST_SESSION_INTERSTITIAL_ALLOWED) {
            return AdEligibilityResult.ineligible(AdEligibilityReason.FIRST_SESSION)
        }
        if (sessionForegroundUsageMs < AdConfig.FrequencyPolicy.MIN_FOREGROUND_USAGE_MS) {
            return AdEligibilityResult.ineligible(AdEligibilityReason.INSUFFICIENT_FOREGROUND_USAGE)
        }
        if (timeSinceLastAdMs < AdConfig.FrequencyPolicy.MIN_INTERSTITIAL_COOLDOWN_MS) {
            return AdEligibilityResult.ineligible(AdEligibilityReason.COOLDOWN_ACTIVE)
        }
        if (sessionInterstitialCount >= AdConfig.FrequencyPolicy.MAX_INTERSTITIALS_PER_SESSION) {
            return AdEligibilityResult.ineligible(AdEligibilityReason.SESSION_CAP_REACHED)
        }
        if (dailyInterstitialCount >= AdConfig.FrequencyPolicy.MAX_INTERSTITIALS_PER_DAY) {
            return AdEligibilityResult.ineligible(AdEligibilityReason.DAILY_CAP_REACHED)
        }
        if (isAttemptConsumed) {
            return AdEligibilityResult.ineligible(AdEligibilityReason.ATTEMPT_ALREADY_CONSUMED)
        }
        if (!hasLoadedAd) {
            return AdEligibilityResult.ineligible(AdEligibilityReason.NO_LOADED_AD)
        }
        return AdEligibilityResult.eligible()
    }
}
