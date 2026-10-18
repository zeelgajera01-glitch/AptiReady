# AptiRise ad integration rebuild — 15 October 2026

Changes were made directly in this project. Existing local changes were retained.

## Preserved

- Package: com.aptirise.app; current version 1.1.2 (13).
- App ID: ca-app-pub-4263244815223132~5490032720.
- Production native: ca-app-pub-4263244815223132/9976072648.
- Production banner: ca-app-pub-4263244815223132/1518998127.
- Production interstitial: ca-app-pub-4263244815223132/4167458178.
- Production rewarded: ca-app-pub-4263244815223132/3765887514.
- Debug builds continue using Google's format-specific demo units.
- App screens, native placements, frequency caps, Firebase/signing configuration, question content and durable hint delivery.

## Rebuilt

- Consent orchestration uses UMP permission, updates once per successful launch, retries failed consent updates on resume, supports prior-session permission and starts Mobile Ads once off the UI thread.
- Consent readiness only changes when permission/readiness changes. Resuming the Activity no longer discards valid cached ads.
- Native, interstitial and rewarded requests now use AdLoadSlot: generation checks, one request at a time, one-hour expiry, one-time fullscreen consumption and recovery from requests with missing callbacks.
- Interstitial cache is keyed by unit/configuration, so practice and mock results can share their configured interstitial unit without needlessly throwing it away.
- Temporary load failures back off for 60, 120, 240, then 300 seconds. Further temporary failures continue at five-minute intervals while placement loading is active; invalid requests stop until cache/configuration reset.
- Rewarded retries run only after hint preparation has been requested and while the app is foregrounded. A loaded rewarded ad is shown only after the user's confirmation; the existing reward callback/durable delivery remains intact.
- Banner loading uses the view's context, measures available width, recovers from missing callbacks and resumes its timeout/retry after pause.
- Explicit internet, network-state and advertising-ID permissions were added to the manifest.
- Diagnostics now show the configured App ID, all four units and actual frequency settings.

## Reference files

The supplied SimpleAlarm archives/text were read as reference material. They describe reconstructed/decompiled code for another application using the Next-Gen SDK. Their setup instructions were not treated as user instructions. No SimpleAlarm IDs, Firebase configuration, app-open ads, alarm-specific state or remote navigation behavior were copied into AptiRise. AptiRise keeps one compatible Google Mobile Ads SDK integration.

## Remaining serving dependency

The pre-existing AD-SERVING-EVIDENCE.txt records production units returning code 3 (No fill), while demo native ads worked. Those are historical observations, not a new device test. Code cannot guarantee inventory or change AdMob account serving status. The same record mentions missing UMP message configuration; required privacy messages must be configured/published in AdMob for this App ID. No authenticated AdMob session is available in this task.

Official references:
- https://developers.google.com/admob/android/privacy
- https://developers.google.com/admob/android/ad-load-errors
- https://support.google.com/admob/answer/15090849

## Validation

- PASS: final build completed successfully with :app:assembleDebug :app:testDebugUnitTest :app:assembleRelease (Gradle 9.5.0).
- PASS: all 87 unit tests, including five new ad-cache regression tests and revised retry tests; zero failures/errors.
- PASS: release lint vital checks and release resource optimization.
- PASS: generated release configuration retains all production IDs and ADS_ENABLED=true; merged manifest contains ad/network permissions and the App ID metadata.
- PASS: no whitespace errors in the files changed for this ad rebuild.
- Outputs: app/build/outputs/apk/debug/app-debug.apk and app/build/outputs/apk/release/app-release.apk.
- NOT TESTED: live ad display and consent UI; no Android device was connected. Production fill and AdMob dashboard settings were not verified in this run.
- Non-blocking compiler warnings concern existing Google SDK APIs marked deprecated. The build succeeded with the installed SDK versions.

- PASS: signed release APK independently verified with Android apksigner (v2 signature, one signer).

## New device evidence — 15 October 2026, 08:54–08:55 IST

User supplied a fresh logcat from the installed APK. TEST_CATALOG_NATIVE requested the production native unit ca-app-pub-4263244815223132/9976072648 at 08:54:57.081 and received code 3 / No fill at 08:54:57.567. TOPIC_LIST_NATIVE requested the same production unit at 08:55:12.230 and received the same error at 08:55:12.596. Both failures were in com.google.android.gms.ads with Response ID null, Adapter Responses empty and mediation_group_name AdMob (default).

These requests show the installed APK reached the native request/load-failure callback. They do not establish why AdMob supplied no ad, whether an account restriction exists, or whether other ad formats work. The user reports demo ads working when launched from Android Studio; debug uses demo units whereas the release APK uses the production units. No further code changes were made based on this log because it provides no evidence of an APK-specific loading/rendering defect. The next investigation is current AdMob serving/app-unit status and request diagnostics for these production units.

## AdMob screenshots supplied after the new logcat

Observed in the supplied screenshots: AptiRise app status Ready; account Home says approved and ad serving enabled; Policy centre Issues says No current issues; Disapproved apps says No disapproved apps. The Home report for Last 7 days shows 560 requests, 141 impressions and 57.14% match rate, and the AptiRise app row shows 141 impressions. The app overview Today so far shows zero requests and impressions. The screenshots do not show the failing native unit's current detailed settings or an explanation for the specific no-fill responses. Reporting is not real-time: Google documents that most metrics are typically delayed by approximately four hours (https://support.google.com/admob/answer/13291097?hl=en). Therefore the today-zero figure does not contradict the newly captured device requests. The grey payment steps are described on-screen as future steps after reaching relevant earnings/verification milestones; no overdue verification or payment-related ad-serving hold is shown in these images.

## Native unit settings confirmed by supplied screenshot

AptiRise_Native is Native advanced, ID ca-app-pub-4263244815223132/9976072648, media type Image | Video, eCPM floor Google optimised (All prices). This matches the production unit in source and the fresh device log. No restrictive manually specified floor or media-type mismatch is visible. Combined with Ready/approved account status, no visible policy issues and repeated code-3 failures, these screenshots do not establish a remaining source-code fix or the exact server-side reason. A publisher-support investigation is appropriate; do not describe an account restriction as confirmed.
