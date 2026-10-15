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

    private val slot = AdLoadSlot<InterstitialAd>(sessionManager.timeProvider::elapsedRealtimeMs)
    private fun key(placement: AdPlacement) = "${AdConfig.getAdUnitIdForPlacement(placement)}:${AdConfig.revision.value}"
    private var currentKey: String? = null
    private fun isActivitySafe(activity: Activity): Boolean {
        return !activity.isFinishing && !activity.isDestroyed &&
            (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.currentState
                ?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) == true
    }

    fun preloadInterstitial(context: Context, placement: AdPlacement) {
        if (!AdConfig.isPlacementEnabled(placement) || !canRequestAds() || isFullScreenAdActive) return
        val requestKey = key(placement)
        currentKey = requestKey
        val token = slot.begin(requestKey) ?: return
        val config = AdConfig.revision.value
        val unitId = AdConfig.getAdUnitIdForPlacement(placement)
        AdLogger.d("AdCoordinator", "Requesting interstitial: placement=$placement, unitId=$unitId")
        try {
            InterstitialAd.load(context.applicationContext, unitId, AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        if (!canRequestAds() || !AdConfig.isPlacementEnabled(placement) || config != AdConfig.revision.value) {
                            slot.fail(token, true)
                            return
                        }
                        if (slot.accept(token, ad))
                            AdLogger.d("AdCoordinator", "Interstitial loaded: response=${ad.responseInfo}")
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        slot.fail(token, error.code != AdRequest.ERROR_CODE_INVALID_REQUEST)
                        AdLogger.w("AdCoordinator", "Interstitial failed: placement=$placement, unitId=$unitId, $error")
                    }
                })
        } catch (failure: Exception) {
            slot.fail(token, true)
            AdLogger.w("AdCoordinator", "Interstitial request failed: ${failure.message}")
        }
    }

    fun invalidateAd() { slot.clear(); currentKey = null }

    fun isAdLoadedAndValid() = currentKey?.let { slot.cached(it) != null } == true
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

        try { sessionManager.checkpoint() } catch (failure: Exception) {
            sessionManager.onStorageFailure(failure)
            onComplete()
            return
        }
        if (attemptId.isBlank()) { onComplete(); return }

        val qualifiedAttemptId = if (attemptId.isNotBlank()) "${placement.name}:$attemptId" else ""
        val isConsumed = try {
            qualifiedAttemptId.isNotBlank() && sessionManager.isAttemptConsumed(qualifiedAttemptId)
        } catch (failure: Exception) {
            sessionManager.onStorageFailure(failure)
            onComplete()
            return
        }

        val hasAd = slot.cached(key(placement)) != null

        val policyResult = AdEligibilityPolicy.evaluateExitInterstitial(
            isAdsEnabled = AdConfig.isAdsEnabled,
            isPlacementEnabled = AdConfig.isPlacementEnabled(placement),
            canRequestAds = canRequestAds(),
            isPolicyLoaded = sessionManager.isPolicyUsable,
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
                runCatching { sessionManager.markAttemptConsumed(qualifiedAttemptId) }.onFailure { sessionManager.onStorageFailure(it) }
            }
            onComplete()
            return
        }

        val ad = slot.take(key(placement))
        if (ad == null) {
            if (qualifiedAttemptId.isNotBlank()) runCatching { sessionManager.markAttemptConsumed(qualifiedAttemptId) }.onFailure { sessionManager.onStorageFailure(it) }
            onComplete()
            return
        }

        isFullScreenAdActive = true

        if (qualifiedAttemptId.isNotBlank()) {
            runCatching { sessionManager.markAttemptConsumed(qualifiedAttemptId) }.onFailure { sessionManager.onStorageFailure(it) }
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
                    }.onFailure { sessionManager.onStorageFailure(it) }
                }
            }

            override fun onAdDismissedFullScreenContent() {
                if (completed) return
                AdLogger.d("AdCoordinator", "Interstitial dismissed by user")
                isFullScreenAdActive = false
                runCatching { sessionManager.recordAdShow() }.onFailure { sessionManager.onStorageFailure(it) }
                completeOnce()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                if (completed) return
                AdLogger.w("AdCoordinator", "Interstitial show failed: ${error.message}")
                isFullScreenAdActive = false
                runCatching { sessionManager.recordAdShow() }.onFailure { sessionManager.onStorageFailure(it) }
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
