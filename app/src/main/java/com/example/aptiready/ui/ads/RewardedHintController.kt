package com.example.aptiready.ui.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.aptiready.data.model.AdPlacement
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class RewardContext(val ownerId: String, val sessionId: String, val questionId: String,
    val position: Int, val operationId: String = UUID.randomUUID().toString())

enum class RewardedState { IDLE, LOADING, READY, SHOWING, UNAVAILABLE }

class RewardedHintController(
    private val canRequestAds: () -> Boolean,
    private val coordinator: FullScreenAdCoordinator? = null
) {
    private val slot = AdLoadSlot<RewardedAd>(SystemClock::elapsedRealtime)
    private fun key() = "${AdConfig.getAdUnitIdForPlacement(placement)}:${AdConfig.revision.value}"
    private var generation = 0

    private val mutableState = MutableStateFlow(RewardedState.IDLE)
    val state: StateFlow<RewardedState> = mutableState.asStateFlow()
    private val placement = AdPlacement.OPTIONAL_HINT_REWARDED
    private fun allowed() = canRequestAds() && AdConfig.isPlacementEnabled(placement)
    private fun hostSafe(a: Activity) = !a.isFinishing && !a.isDestroyed &&
        (a as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true
    fun preloadRewardedAd(context: Context) {
        if (!allowed() || coordinator?.isFullScreenAdActive == true) return
        val token = slot.begin(key()) ?: return
        mutableState.value = RewardedState.LOADING
        val config = AdConfig.revision.value
        val unitId = AdConfig.getAdUnitIdForPlacement(placement)
        AdLogger.d("RewardedHintController", "Requesting rewarded: unitId=$unitId")
        try {
            RewardedAd.load(context.applicationContext, unitId, AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        if (!allowed() || config != AdConfig.revision.value) {
                            if (slot.fail(token, true)) mutableState.value = RewardedState.IDLE
                            return
                        }
                        if (slot.accept(token, ad)) {
                            mutableState.value = RewardedState.READY
                            AdLogger.d("RewardedHintController", "Rewarded loaded: response=${ad.responseInfo}")
                        }
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (slot.fail(token, error.code != AdRequest.ERROR_CODE_INVALID_REQUEST))
                            mutableState.value = RewardedState.UNAVAILABLE
                        AdLogger.w("RewardedHintController", "Rewarded failed: unitId=$unitId, $error")
                    }
                })
        } catch (failure: Exception) {
            if (slot.fail(token, true)) mutableState.value = RewardedState.UNAVAILABLE
            AdLogger.w("RewardedHintController", "Rewarded request failed: ${failure.message}")
        }
    }
    fun invalidateAd() {
        generation++
        slot.clear()
        if (coordinator?.isFullScreenAdActive != true) mutableState.value = RewardedState.IDLE
    }
    fun isAdLoadedAndReady() = allowed() && slot.cached(key()) != null
    fun requestRewardedHint(activity: Activity, rewardContext: RewardContext, questionTitle: String,
        onRewardEarned: (RewardContext) -> Unit) {
        if (!allowed() || !hostSafe(activity) || coordinator?.isFullScreenAdActive == true) return
        if (!isAdLoadedAndReady()) {
            preloadRewardedAd(activity.applicationContext)
            return // Never automatically show an ad after loading.
        }
        // Reserve the same lock while confirmation is open to prevent duplicate dialogs/ads.
        coordinator?.isFullScreenAdActive = true
        var handedToSdk = false
        val token = generation
        val config = AdConfig.revision.value
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Unlock Hint")
            .setMessage(
                "Watch a rewarded ad to unlock the hint for this question. " +
                    "The hint unlocks only when the ad confirms you earned the reward. " +
                    "Your one free hint allowance will not reset."
            )
            .setPositiveButton("Watch Ad") { _, _ ->
                if (
                    token == generation &&
                    config == AdConfig.revision.value &&
                    allowed() &&
                    hostSafe(activity) &&
                    isAdLoadedAndReady()
                ) {
                    handedToSdk = true
                    show(activity, rewardContext, onRewardEarned)
                }
            }.setNegativeButton("Cancel", null).create()
        val lifecycleOwner = activity as LifecycleOwner
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) { if (!handedToSdk) dialog.dismiss() }
            override fun onDestroy(owner: LifecycleOwner) { if (!handedToSdk) dialog.dismiss() }
        }
        dialog.setOnDismissListener {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!handedToSdk) coordinator?.isFullScreenAdActive = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        try { dialog.show() } catch (_: Exception) {
            lifecycleOwner.lifecycle.removeObserver(observer)
            coordinator?.isFullScreenAdActive = false
        }
    }
    private fun show(
        activity: Activity,
        context: RewardContext,
        earned: (RewardContext) -> Unit
    ) {
        val ad = slot.take(key())

        if (ad == null || !allowed() || !hostSafe(activity)) {
            coordinator?.isFullScreenAdActive = false
            mutableState.value = RewardedState.IDLE
            return
        }

        mutableState.value = RewardedState.SHOWING

        val grantGate = RewardGrantGate()
        var finished = false
        var impressionRecorded = false

        fun finish() {
            if (finished) return
            finished = true

            coordinator?.isFullScreenAdActive = false
            mutableState.value = RewardedState.IDLE
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                if (impressionRecorded) return
                impressionRecorded = true

                runCatching {
                    coordinator?.recordRewardedShow()
                }.onFailure {
                    coordinator?.sessionManager?.onStorageFailure(it)
                }
            }

            override fun onAdDismissedFullScreenContent() {
                // Closing an ad does not grant a hint.
                finish()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                AdLogger.w("RewardedHintController", "Rewarded show failed: $error")
                finish()
            }
        }

        try {
            ad.show(activity) {
                // Keep delivery independent of the Fragment lifecycle.
                // Do not reject an earned reward merely because dismissal
                // arrived first: mediation callback ordering can differ.
                grantGate.grant {
                    earned(context)
                }
            }
        } catch (_: Exception) {
            finish()
        }
    }
}
