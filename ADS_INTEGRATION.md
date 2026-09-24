# AptiRise Test Ad Integration & Placement Mapping

## Overview
AptiRise integrates **Google Mobile Ads SDK** (`23.6.0`) and **User Messaging Platform (UMP) SDK** (`3.1.0`) using official Google sample test Application IDs and format-specific test ad unit IDs.

All ad units are strictly configured in **TEST MODE** (`isAdsEnabled = BuildConfig.DEBUG`). No live publisher IDs, revenue tracking, or production ad requests are activated in release builds.

---

## Placement-to-Screen & Format Mapping (10 Slots)

| Placement Identifier | Format | Target Screen / Section | Threshold / Trigger Condition |
| :--- | :--- | :--- | :--- |
| **`HOME_NATIVE`** | Native Advanced | Home Feed (`fragment_home.xml`) | Displayed inline above practice shortcuts. |
| **`TOPIC_LIST_NATIVE`** | Native Advanced | Practice Topic List (`fragment_practice.xml`) | Embedded after 4th topic row (requires $\ge$ 4 topics). |
| **`TEST_CATALOG_NATIVE`** | Native Advanced | Test Catalogue (`fragment_test_catalog.xml`) | Embedded after 3rd test card (requires $\ge$ 3 test cards). |
| **`HISTORY_NATIVE`** | Native Advanced | Attempt History (`fragment_attempt_history.xml`) | Embedded after 5th history item (requires $\ge$ 5 completed attempts). |
| **`BOOKMARKS_NATIVE`** | Native Advanced | Bookmarks List (`fragment_bookmarks.xml`) | Embedded after 5th bookmark (requires $\ge$ 5 saved bookmarks). |
| **`FORMULA_LIBRARY_BANNER`** | Adaptive Banner | Topic Details (`fragment_topic_detail.xml`) | Anchored adaptive banner in Formula Preview section. |
| **`RESULT_SUMMARY_NATIVE`** | Native Advanced | Practice Result & Mock Test Result | Positioned below score card and primary actions. |
| **`PRACTICE_EXIT_INTERSTITIAL`** | Interstitial | Practice Result Exit (`"Return to Home"`) | Triggers on explicit result exit if frequency rules permit. |
| **`MOCK_EXIT_INTERSTITIAL`** | Interstitial | Mock Result Exit (`"Return to Tests"`) | Triggers on explicit result exit if frequency rules permit. |
| **`OPTIONAL_HINT_REWARDED`** | Rewarded Video | Untimed Topic Practice | Voluntary button; user earns 1 shortcut hint upon video completion. |

---

## Official Test Configuration Identifiers

- **Sample AdMob Application ID:** `ca-app-pub-3940256099942544~3347511713`
- **Test Native Ad Unit ID:** `ca-app-pub-3940256099942544/2247696110`
- **Test Adaptive Banner Ad Unit ID:** `ca-app-pub-3940256099942544/6300978111`
- **Test Interstitial Ad Unit ID:** `ca-app-pub-3940256099942544/1033173712`
- **Test Rewarded Video Ad Unit ID:** `ca-app-pub-3940256099942544/5224354917`

---

## UMP Consent & Privacy Policy

1. **Consent Request at Launch:** `ConsentManager` requests consent info updates on app launch using `UserMessagingPlatform.getConsentInformation(activity)`.
2. **Form Presentation:** If required, presents consent form via `UserMessagingPlatform.loadAndShowConsentFormIfRequired`.
3. **Privacy Choices:** Accessible under Profile screen via `"Privacy Choices"`. User can re-evaluate consent settings at any time.
4. **Child-Directed & Content Rating Settings:** Configured via `RequestConfiguration.Builder().setTagForChildDirectedTreatment(...)`.

---

## Shared Interstitial Frequency Policy & Cooldowns

All full-screen ad units (*Practice Exit Interstitial*, *Mock Exit Interstitial*, and *Rewarded Ads*) share centralized frequency controls in `AdSessionManager` and `FullScreenAdCoordinator`:

- **First Session Exclusion:** No interstitials during the first app-use session (`isFirstSession = true`).
- **Initial Usage Gate:** Minimum **2 minutes (120,000 ms)** of active foreground app usage in current session before first interstitial.
- **Cooldown Interval:** Minimum **3 minutes (180,000 ms)** between full-screen ad displays.
- **Session Cap:** Maximum **2 interstitials** per app-use session.
- **Daily Cap:** Maximum **4 interstitials** per device-local calendar date ("yyyy-MM-dd").
- **Attempt Cap:** Maximum 1 interstitial opportunity per completed practice or mock attempt.
- **Session Boundary:** 30 minutes in background resets app-use session tracking and sets `isFirstSession = false`.

---

## Resource Cleanup & Safe Lifecycle Handling

- `NativeAdLoader` destroys prior `NativeAd` objects (`currentNativeAd?.destroy()`) before rebinding or when the host view is detached.
- `BannerAdWrapper` registers `pause()`, `resume()`, and `destroy()` callbacks tied to host Fragment/Activity lifecycle.
- Containers collapse to `View.GONE` if ad requests fail or if list thresholds are not met.
- Interstitial callbacks execute navigation `onComplete()` exactly once upon dismissal or show failure, preventing UI lockups.

---

## Debug Tools & Diagnostics Screen

Access **Ad Placement Diagnostics** from the Profile screen (in debug builds only):
- Live status of Test Mode (`BuildConfig.DEBUG`) and UMP Consent Eligibility.
- Live shared session, daily, cooldown, and placement state counters.
- Interactive checklist for all 10 placements.
- Fixture Buttons (*"Generate 5 Sample Attempts"*, *"Generate 5 Sample Bookmarks"*) to test native ad insertion thresholds in list views.
