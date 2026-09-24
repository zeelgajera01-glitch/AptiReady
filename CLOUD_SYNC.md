# AptiRise Optional Cloud Sync & Outbox Architecture (Stage 7)

## Overview
AptiRise provides **optional cloud backup & synchronization** for verified signed-in users. Cloud sync is **OFF by default** on each installation/account pairing. Turning sync OFF halts uploading and downloading without deleting existing cloud documents.

Local practice, mock tests, and offline learning remain 100% functional without an internet connection or cloud backup enabled.

---

## What Syncs vs. What Remains Local

| Syncs to Cloud (`users/{uid}/...`) | Remains Device-Local Only |
| :--- | :--- |
| Finalized practice attempt snapshots & scores | Unfinished practice sessions & active timers |
| Finalized mock-test attempt snapshots & scores | Unfinished mock test attempts & active timers |
| Question bookmarks & saved notes | App theme preferences (Light/Dark/System) |
| Daily challenge completion records | Ad frequency counters, cooldowns & rewarded unlocks |

---

## Room Outbox Pattern & Sync States

AptiRise implements an **idempotent local outbox pattern** in Room (`sync_outbox` table):

1. **Queueing:** When an attempt is finalized or a bookmark is saved, an outbox record is atomically inserted into `sync_outbox` for the active `ownerId`.
2. **WorkManager Worker:** `CloudSyncWorker` triggers on `NetworkType.CONNECTED` constraints.
3. **Execution:** Filters items strictly for the active verified `ownerId`. Uploads via trusted Callable Functions (`uploadAttempt`, `mutateBookmark`).
4. **Acknowledgement:** Upon server acknowledgement, outbox items are removed from the local database.

### Sync Status Lifecycle

- **`OFF`:** Cloud backup disabled by user in Profile settings.
- **`WAITING_FOR_VERIFICATION`:** User is signed in but email is unverified.
- **`PENDING`:** Local outbox contains unsynced changes.
- **`SYNCING`:** `CloudSyncWorker` is actively uploading or downloading.
- **`UP_TO_DATE`:** Local outbox is empty and server cursors are synced.
- **`OFFLINE`:** Network connection is unavailable.
- **`ERROR`:** Transient or permanent sync error (displayed in Profile status).
- **`DELETION_PENDING`:** Account deletion lock is active; sync is suspended.

---

## Cloud Firestore Schema

- `users/{uid}/attempts/{attemptId}`:
  - `schemaVersion`: `1`
  - `attemptId`: Globally unique attempt ID (reused from local creation)
  - `attemptType`: `"PRACTICE"` | `"MOCK_TEST"`
  - `topicId`: Referenced topic ID
  - `clientCompletionTime`: Long timestamp
  - `questionSnapshots`: Array of question snapshot objects
  - `earnedMarks` / `maxMarks` / `scorePercentage`: Recalculated server-side
  - `serverUploadTimestamp`: `FieldValue.serverTimestamp()`
- `users/{uid}/questionBookmarks/{questionId}`:
  - `questionId`: Referenced question ID
  - `active`: Boolean (`false` serves as deletion tombstone)
  - `serverRevision`: Integer version counter
  - `serverUpdateTimestamp`: `FieldValue.serverTimestamp()`
