package com.example.aptiready.ui.ads

import android.app.Activity
import com.example.aptiready.data.model.AdPlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAdProvider(
    shouldGrantConsent: Boolean = true,
    private val shouldShowInterstitial: Boolean = true
) : AdProvider {

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _canRequestAds = MutableStateFlow(shouldGrantConsent)
    override val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    override val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    var exitInterstitialShowCount = 0
    var rewardedHintShowCount = 0

    override fun initialize(activity: Activity) {
        _isInitialized.value = true
        AdLogger.d("FakeAdProvider", "Fake Ads Initialized")
    }

    override fun showPrivacyOptionsForm(activity: Activity, onComplete: () -> Unit) {
        AdLogger.d("FakeAdProvider", "Showing Fake Privacy Options")
        onComplete()
    }

    override fun showExitInterstitialIfEligible(
        activity: Activity,
        placement: AdPlacement,
        attemptId: String,
        onComplete: () -> Unit
    ) {
        AdLogger.d("FakeAdProvider", "Showing Fake Interstitial for $placement with attempt $attemptId")
        if (shouldShowInterstitial && canRequestAds.value) {
            exitInterstitialShowCount++
        }
        onComplete()
    }

    override fun showRewardedHintAd(
        activity: Activity,
        rewardContext: RewardContext,
        questionTitle: String,
        onRewardEarned: (RewardContext) -> Unit
    ) {
        AdLogger.d("FakeAdProvider", "Showing Fake Rewarded Ad for ${rewardContext.questionId}")
        if (canRequestAds.value) {
            rewardedHintShowCount++
            onRewardEarned(rewardContext)
        }
    }
}
