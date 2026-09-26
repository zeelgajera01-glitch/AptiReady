package com.example.aptiready.ui.ads

import android.app.Activity
import android.content.Context
import com.example.aptiready.data.model.AdPlacement
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AdRuntimeState(val ready: Boolean = false, val generation: Long = 0)

class ConsentManager(context: Context) {
    private val appContext = context.applicationContext
    private val information by lazy { UserMessagingPlatform.getConsentInformation(appContext) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val allowed = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = allowed.asStateFlow()
    private val privacy = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = privacy.asStateFlow()
    private val initialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = initialized.asStateFlow()
    private val runtime = MutableStateFlow(AdRuntimeState())
    val adState: StateFlow<AdRuntimeState> = runtime.asStateFlow()
    private var latestHost = java.lang.ref.WeakReference<Activity>(null)
    private var gathering = false
    private var sdkStarting = false
    private var gatheredThisProcess = false
    private var nextConsentRetryAt = 0L

    init {
        scope.launch { AdConfig.revision.collect { publish() } }
    }
    fun canLoad(placement: AdPlacement): Boolean =
        runtime.value.ready && AdConfig.isPlacementEnabled(placement)

    private fun publish() {
        runtime.value = AdRuntimeState(
            AdConfig.isAdsEnabled && allowed.value && initialized.value && !gathering,
            runtime.value.generation + 1
        )
    }
    private fun safe(activity: Activity) = !activity.isFinishing && !activity.isDestroyed

    fun gatherConsent(activity: Activity, onConsentGathered: (Boolean) -> Unit) {
        if (!AdConfig.isAdsEnabled || !safe(activity)) { onConsentGathered(false); return }
        latestHost = java.lang.ref.WeakReference(activity)
        if (gathering) return // The shared observable state delivers completion to every screen.
        if (gatheredThisProcess) { onConsentGathered(runtime.value.ready); return }
        if (android.os.SystemClock.elapsedRealtime() < nextConsentRetryAt) {
            onConsentGathered(false)
            return
        }
        gathering = true
        publish()
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(AdConfig.Audience.TAG_FOR_UNDER_AGE_OF_CONSENT).build()
        information.requestConsentInfoUpdate(activity, params, {
            val host = latestHost.get()
            if (host == null || !safe(host)) {
                gathering = false
                gatheredThisProcess = false
                publish()
                onConsentGathered(false)
            } else {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(host) {
                    finishConsent(onConsentGathered)
                }
            }
        }, { finishConsent(onConsentGathered) })
    }
    private fun finishConsent(done: (Boolean) -> Unit) {
        gathering = false

        val canRequest = information.canRequestAds()
        allowed.value = canRequest
        gatheredThisProcess = canRequest

        nextConsentRetryAt = if (canRequest) {
            0L
        } else {
            android.os.SystemClock.elapsedRealtime() + 30_000L
        }

        privacy.value =
            information.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        publish()
        initializeSdkIfAllowed()
        done(runtime.value.ready)
    }
    private fun initializeSdkIfAllowed() {
        if (!AdConfig.isAdsEnabled || !allowed.value || initialized.value || sdkStarting) return

        sdkStarting = true

        val requestConfiguration = MobileAds.getRequestConfiguration()
            .toBuilder()
            .setTagForUnderAgeOfConsent(
                if (AdConfig.Audience.TAG_FOR_UNDER_AGE_OF_CONSENT)
                    RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
                else
                    RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
            )
            .build()

        MobileAds.setRequestConfiguration(requestConfiguration)

        MobileAds.initialize(appContext) {
            scope.launch {
                sdkStarting = false
                initialized.value = true
                publish()
            }
        }
    }
    fun showPrivacyOptionsForm(activity: Activity, onComplete: () -> Unit) {
        if (!AdConfig.isAdsEnabled || !safe(activity) || gathering) { onComplete(); return }
        gathering = true
        publish() // Invalidate old requests even when the eventual permission stays true.
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            finishConsent { onComplete() }
        }
    }
}
