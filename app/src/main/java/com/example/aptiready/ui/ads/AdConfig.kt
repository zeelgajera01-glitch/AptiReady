package com.example.aptiready.ui.ads

import com.example.aptiready.BuildConfig
import com.example.aptiready.data.model.AdPlacement

object AdConfig {

    // Build-level eligibility plus the existing runtime switch.
    private val changes = kotlinx.coroutines.flow.MutableStateFlow(0L)

    val revision: kotlinx.coroutines.flow.StateFlow<Long> =
        changes

    private var requestedEnabled = true

    var isAdsEnabled: Boolean
        get() = BuildConfig.ADS_ENABLED && requestedEnabled
        set(value) {
            if (requestedEnabled == value) return

            requestedEnabled = value
            changes.value += 1
        }


    // Real AdMob Application ID for AptiRise
    const val REAL_APP_ID = "ca-app-pub-4263244815223132~5490032720"

    // Official Google Test AdMob Application ID
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    // Official Format-Specific Test Ad Unit IDs from Google Documentation
    const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Per-Placement Enabled Flags
    private val placementEnabledMap = mutableMapOf<AdPlacement, Boolean>().apply {
        AdPlacement.entries.forEach { placement -> put(placement, true) }
    }

    fun isPlacementEnabled(placement: AdPlacement): Boolean {
        return isAdsEnabled && (placementEnabledMap[placement] ?: true)
    }

    fun setPlacementEnabled(placement: AdPlacement, enabled: Boolean) {
        placementEnabledMap[placement] = enabled
        changes.value += 1
    }

    fun getAdUnitIdForPlacement(placement: AdPlacement): String {
        return when (placement) {
            AdPlacement.FORMULA_LIBRARY_BANNER -> BuildConfig.BANNER_AD_UNIT_ID
            AdPlacement.PRACTICE_EXIT_INTERSTITIAL,
            AdPlacement.MOCK_EXIT_INTERSTITIAL -> BuildConfig.INTERSTITIAL_AD_UNIT_ID
            AdPlacement.OPTIONAL_HINT_REWARDED -> BuildConfig.REWARDED_AD_UNIT_ID
            else -> BuildConfig.NATIVE_AD_UNIT_ID
        }
    }

    // Shared Frequency Policy Thresholds
    object FrequencyPolicy {
        const val FIRST_SESSION_INTERSTITIAL_ALLOWED = true
        const val MIN_FOREGROUND_USAGE_MS = 0L // 0s minimum usage
        const val MIN_INTERSTITIAL_COOLDOWN_MS = 15_000L // 15 seconds
        const val MAX_INTERSTITIALS_PER_SESSION = 5
        const val MAX_INTERSTITIALS_PER_DAY = 10
        const val SESSION_TIMEOUT_MS = 1_800_000L // 30 minutes in background resets session
        const val AD_EXPIRE_DURATION_MS = 3_600_000L // 1-hour ad caching limit
    }

    // Audience & Policy Configuration
    object Audience {
        // Tag for users under the age of consent (TFUA).
        const val TAG_FOR_UNDER_AGE_OF_CONSENT = false
    }

    // List Insertion Thresholds
    object ListThresholds {
        const val MIN_TOPICS_FOR_NATIVE_AD = 4
        const val MIN_TESTS_FOR_NATIVE_AD = 3
        const val MIN_HISTORY_FOR_NATIVE_AD = 5
        const val MIN_BOOKMARKS_FOR_NATIVE_AD = 5
    }

    /**
     * Validates that the active build is using the correct AdMob IDs.
     *
     * DEBUG:
     * - Must use Google's official test ad unit IDs.
     *
     * RELEASE:
     * - Must use AptiRise's production ad unit IDs.
     *
     * ADS_ENABLED remains controlled by the existing Gradle property.
     */
    fun validateAdMobConfiguration() {
        val isDebug = BuildConfig.DEBUG

        val expectedNativeId = if (isDebug) {
            TEST_NATIVE_AD_UNIT_ID
        } else {
            "ca-app-pub-4263244815223132/9976072648"
        }

        val expectedBannerId = if (isDebug) {
            TEST_BANNER_AD_UNIT_ID
        } else {
            "ca-app-pub-4263244815223132/1518998127"
        }

        val expectedInterstitialId = if (isDebug) {
            TEST_INTERSTITIAL_AD_UNIT_ID
        } else {
            "ca-app-pub-4263244815223132/4167458178"
        }

        val expectedRewardedId = if (isDebug) {
            TEST_REWARDED_AD_UNIT_ID
        } else {
            "ca-app-pub-4263244815223132/3765887514"
        }

        check(BuildConfig.ADMOB_APP_ID == REAL_APP_ID) {
            "Incorrect AdMob App ID for ${BuildConfig.BUILD_TYPE}"
        }

        check(BuildConfig.NATIVE_AD_UNIT_ID == expectedNativeId) {
            "Incorrect Native Ad Unit ID for ${BuildConfig.BUILD_TYPE}"
        }

        check(BuildConfig.BANNER_AD_UNIT_ID == expectedBannerId) {
            "Incorrect Banner Ad Unit ID for ${BuildConfig.BUILD_TYPE}"
        }

        check(BuildConfig.INTERSTITIAL_AD_UNIT_ID == expectedInterstitialId) {
            "Incorrect Interstitial Ad Unit ID for ${BuildConfig.BUILD_TYPE}"
        }

        check(BuildConfig.REWARDED_AD_UNIT_ID == expectedRewardedId) {
            "Incorrect Rewarded Ad Unit ID for ${BuildConfig.BUILD_TYPE}"
        }

        if (isDebug) {
            check(BuildConfig.ADS_ENABLED) {
                "DEBUG builds must keep ADS_ENABLED=true for test ads"
            }
        }
    }
}
