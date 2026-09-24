# AptiRise Firebase Integration Guide (Stage 2)

## Overview
AptiRise is architected for seamless future Firebase integration. In Stage 1, the app operates completely offline using `DemoRepository` and requires **no `google-services.json` file**.

---

## Step-by-Step Stage 2 Setup Instructions

1. **Firebase Project Creation**
   - Create a Firebase project named `AptiRise` in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android App with package name `com.example.aptirise` (or your final production application ID).
   - Register SHA-1 fingerprint for Google One Tap / Google Sign-In support.

2. **Configuration File**
   - Download the generated `google-services.json` file from Firebase Console.
   - Place `google-services.json` in the `app/` root directory (`E:/Android Project/AptiReady/app/google-services.json`).

3. **Gradle Plugins & BoM Dependencies**
   - Add Google Services plugin to `build.gradle.kts`:
     ```kotlin
     alias(libs.plugins.google.services) apply false
     ```
   - Apply Google Services plugin in `app/build.gradle.kts`:
     ```kotlin
     plugins {
         alias(libs.plugins.google.services)
     }
     ```
   - Add Firebase BoM to `app/build.gradle.kts`:
     ```kotlin
     implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
     implementation("com.google.firebase:firebase-auth-ktx")
     implementation("com.google.firebase:firebase-firestore-ktx")
     ```

4. **Cloud Firestore Schema**
   - `users/{userId}`: User profile metadata, email, creation date, current streak.
   - `users/{userId}/progress/{topicId}`: Topic accuracy, questions attempted, last practiced timestamp.
   - `question_banks/{topicId}/questions/{questionId}`: Question text, options, correct index, explanation, difficulty.
   - `test_results/{resultId}`: Full mock test attempts, section scores, duration spent.

5. **Security Rules Draft**
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
       match /question_banks/{document=**} {
         allow read: if request.auth != null;
         allow write: if false; // Admin only
       }
     }
   }
   ```
