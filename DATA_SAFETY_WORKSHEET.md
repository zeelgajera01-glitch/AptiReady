# AptiRise Google Play Data Safety Worksheet (Stage 9)

## Google Play Data Safety Declarations

| Data Category | Data Type | Collected? | Shared? | Purpose | Optional / Mandatory |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Personal Info** | Email Address | Yes | No | Account Management, Authentication | Mandatory for signed-in users (Guest mode available) |
| **Personal Info** | Name / Display Name | Yes | No | App Functionality, Personalization | Mandatory for signed-in users |
| **App Activity** | User Interaction (Practice scores, test completions) | Yes | No | App Functionality, Progress Tracking | Optional (stored locally; synced only when Cloud Backup is ON) |
| **App Info & Performance** | Diagnostics / Crash Data | No | No | N/A | Not collected in current release |
| **Device or Other IDs** | Device / Advertising Identifiers | Yes | Yes (Google Mobile Ads) | Advertising, Consent Management | Mandatory for ad serving (Google Mobile Ads SDK) |

---

## Security Practices Declarations

1. **Data Encrypted in Transit?**
   - **YES.** All network traffic to Firebase Auth, Cloud Firestore, and Google Mobile Ads SDK uses HTTPS/TLS encryption.

2. **Data Deletion Mechanism Provided?**
   - **YES.** Users can request permanent deletion of their account and associated data directly in-app via **Profile -> Delete Account**, or externally via support email.

3. **Target Audience & COPPA Declaration:**
   - App is targeted at users **13 and older** (High school, college students, job seekers).
   - `RequestConfiguration` sets child-directed treatment to unspecified.
