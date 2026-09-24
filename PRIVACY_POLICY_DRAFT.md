# AptiRise Privacy Policy Draft (Stage 9)

**Effective Date:** September 19, 2026

## 1. Introduction
AptiRise ("we", "our", or "us") respects your privacy. This Privacy Policy describes how AptiRise collects, uses, and protects your information when you use our Android application.

---

## 2. Information We Collect

### A. Local Device Data
AptiRise is designed to function offline. Practice session snapshots, mock test progress, theme preferences, and local bookmarks are stored locally on your device using Android Room Database and DataStore preferences.

### B. Account & Profile Information (Optional Signed-In Users)
When you register for an account using Firebase Authentication, we collect:
- **Email Address:** Used for account creation, email verification, and password resets.
- **Display Name & Daily Learning Goal:** Stored in Cloud Firestore (`users/{uid}`) to customize your experience.

### C. Learning Records (Optional Cloud Sync)
If you explicitly enable Cloud Backup in Profile Settings, your completed practice attempt scores, mock test results, and question bookmarks are synchronized with Cloud Firestore (`users/{uid}/...`). Cloud backup is **OFF by default**.

### D. Advertising SDK Data (Google Mobile Ads)
AptiRise integrates the Google Mobile Ads SDK and User Messaging Platform (UMP) SDK to serve advertisements. In test mode, Google may process IP addresses, coarse location, device identifiers, and ad interaction data in accordance with Google's Privacy Policy.

---

## 3. How We Use Information
We use collected data solely to:
- Provide aptitude learning, timed self-assessments, and progress tracking.
- Manage user authentication and account security.
- Synchronize your learning progress across your verified devices (when cloud backup is enabled).
- Serve relevant test advertisements in compliance with user consent choices.

---

## 4. Account & Data Deletion
You have complete control over your personal data:
- **In-App Account Erasure:** You can permanently delete your account and all associated cloud learning records directly within the app via **Profile Settings -> Delete Account**.
- **External Deletion Requests:** Users who cannot access the app may submit an account erasure request by emailing our privacy team at `privacy@aptirise-placeholder.com`.

---

## 5. Third-Party Services
AptiRise integrates the following Google Cloud and Firebase services:
- **Firebase Authentication:** Account sign-in and security tokens.
- **Cloud Firestore & Cloud Functions:** Optional cloud backup and account deletion execution.
- **Google Mobile Ads & UMP SDK:** Consent management and test ad serving.

---

## 6. Contact Us
If you have questions regarding this Privacy Policy, please contact us at:
`support@aptirise-placeholder.com`
