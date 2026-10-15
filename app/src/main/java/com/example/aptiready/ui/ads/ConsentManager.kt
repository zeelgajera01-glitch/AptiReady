package com.example.aptiready.ui.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.example.aptiready.data.model.AdPlacement
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference

data class AdRuntimeState(val ready: Boolean = false, val generation: Long = 0)

/** One consent update per launch; UMP remains the authority for every ad request. */
class ConsentManager(context: Context) {
    private val appContext = context.applicationContext
    private val information = UserMessagingPlatform.getConsentInformation(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val allowed = MutableStateFlow(false)
    val canRequestAds = allowed.asStateFlow()
    private val privacy = MutableStateFlow(false)
    val isPrivacyOptionsRequired = privacy.asStateFlow()
    private val initialized = MutableStateFlow(false)
    val isInitialized = initialized.asStateFlow()
    private val runtime = MutableStateFlow(AdRuntimeState())
    val adState = runtime.asStateFlow()
    private var host = WeakReference<Activity>(null)
    private var gathering = false
    private var privacyFormOpen = false
    private var sdkStarting = false
    private var updated = false
    private var retryAt = 0L
    private val completions = mutableListOf<(Boolean) -> Unit>()

    init {
        scope.launch { AdConfig.revision.collect { publish(force = true); initializeSdkIfAllowed() } }
    }

    fun canLoad(placement: AdPlacement) = runtime.value.ready && AdConfig.isPlacementEnabled(placement)
    private fun safe(activity: Activity) = !activity.isFinishing && !activity.isDestroyed

    private fun publish(force: Boolean = false) {
        val ready = AdConfig.isAdsEnabled && allowed.value && initialized.value && !privacyFormOpen
        if (force || ready != runtime.value.ready)
            runtime.value = AdRuntimeState(ready, runtime.value.generation + 1)
    }

    private fun refreshPermission() {
        allowed.value = information.canRequestAds()
        privacy.value = information.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        publish()
        initializeSdkIfAllowed()
    }

    fun gatherConsent(activity: Activity, onConsentGathered: (Boolean) -> Unit) {
        if (!AdConfig.isAdsEnabled || !safe(activity)) { onConsentGathered(false); return }
        host = WeakReference(activity)
        if (gathering) { completions += onConsentGathered; return }
        if (updated || SystemClock.elapsedRealtime() < retryAt) {
            refreshPermission()
            onConsentGathered(runtime.value.ready)
            return
        }
        gathering = true
        completions += onConsentGathered
        val parameters = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(AdConfig.Audience.TAG_FOR_UNDER_AGE_OF_CONSENT).build()
        information.requestConsentInfoUpdate(activity, parameters, {
            refreshPermission()
            val activityHost = host.get()
            if (activityHost == null || !safe(activityHost)) {
                finish(false)
            } else {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activityHost) { error ->
                    if (error != null) AdLogger.w("ConsentManager", "Consent form: ${error.errorCode}: ${error.message}")
                    finish(error == null)
                }
            }
        }, { error ->
            AdLogger.w("ConsentManager", "Consent update: ${error.errorCode}: ${error.message}")
            finish(false)
        })
        // Google permits using UMP's previous-session permission while this update runs.
        refreshPermission()
    }

    private fun finish(success: Boolean) {
        gathering = false
        updated = success
        retryAt = if (success) 0L else SystemClock.elapsedRealtime() + 30_000L
        refreshPermission()
        val callbacks = completions.toList()
        completions.clear()
        callbacks.forEach { it(runtime.value.ready) }
    }

    private fun initializeSdkIfAllowed() {
        if (!AdConfig.isAdsEnabled || !allowed.value || initialized.value || sdkStarting) return
        sdkStarting = true
        MobileAds.setRequestConfiguration(MobileAds.getRequestConfiguration().toBuilder()
            .setTagForUnderAgeOfConsent(if (AdConfig.Audience.TAG_FOR_UNDER_AGE_OF_CONSENT)
                RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
            else RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE).build())
        scope.launch(Dispatchers.IO) {
            try {
                MobileAds.initialize(appContext) { status ->
                    scope.launch {
                        sdkStarting = false
                        initialized.value = true
                        publish()
                        AdLogger.d("ConsentManager", "SDK ready=${runtime.value.ready}; adapters=${status.adapterStatusMap}")
                    }
                }
            } catch (failure: Exception) {
                withContext(Dispatchers.Main.immediate) {
                    sdkStarting = false
                    AdLogger.w("ConsentManager", "SDK initialization failed: ${failure.message}")
                }
            }
        }
    }

    fun showPrivacyOptionsForm(activity: Activity, onComplete: () -> Unit) {
        if (!safe(activity) || gathering || privacyFormOpen) { onComplete(); return }
        privacyFormOpen = true
        publish(force = true)
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) AdLogger.w("ConsentManager", "Privacy options: ${error.errorCode}: ${error.message}")
            privacyFormOpen = false
            refreshPermission()
            onComplete()
        }
    }
}
