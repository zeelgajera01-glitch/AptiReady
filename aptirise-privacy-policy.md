# AptiRise Privacy Policy

**Effective Date:** September 24, 2026  
**Application Name:** AptiRise  
**Application Package:** `com.aptirise.app`  

---

## 1. Introduction
Welcome to **AptiRise**. We respect your personal privacy and are committed to protecting the data you share with us. This Privacy Policy explains how AptiRise ("we", "our", or "us") collects, uses, stores, and discloses information when you use our Android mobile application and associated services.

By installing or using AptiRise, you agree to the collection and use of information in accordance with this policy.

---

## 2. Information We Collect

### A. Account & Profile Information
When you register for an account or sign in using Firebase Authentication, we collect:
- **Email Address:** Used for account registration, sign-in authentication, email verification, and password recovery.
- **Display Name:** Used to personalize your profile and progress reports within the app.
- **Daily Learning Target:** Stored in Cloud Firestore (`users/{uid}`) to customize your daily question recommendations.

### B. Local Practice & Test Progress
AptiRise stores learning data on your device using Android Room Database and DataStore Preferences, including:
- Practice session attempts, correct/incorrect responses, and timestamps.
- Mock test scores and question completion history.
- Saved question bookmarks and custom app appearance/theme settings.

### C. Cloud Sync & Backup Data
If you enable Cloud Backup in Profile Settings, your practice attempts, mock test results, and question bookmarks are securely synchronized with Google Cloud Firestore (`users/{uid}/...`). Cloud sync can be toggled on or off at any time.

### D. Device & Advertising SDK Identifiers
AptiRise integrates the **Google Mobile Ads SDK** and **User Messaging Platform (UMP) SDK** to manage consent and deliver advertisements:
- **Device Identifiers & IP Address:** Processed by Google Mobile Ads to serve ads, prevent fraud, and enforce frequency limits.
- **Consent Choices:** Collected via the UMP consent form to record your ad personalization choices in compliance with GDPR and Google Play policy.

---

## 3. How We Use Your Information
We use the collected information exclusively for the following purposes:
- **Core Functionality:** To provide aptitude learning modules, timed mock tests, and answer explanations.
- **Progress Syncing:** To synchronize your learning progress and bookmarks across your verified devices.
- **Account Management:** To manage user authentication, verify email addresses, and handle secure password resets.
- **Monetization & Ad Serving:** To serve ads in compliance with your consent choices and regional regulations.
- **App Security:** To prevent fraudulent activity and protect user data integrity.

---

## 4. Third-Party Services
AptiRise integrates trusted third-party SDKs provided by Google LLC:
- **Firebase Authentication:** Secure user authentication and token handling.
- **Cloud Firestore & Cloud Functions:** Secure cloud database storage and serverless account cleanup routines.
- **Google Mobile Ads / AdMob & UMP SDK:** Privacy consent management and ad serving.

For more information on how Google uses data, please review [Google's Privacy Policy](https://policies.google.com/privacy).

---

## 5. Account & Data Deletion
You have complete control over your personal data:
- **In-App Account Deletion:** You can permanently delete your account and all associated cloud data at any time inside the app by going to **Profile -> Cloud Sync & Backup -> Delete Account**.
- **Data Erasure Scope:** Deleting your account permanently erases your profile document, cloud attempts, bookmarks, and sync outbox from Cloud Firestore.
- **Support Requests:** If you cannot access the app, you may request manual account deletion by contacting support at `zeelgajera010@gmail.com`.

---

## 6. Data Security
We implement industry-standard security measures to safeguard your information:
- All data in transit between AptiRise and Firebase services is encrypted using HTTPS / TLS encryption.
- Cloud Firestore security rules strictly enforce user data isolation (`request.auth.uid == userId`).

---

## 7. Children's Privacy
AptiRise does not knowingly collect personal identifiable information from children under 13. If you believe a child has provided us with personal data, please contact us so we can immediately delete the information.

---

## 8. Changes to This Privacy Policy
We may update our Privacy Policy periodically. Any changes will be posted on this page with an updated "Effective Date".

---

## 9. Contact Us
If you have any questions or suggestions regarding this Privacy Policy, please contact us:
- **Email:** `zeelgajera010@gmail.com`
