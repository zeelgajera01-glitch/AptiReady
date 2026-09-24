package com.example.aptiready.ui.ads

import android.app.Activity
import com.example.aptiready.data.model.AdPlacement
import kotlinx.coroutines.flow.StateFlow

interface AdProvider {
    val isInitialized: StateFlow<Boolean>
    val canRequestAds: StateFlow<Boolean>
    val isPrivacyOptionsRequired: StateFlow<Boolean>

    fun initialize(activity: Activity)
    fun showPrivacyOptionsForm(activity: Activity, onComplete: () -> Unit)
    fun showExitInterstitialIfEligible(
        activity: Activity,
        placement: AdPlacement,
        attemptId: String = "",
        onComplete: () -> Unit
    )
    fun showRewardedHintAd(
        activity: Activity,
        rewardContext: RewardContext,
        questionTitle: String,
        onRewardEarned: (RewardContext) -> Unit
    )
}
