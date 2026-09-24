package com.example.aptiready.data.model

/**
 * Exactly 10 planned ad placement logical slots for AptiRise.
 * In Stage 1, these containers remain unused and GONE to prevent empty spaces or active requests.
 */
enum class AdPlacement(val placementId: String, val description: String) {
    HOME_NATIVE("HOME_NATIVE", "Native ad within the Home feed."),
    TOPIC_LIST_NATIVE("TOPIC_LIST_NATIVE", "Native ad within the topic list."),
    TEST_CATALOG_NATIVE("TEST_CATALOG_NATIVE", "Native ad within the test catalogue."),
    HISTORY_NATIVE("HISTORY_NATIVE", "Native ad within the future attempt-history list."),
    BOOKMARKS_NATIVE("BOOKMARKS_NATIVE", "Native ad within the future bookmarks list."),
    FORMULA_LIBRARY_BANNER("FORMULA_LIBRARY_BANNER", "Adaptive banner on the future formula library."),
    RESULT_SUMMARY_NATIVE("RESULT_SUMMARY_NATIVE", "Native ad below a future result summary."),
    PRACTICE_EXIT_INTERSTITIAL("PRACTICE_EXIT_INTERSTITIAL", "Occasional interstitial after a completed practice session when leaving results."),
    MOCK_EXIT_INTERSTITIAL("MOCK_EXIT_INTERSTITIAL", "Occasional interstitial after a submitted mock test when leaving results."),
    OPTIONAL_HINT_REWARDED("OPTIONAL_HINT_REWARDED", "Voluntary rewarded ad for an extra practice hint, never during timed mock tests.")
}