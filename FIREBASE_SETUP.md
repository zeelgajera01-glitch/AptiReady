# AptiRise Firebase Setup & Production Deployment Guide (Stage 2)

## Overview
AptiRise uses **Firebase Authentication** (Email & Password) and **Cloud Firestore** (`users/{uid}`) for user sessions and secure profile management.

In Stage 2, the app includes **explicit local fallback mode**. If `google-services.json` is missing from `app/`, the app compiles cleanly, displays a clear configuration banner on authentication screens, and operates in local guest mode without fake logins or silent errors.

---

## Step 1: Firebase Project Creation & App Registration

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Create Project** and name it `AptiRise-Production` (or select an existing project).
3. Confirm your final Application ID:
   - Development Application ID: `com.example.aptiready`
   - Production Application ID placeholder: `com.example.aptirise`
4. Click **Add App** -> **Android**.
5. Enter the matching package name (e.g. `com.example.aptiready`).
6. Register SHA-1 fingerprint (required if using Google One Tap or App Check later).

---

## Step 2: Download & Install `google-services.json`

1. Download `google-services.json` from the Firebase Console.
2. Place the file inside the project's `app/` directory:
   `E:/Android Project/AptiReady/app/google-services.json`
3. Gradle automatically detects this file and applies the `com.google.gms.google-services` plugin at build time.

---

## Step 3: Enable Email/Password Authentication

1. In Firebase Console, navigate to **Build -> Authentication**.
2. Click **Get Started** -> **Sign-in method**.
3. Select **Email/Password**:
   - Enable **Email/Password**.
   - Leave Passwordless sign-in disabled for now.
4. Under **Settings -> Password Policy**:
   - Set minimum length to **6 characters** (matching Firebase Auth standard policy).
5. Customize Email Templates:
   - Go to **Templates -> Email address verification**. Customize sender name to `AptiRise Team`.
   - Go to **Templates -> Password reset**. Customize link branding.

---

## Step 4: Create Cloud Firestore Database

1. In Firebase Console, navigate to **Build -> Firestore Database**.
2. Click **Create Database**.
3. Select a location close to your user base (e.g., `asia-south1` or `us-central1`).
4. Select **Start in Production Mode** (restrictive rules).
5. Click **Enable**.

---

## Step 5: Deploy Security Rules

AptiRise includes deployable security rules in `firestore.rules` enforcing strict owner-only profile access, type checking, bounds validation, and server timestamps.

Deploy rules via Firebase CLI:
```bash
# Install Firebase CLI if needed
npm install -g firebase-tools

# Login to your Firebase account
firebase login

# Select or link your project
firebase use --add

# Deploy the security rules
firebase deploy --only firestore:rules
```

---

## Step 6: Running Firebase Local Emulators

For local testing without affecting production database records:

1. Start emulators:
   ```bash
   firebase emulators:start
   ```
2. The emulators expose:
   - Auth Emulator: `localhost:9099` (Android Emulator host: `10.0.2.2:9099`)
   - Firestore Emulator: `localhost:8080` (Android Emulator host: `10.0.2.2:8080`)
   - Emulator UI: `http://localhost:4000`

3. **Debug-Only Safety Rule:**
   Emulator connection code must be wrapped in `BuildConfig.DEBUG` checks so production release builds can **never** connect to local emulators:
   ```kotlin
   if (BuildConfig.DEBUG && USE_EMULATOR) {
       firebaseAuth.useEmulator("10.0.2.2", 9099)
       firestore.useEmulator("10.0.2.2", 8080)
   }
   ```

---

## Step 7: Publishing Checklist & Release Protection

Before publishing a release build to Google Play:

- Ensure `app/google-services.json` is present and matches the release package name.
- Verify `firestore.rules` have been deployed to your live Firebase project.
- Confirm `BuildConfig.DEBUG` is `false` so local emulator routing is disabled.
- Never upload service-account keys or API secrets to app assets or public repositories.
