# Safe Production Ad Execution Switch (`LIVE_ADS_RELEASE.md`)

This document describes how to safely build AptiRise for production with or without live AdMob ad serving enabled.

---

## 1. Default Release Build (Ads Disabled)

Running the standard release build command:

```bash
./gradlew :app:assembleRelease
```
or
```bash
./gradlew :app:bundleRelease
```

### Result:
- `BuildConfig.ADS_ENABLED` = `false`
- Live production AdMob ad requests are **STRICTLY DISABLED**.
- Real AdMob production Ad Unit IDs are compiled into `BuildConfig`, but no network ad requests are made to AdMob servers.

---

## 2. Explicit Production-Ad Release Build (Ads Enabled)

To explicitly enable live production ads for a production release, pass the `-PenableLiveAds=true` Gradle property:

```bash
./gradlew :app:assembleRelease -PenableLiveAds=true
```
or
```bash
./gradlew :app:bundleRelease -PenableLiveAds=true
```

### Result:
- `BuildConfig.ADS_ENABLED` = `true`
- Live production AdMob ad requests are **ENABLED** using your configured production Ad Unit IDs.

---

## 3. Mandatory Production Prerequisites Before Using `-PenableLiveAds=true`

> **WARNING**: Never use `-PenableLiveAds=true` for routine development, internal testing, or pre-launch QA builds.

Only build with `-PenableLiveAds=true` AFTER all of the following conditions are met:
1. The app is publicly listed on a supported app store (Google Play Store).
2. The AdMob App is linked to the live Google Play Store listing URL in AdMob Console.
3. Your `app-ads.txt` file is hosted on your developer website and verified by Google AdMob.
4. AdMob has completed and approved the **App Readiness Review**.
5. You have confirmed the app is ready for live production ad serving.

---

## 4. Debug Build Behavior

DEBUG builds **ALWAYS** use Google's official test ad unit IDs and `BuildConfig.ADS_ENABLED = true` regardless of whether `-PenableLiveAds` is specified.

```bash
./gradlew :app:assembleDebug
```

- `-PenableLiveAds` has **ZERO effect** on DEBUG builds.
- DEBUG builds will **NEVER** request or load live production ads.
