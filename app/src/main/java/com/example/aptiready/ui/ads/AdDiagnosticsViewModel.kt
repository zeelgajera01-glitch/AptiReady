package com.example.aptiready.ui.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aptiready.BuildConfig
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.PracticeRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdDiagnosticsViewModel(
    private val practiceRepository: PracticeRepository,
    private val authRepository: AuthRepository,
    val sessionManager: AdSessionManager,
    val consentManager: ConsentManager
) : ViewModel() {

    val isDebugBuild: Boolean = BuildConfig.DEBUG
    val currentOwnerId: String get() = authRepository.currentUserId ?: "guest"

    val canRequestAds: StateFlow<Boolean> = consentManager.canRequestAds
    val isPrivacyOptionsRequired: StateFlow<Boolean> = consentManager.isPrivacyOptionsRequired

    fun getDiagnosticSummary(): String {
        sessionManager.init()
        val fgSecs = sessionManager.getAccumulatedForegroundUsageMs() / 1000
        val cdSecs = sessionManager.getTimeSinceLastAdMs().let { if (it == Long.MAX_VALUE) -1 else it / 1000 }
        val cdText = if (cdSecs < 0) "None (Ready)" else "${cdSecs}s / 180s"

        return """
            App ID: ${AdConfig.TEST_APP_ID}
            Debug Build (Test Mode): $isDebugBuild
            Global Ads Enabled: ${AdConfig.isAdsEnabled}
            UMP Consent Eligible: ${canRequestAds.value}
            SDK Initialized: ${consentManager.isInitialized.value}
            Requests Ready: ${consentManager.adState.value.ready}
            Privacy Options Required: ${isPrivacyOptionsRequired.value}
            First Session Active: ${sessionManager.isFirstSession}
            Session Interstitials: ${sessionManager.sessionInterstitialCount} / ${AdConfig.FrequencyPolicy.MAX_INTERSTITIALS_PER_SESSION}
            Daily Interstitials: ${sessionManager.dailyInterstitialCount} / ${AdConfig.FrequencyPolicy.MAX_INTERSTITIALS_PER_DAY}
            Calendar Date Key: '${sessionManager.currentDateKey}'
            Foreground Usage: ${fgSecs}s / 120s
            Active Cooldown: $cdText
        """.trimIndent()
    }

    fun getPlacementsChecklist(): String {
        val sb = StringBuilder()
        AdPlacement.entries.forEachIndexed { idx, placement ->
            val enabled = AdConfig.isPlacementEnabled(placement)
            sb.append("${idx + 1}. ${placement.name}: ${if (enabled) "ENABLED" else "DISABLED"}\n")
        }
        return sb.toString().trimEnd()
    }

    class Factory(
        private val practiceRepository: PracticeRepository,
        private val authRepository: AuthRepository,
        private val sessionManager: AdSessionManager,
        private val consentManager: ConsentManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdDiagnosticsViewModel(practiceRepository, authRepository, sessionManager, consentManager) as T
        }
    }
}
