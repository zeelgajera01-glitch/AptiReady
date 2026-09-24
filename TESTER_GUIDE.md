# AptiRise Tester & Reviewer Guide (Stage 9)

## Overview
This guide provides step-by-step instructions for QA testers and store reviewers to evaluate all core features of **AptiRise v1.0.0**.

---

## Key Testing Scenarios

### 1. Guest Mode & Offline Practice
1. Open the app directly after installation.
2. Select **Guest Mode** (no registration required).
3. Navigate to **Practice** tab $\rightarrow$ Select **Percentages** topic.
4. Answer 5 practice questions. Observe options, hints, and step-by-step explanations after answering.
5. Tap the **Bookmark** star icon on a question. Verify it appears in **Profile -> Saved Bookmarks**.

### 2. Timed Mock Test Engine & Timer Recovery
1. Navigate to **Tests** tab $\rightarrow$ Select **Quantitative Speed Sprint 01** (15 mins).
2. Read test instructions and tap **Start Test**.
3. Select answers for 3 questions and tap **Mark for Review** on Question 1.
4. Press the Home button or lock the screen for 30 seconds.
5. Re-open AptiRise. Verify that remaining time was correctly adjusted using monotonic clock recovery.
6. Tap **Submit Test** $\rightarrow$ Confirm submission.
7. Verify score breakdown card (Earned Marks, Accuracy %, Correct/Incorrect/Unanswered counts).

### 3. User Authentication & Profile
1. Navigate to **Profile** tab $\rightarrow$ Tap **Sign In or Register**.
2. Tap **Create Account**, enter email and password.
3. Observe **VERIFIED / UNVERIFIED** badge status.
4. Tap **Edit Profile**, change display name and daily target. Verify Firestore updates.

### 4. Optional Cloud Backup & Outbox Sync
1. In Profile Settings, toggle **Cloud Backup** to **ON**.
2. Complete a practice session. Observe the outbox queue status in Profile Settings (*"Status: Syncing..."* $\rightarrow$ *"Status: Up to date"*).
3. Sign in with the same account on a second device or emulator. Verify that completed attempts and bookmarks are available.

### 5. Account Deletion Workflow
1. Navigate to **Profile -> Delete Account**.
2. Read the permanent deletion impact warning.
3. Enter password to re-authenticate.
4. Tap **Permanently Delete My Account**.
5. Verify that cloud records are erased, local outbox is wiped, user is signed out, and app returns to the Welcome screen.

### 6. Test Ad Placements Audit
1. Verify native test ad cards in Home feed, Topic list, Test catalog, Attempt history, and Results screens.
2. Observe anchored adaptive banner in Topic Details formula section.
3. In untimed practice, tap **Watch Ad for Hint**. Confirm test rewarded video dialog and hint unlocking.
