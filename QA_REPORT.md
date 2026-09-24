# AptiRise Comprehensive QA Report & Verification (Stage 8 & Parts 1-5 Ad Integration)

## 1. Environment & Build Configuration

- **Application Name:** AptiRise
- **Application Package / Namespace:** `com.example.aptiready`
- **Gradle AGP Version:** `9.3.2`
- **Kotlin Version:** `2.1.10`
- **Compile SDK:** 35 | **Target SDK:** 35 | **Min SDK:** 24
- **Database Engine:** Room v3 (`aptirise_database.db` with migrations up to `MIGRATION_5_6`)
- **Key Dependencies:**
  - Google Mobile Ads SDK (`23.6.0`) & User Messaging Platform (`3.1.0`)
  - Firebase BoM (`33.9.0`): Auth, Firestore, Functions
  - Jetpack Navigation (`2.8.5`), DataStore (`1.1.1`), WorkManager (`2.10.0`)
- **Ad Environment:** Strict **Google Test Ads** mode enabled (`isAdsEnabled = BuildConfig.DEBUG`). Release builds remain strictly disabled.

---

## 2. Feature Classification & Status

| Stage / Module | Feature Area | Status | Classification Details |
| :-: | :--- | :--- | :--- |
| **1** | Architecture, Single Activity, Navigation & Material 3 Theme | **VERIFIED** | Single `MainActivity` with 5-tab `BottomNavigationView`. Light, Dark, and System theme switching via DataStore. |
| **2** | Auth, Firestore Profiles & Safe Guest Fallback | **VERIFIED** | Firebase Auth (Email/Password, Email Verification, Password Reset). Local Guest Mode activates when `google-services.json` is absent. |
| **3** | Starter Content, Offline Room DB & Untimed Practice Engine | **VERIFIED** | 1200 starter questions across 10 topics in Room. Untimed practice engine, answer review, bookmarks, step-by-step explanations. |
| **4** | Timed Mock Tests & Monotonic Clock Timer Engine | **VERIFIED** | 3 bundled mock tests (`starter_tests.json`). Monotonic timer recovery (`SystemClock.elapsedRealtime()`), question navigator grid, auto-submit on expiry. |
| **5** | Unified Attempt History & Real Progress Dashboard | **VERIFIED** | Unified Practice & Mock attempt history with filter chips. Real progress dashboard computing weighted accuracy % and total attempts. |
| **6-10 (Parts 1-5)** | Complete Development-Mode Ad Integration & Consent Flow | **VERIFIED** | 10 logical ad placements in `AdConfig.kt`. UMP consent integration, shared frequency limits (120s foreground usage, 180s cooldown, 2/session, 4/day), voluntary rewarded extra hint. |
| **Cloud Sync** | Optional Cloud Backup/Sync & Durable Account Erasure | **VERIFIED** | Room Outbox (`sync_outbox`), `CloudSyncWorker` WorkManager job, idempotent TypeScript callable functions, re-authentication password requirement, cascading erasure. |
| **Testing** | Automated Testing, Defect Auditing & Polish | **VERIFIED** | **80 unit tests passing (100% pass rate)** across 6 test suites. Zero compiler errors. |

---

## 3. Automated Test Outcome

- **Gradle Build Task:** `./gradlew :app:assembleDebug` $\rightarrow$ **BUILD SUCCESSFUL**
- **Unit Test Task:** `./gradlew :app:testDebugUnitTest` $\rightarrow$ **80 Passed, 0 Failed, 0 Skipped (100% Pass Rate)**

### Test Suites Summary
1. **`PracticeSessionTest.kt`:** Practice session creation, option selection, score calculation, bookmark toggling.
2. **`MockTestTimerAndScoringTest.kt`:** Timer recovery logic, monotonic clock difference vs reboot wall-clock fallback, auto-expiry, marked-for-review scoring.
3. **`AdPolicyTest.kt`:** Native ad list threshold checks ($\ge$ 4 topics, $\ge$ 3 tests, $\ge$ 5 history items, $\ge$ 5 bookmarks), interstitial 120s foreground usage gate, 30m session timeout, 180s cooldowns & session/daily caps, clock rollback handling, consent invalidation, single-tap navigation.
4. **`ListAdPresentationTest.kt`:** Presentation row construction thresholds and dynamic filtering removal.
5. **`RewardedHintTest.kt`:** Free hint preservation, extra hint reward context binding, SDK earned callback trigger, idempotency, and release ad disabling.
6. **`CloudSyncAndDeletionTest.kt`:** Outbox entity creation, sync status display names, payload size validation (< 500 KB), user isolation, deletion re-authentication timestamp check (< 15 mins).

---

## 4. Ten Logical Ad Placements Audit

All 10 ad placements are configured strictly with **Google official test ad unit IDs**:

1. **`HOME_NATIVE`:** Native card in Home feed.
2. **`TOPIC_LIST_NATIVE`:** Embedded in topic list after 4th topic row.
3. **`TEST_CATALOG_NATIVE`:** Embedded in test catalogue after 3rd test card.
4. **`HISTORY_NATIVE`:** Embedded in attempt history after 5th attempt item.
5. **`BOOKMARKS_NATIVE`:** Embedded in bookmarks list after 5th bookmark item.
6. **`FORMULA_LIBRARY_BANNER`:** Anchored adaptive banner in Topic Details formula card.
7. **`RESULT_SUMMARY_NATIVE`:** Native card below score summary on Practice & Mock result screens.
8. **`PRACTICE_EXIT_INTERSTITIAL`:** Triggered on *"Return to Home"* from practice result if frequency rules permit.
9. **`MOCK_EXIT_INTERSTITIAL`:** Triggered on *"Return to Tests"* from mock result if frequency rules permit.
10. **`OPTIONAL_HINT_REWARDED`:** Voluntary extra hint button in untimed topic practice; grants 1 shortcut hint on video completion via SDK earned callback.

---

## 5. Offline Durability & Security Audit

- **Offline Independence:** App launches and allows full untimed practice, formula review, and timed mock tests without network or Firebase dependencies.
- **Account Isolation:** Outbox items, practice history, mock attempts, and bookmarks are strictly scoped by `ownerId`. Guest data is never automatically merged into a signed-in account.
- **Firestore Security Rules:** Private subcollections (`users/{uid}/...`) enforce `request.auth.uid == uid` and `request.auth.token.email_verified == true`. Direct client writes are denied; mutations route through trusted Callable Functions.

---

## 6. External Configuration Blockers & Manual Checks Needed

1. **Live Firebase Deployment:** Deploying production Cloud Functions and Firestore Security Rules requires executing `firebase deploy` via Firebase CLI with an active Firebase project.
2. **AdMob Production Ad Units:** Transitioning from test ads to live ads in release will require registering a production AdMob account and configuring live ad unit IDs.
