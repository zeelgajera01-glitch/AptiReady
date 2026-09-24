package com.example.aptiready.ui.ads

import android.app.Activity
import android.content.Context
import com.example.aptiready.data.model.AdPlacement
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MobileAdsProvider(
    private val context: Context,
    private val consentManager: ConsentManager,
    val sessionManager: AdSessionManager
) : AdProvider {
    override val isInitialized = consentManager.isInitialized
    private val ready = MutableStateFlow(false)
    override val canRequestAds: StateFlow<Boolean> = ready.asStateFlow()
    override val isPrivacyOptionsRequired = consentManager.isPrivacyOptionsRequired
    val fullScreenCoordinator = FullScreenAdCoordinator(
        { consentManager.adState.value.ready }, sessionManager)
    val rewardedHintController = RewardedHintController(
        { consentManager.canLoad(AdPlacement.OPTIONAL_HINT_REWARDED) }, fullScreenCoordinator)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    init {
        scope.launch {
            consentManager.adState.collect { state ->
                fullScreenCoordinator.invalidateAd()
                rewardedHintController.invalidateAd()
                ready.value = state.ready
                // Rewarded loading is requested only by a question with real extra content.
            }
        }
    }
    override fun initialize(activity: Activity) {
        if (!AdConfig.isAdsEnabled) return
        consentManager.gatherConsent(activity) {}
    }
    override fun showPrivacyOptionsForm(activity: Activity, onComplete: () -> Unit) =
        consentManager.showPrivacyOptionsForm(activity, onComplete)
    override fun showExitInterstitialIfEligible(activity: Activity, placement: AdPlacement,
        attemptId: String, onComplete: () -> Unit) {
        fullScreenCoordinator.showExitInterstitialIfEligible(activity, placement, attemptId, onComplete)
    }
    fun prepareExitAd(placement: AdPlacement) {
        if (consentManager.canLoad(placement))
            fullScreenCoordinator.preloadInterstitial(context.applicationContext, placement)
    }
    fun prepareRewardedHint() {
        if (consentManager.canLoad(AdPlacement.OPTIONAL_HINT_REWARDED))
            rewardedHintController.preloadRewardedAd(context.applicationContext)
    }
    override fun showRewardedHintAd(activity: Activity, rewardContext: RewardContext,
        questionTitle: String, onRewardEarned: (RewardContext) -> Unit) {
        if (consentManager.canLoad(AdPlacement.OPTIONAL_HINT_REWARDED))
            rewardedHintController.requestRewardedHint(activity, rewardContext, questionTitle, onRewardEarned)
    }
}
