# AptiRise Account & Cloud Data Deletion Guide (Stage 7)

## Overview
AptiRise implements a **secure, durable account and data erasure pipeline** complying with privacy regulations (GDPR / CCPA) and Google Play policy.

Users can initiate account deletion directly from **Profile Settings -> Delete Account**.

---

## Account Deletion User Experience & Safeguards

1. **Explicit Impact Explanation:**
   The `AccountDeletionFragment` UI clearly states:
   - Profile, cloud attempts, bookmarks, and saved notes will be permanently erased.
   - This device's local outbox and account records will be wiped immediately.
   - Other offline devices cannot be wiped until they reconnect to the internet.
   - Public learning content and separate local guest records remain unaffected.
   - Once accepted by the server, deletion **cannot be undone**.

2. **Re-Authentication Enforcement:**
   - The user must re-enter their current account password.
   - The app executes `reauthenticateWithCredential`.
   - The server verifies that the ID token's `auth_time` was issued within the last **15 minutes (900 seconds)** before accepting the request.

---

## Server Deletion Pipeline & Lifecycle Lock

1. **Lifecycle Lock (`accountLifecycle/{uid}`):**
   - The Cloud Function `requestAccountDeletion` atomically sets `accountLifecycle/{uid}` status to `"DELETION_PENDING"`.
   - While `DELETION_PENDING` is active, all sync functions (`uploadAttempt`, `mutateBookmark`) and Firestore rules reject any incoming read/write requests.

2. **Cascading Erasure:**
   - Deletes all documents in subcollections `users/{uid}/attempts`, `users/{uid}/questionBookmarks`, and `users/{uid}/savedNotes`.
   - Deletes document `users/{uid}`.
   - Calls Firebase Admin Auth `admin.auth().deleteUser(uid)`.
   - Sets `accountLifecycle/{uid}` status to `"DELETION_COMPLETED"`.

3. **Local Android Device Cleanup:**
   - Clears local `sync_outbox` items for the deleted `ownerId`.
   - Clears local completed attempts & bookmarks for that `ownerId` in Room.
   - Disables cloud backup toggle in `SyncPreferencesRepository`.
   - Signs out of Firebase Auth and navigates back to the Welcome screen.
