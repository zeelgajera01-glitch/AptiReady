package com.example.aptiready.ui.ads

import android.app.Activity
import android.content.Context
import com.example.aptiready.data.model.AdPlacement
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class FullScreenAdCoordinator(
    private val canRequestAds: () -> Boolean,
    val sessionManager: AdSessionManager
) {

    var isFullScreenAdActive: Boolean = false

    private var interstitialAd: InterstitialAd? = null
    private var loadedPlacement: AdPlacement? = null
    private var nextRetryMs = 0L
    private var adLoadedTimeMs: Long = 0L
    private var isLoadingInterstitial = false
    private var loadGeneration: Int = 0

    private fun isActivitySafe(activity: Activity): Boolean {
        return !activity.isFinishing && !activity.isDestroyed &&
            (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.currentState
                ?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) == true
    }

    fun preloadInterstitial(context: Context, placement: AdPlacement) {
        if (isLoadingInterstitial || sessionManager.timeProvider.elapsedRealtimeMs() < nextRetryMs) return
        if (loadedPlacement != null && loadedPlacement != placement) invalidateAd()
        if (!AdConfig.isPlacementEnabled(placement) || !canRequestAds()) return

        if (interstitialAd != null) {
            val now = sessionManager.timeProvider.elapsedRealtimeMs()
            if (now - adLoadedTimeMs < AdConfig.FrequencyPolicy.AD_EXPIRE_DURATION_MS) {
                return
            } else {
                interstitialAd = null
            }
        }

        isLoadingInterstitial = true
        val currentGen = loadGeneration
        val configToken = AdConfig.revision.value
        val adUnitId = AdConfig.getAdUnitIdForPlacement(placement)
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    if (currentGen != loadGeneration) return
                    isLoadingInterstitial = false
                    if (configToken == AdConfig.revision.value && canRequestAds() && AdConfig.isPlacementEnabled(placement)) {
                        interstitialAd = ad
                        loadedPlacement = placement
                        adLoadedTimeMs = sessionManager.timeProvider.elapsedRealtimeMs()
                        AdLogger.d("AdCoordinator", "Interstitial ad loaded successfully for $placement")
                    } else {
                        interstitialAd = null
                        AdLogger.d("AdCoordinator", "Loaded interstitial discarded due to invalidation")
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (currentGen != loadGeneration) return
                    nextRetryMs = sessionManager.timeProvider.elapsedRealtimeMs() + 30_000L
                    isLoadingInterstitial = false
                    interstitialAd = null
                    AdLogger.w("AdCoordinator", "Interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    fun invalidateAd() {
        interstitialAd = null
        loadedPlacement = null
        isLoadingInterstitial = false
        loadGeneration++
        AdLogger.d("AdCoordinator", "FullScreenAdCoordinator ads invalidated")
    }

    fun isAdLoadedAndValid(): Boolean {
        val ad = interstitialAd ?: return false
        val now = sessionManager.timeProvider.elapsedRealtimeMs()
        if (now - adLoadedTimeMs >= AdConfig.FrequencyPolicy.AD_EXPIRE_DURATION_MS) {
            interstitialAd = null
            return false
        }
        return true
    }

    fun showExitInterstitialIfEligible(
        activity: Activity,
        placement: AdPlacement,
        attemptId: String = "",
        onComplete: () -> Unit
    ) {
        val isHostSafe = isActivitySafe(activity)
        if (!isHostSafe) {
            AdLogger.w("AdCoordinator", "Host activity is not safe for display")
            onComplete()
            return
        }

        try { sessionManager.checkpoint() } catch (_: Exception) { onComplete(); return }
        if (attemptId.isBlank()) { onComplete(); return }

        val qualifiedAttemptId = if (attemptId.isNotBlank()) "${placement.name}:$attemptId" else ""
        val isConsumed = if (qualifiedAttemptId.isNotBlank()) sessionManager.isAttemptConsumed(qualifiedAttemptId) else false

        val hasAd = loadedPlacement == placement && isAdLoadedAndValid()

        val policyResult = AdEligibilityPolicy.evaluateExitInterstitial(
            isAdsEnabled = AdConfig.isAdsEnabled,
            isPlacementEnabled = AdConfig.isPlacementEnabled(placement),
            canRequestAds = canRequestAds(),
            isPolicyLoaded = sessionManager.isLoaded,
            isFirstSession = sessionManager.isFirstSession,
            sessionForegroundUsageMs = sessionManager.getAccumulatedForegroundUsageMs(),
            timeSinceLastAdMs = sessionManager.getTimeSinceLastAdMs(),
            sessionInterstitialCount = sessionManager.sessionInterstitialCount,
            dailyInterstitialCount = sessionManager.dailyInterstitialCount,
            isAttemptConsumed = isConsumed,
            hasLoadedAd = hasAd,
            isFullScreenAdActive = isFullScreenAdActive,
            isHostSafe = isHostSafe
        )

        AdLogger.d("AdCoordinator", "Exit interstitial evaluation for $placement: eligible=${policyResult.isEligible}, reason=${policyResult.reason}")

        if (!policyResult.isEligible) {
            if (qualifiedAttemptId.isNotBlank() && policyResult.reason != AdEligibilityReason.ATTEMPT_ALREADY_CONSUMED) {
                runCatching { sessionManager.markAttemptConsumed(qualifiedAttemptId) }.onFailure { AdConfig.isAdsEnabled = false }
            }
            onComplete()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            if (qualifiedAttemptId.isNotBlank()) runCatching { sessionManager.markAttemptConsumed(qualifiedAttemptId) }.onFailure { AdConfig.isAdsEnabled = false }
            onComplete()
            return
        }

        isFullScreenAdActive = true
        interstitialAd = null
        if (qualifiedAttemptId.isNotBlank()) {
            runCatching { sessionManager.markAttemptConsumed(qualifiedAttemptId) }.onFailure { AdConfig.isAdsEnabled = false }
        }

        var hasIncrementedCount = false
        var completed = false
        fun completeOnce() { if (!completed) { completed = true; onComplete() } }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                AdLogger.d("AdCoordinator", "Interstitial full-screen showed")
                if (!completed && !hasIncrementedCount) {
                    hasIncrementedCount = true
                    runCatching {
                        sessionManager.recordAdShow() // Persist a cooldown even if the process dies during display.
                        sessionManager.incrementInterstitialCount()
                    }.onFailure { AdConfig.isAdsEnabled = false }
                }
            }

            override fun onAdDismissedFullScreenContent() {
                if (completed) return
                AdLogger.d("AdCoordinator", "Interstitial dismissed by user")
                isFullScreenAdActive = false
                runCatching { sessionManager.recordAdShow() }.onFailure { AdConfig.isAdsEnabled = false }
                completeOnce()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                if (completed) return
                AdLogger.w("AdCoordinator", "Interstitial show failed: ${error.message}")
                isFullScreenAdActive = false
                runCatching { sessionManager.recordAdShow() }.onFailure { AdConfig.isAdsEnabled = false }
                completeOnce()
            }
        }

        try {
            if (!canRequestAds() || !AdConfig.isPlacementEnabled(placement) || !isActivitySafe(activity)) {
                isFullScreenAdActive = false
                completeOnce()
                return
            }
            ad.show(activity)
        } catch (e: Exception) {
            AdLogger.w("AdCoordinator", "Exception while showing interstitial: ${e.message}")
            isFullScreenAdActive = false
            completeOnce()
        }
    }

    fun recordRewardedShow() {
        sessionManager.recordAdShow()
    }
}
