# AptiRise Ad Placement Strategy & Stage 2 Guidelines

## Overview
AptiRise specifies **10 logical ad placement slots** across the application. In Stage 1, all ad containers remain unused and set to `GONE` with no fake advertisements or network requests.

---

## 10 Planned Logical Ad Placements

1. **`HOME_NATIVE`** — Native inline card embedded within the Home feed above or below progress summary.
2. **`TOPIC_LIST_NATIVE`** — Native card embedded after every 5th item in the practice topic list.
3. **`TEST_CATALOG_NATIVE`** — Native card embedded within the mock test catalogue list.
4. **`HISTORY_NATIVE`** — Native card within the future attempt-history list (Stage 2).
5. **`BOOKMARKS_NATIVE`** — Native card within the future bookmarked questions list (Stage 2).
6. **`FORMULA_LIBRARY_BANNER`** — Adaptive banner anchor at the bottom of the formula/concept reference library.
7. **`RESULT_SUMMARY_NATIVE`** — Native ad displayed below the quiz/test score result summary.
8. **`PRACTICE_EXIT_INTERSTITIAL`** — Interstitial ad shown occasionally when exiting a completed practice session.
9. **`MOCK_EXIT_INTERSTITIAL`** — Interstitial ad shown occasionally when exiting a submitted full-length mock test.
10. **`OPTIONAL_HINT_REWARDED`** — Voluntary rewarded video ad giving a free hint during self-paced practice. Never shown during timed mock tests.

---

## Technical & Non-Intrusive Guidelines for Stage 2

* **Google Test Ad Units:** Always use official AdMob test ad unit IDs during development and QA.
* **Consent & Compliance:** Initialize Google User Messaging Platform (UMP) SDK for GDPR/Privacy consent and age-appropriate ad settings prior to requesting ads.
* **Shared Interstitial Cooldown:** Enforce a minimum 4-minute cooldown and a maximum session cap across both interstitial slots (`PRACTICE_EXIT_INTERSTITIAL` and `MOCK_EXIT_INTERSTITIAL`).
* **Protected Views:** No ads permitted on launch/splash, authentication, permission dialogs, or active timed test screens.
* **Back-to-Back Prevention:** Never serve back-to-back or simultaneous full-screen ad units.
* **Native Separation:** Native ads must be clearly labelled ("Ad" / "Sponsored") with distinct border styling separated from interactive navigation components.
* **Fail-Safe Operation:** Ad loading failures, timeouts, or network errors must fail silently without blocking navigation or quiz results.
* **Voluntary Rewards:** Rewarded hints must remain 100% optional; declining an ad must not restrict normal practice access.
* **Format Efficiency:** The 10 placements use shared reusable native, banner, interstitial, and rewarded ad loaders.
