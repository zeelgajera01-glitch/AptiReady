"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.requestAccountDeletion = exports.mutateBookmark = exports.uploadAttempt = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();
/**
 * 1. Upload Attempt (Idempotent Trusted Call)
 */
exports.uploadAttempt = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated.');
    }
    const uid = context.auth.uid;
    const isEmailVerified = context.auth.token.email_verified === true;
    if (!isEmailVerified) {
        throw new functions.https.HttpsError('failed-precondition', 'Email verification is required for cloud sync.');
    }
    // Check Deletion Lock
    const lifecycleDoc = await db.collection('accountLifecycle').doc(uid).get();
    if (lifecycleDoc.exists && lifecycleDoc.data()?.status === 'DELETION_PENDING') {
        throw new functions.https.HttpsError('unavailable', 'Account is currently undergoing deletion.');
    }
    const { attemptId, attemptType, topicId, clientCompletionTime, questionSnapshots, durationSeconds, marksPerCorrect } = data;
    if (!attemptId || !topicId || !Array.isArray(questionSnapshots)) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing or invalid attempt fields.');
    }
    // Validate Payload Size (< 500 KB)
    const payloadSize = JSON.stringify(data).length;
    if (payloadSize > 500000) {
        throw new functions.https.HttpsError('invalid-argument', 'Payload exceeds max allowed document size.');
    }
    const attemptRef = db.collection('users').doc(uid).collection('attempts').doc(attemptId);
    const existingDoc = await attemptRef.get();
    if (existingDoc.exists) {
        // Idempotent success if same payload
        return { status: 'ACKNOWLEDGED', attemptId: attemptId, isDuplicate: true };
    }
    // Recalculate personal score server-side
    let correctCount = 0;
    questionSnapshots.forEach((snap) => {
        if (snap.selectedOptionId && snap.selectedOptionId === snap.correctOptionId) {
            correctCount++;
        }
    });
    const totalQuestions = questionSnapshots.length;
    const unitMarks = marksPerCorrect || 1;
    const earnedMarks = correctCount * unitMarks;
    const maxMarks = totalQuestions * unitMarks;
    const scorePercentage = maxMarks > 0 ? (earnedMarks / maxMarks) * 100 : 0;
    const attemptDoc = {
        schemaVersion: 1,
        attemptId: attemptId,
        attemptType: attemptType || 'PRACTICE',
        topicId: topicId,
        clientCompletionTime: clientCompletionTime || Date.now(),
        durationSeconds: durationSeconds || 0,
        marksPerCorrect: unitMarks,
        earnedMarks: earnedMarks,
        maxMarks: maxMarks,
        scorePercentage: scorePercentage,
        questionSnapshots: questionSnapshots,
        serverUploadTimestamp: admin.firestore.FieldValue.serverTimestamp()
    };
    await attemptRef.set(attemptDoc);
    return { status: 'SUCCESS', attemptId: attemptId, earnedMarks: earnedMarks };
});
/**
 * 2. Mutate Bookmark (Conflict Detection & Tombstones)
 */
exports.mutateBookmark = functions.https.onCall(async (data, context) => {
    if (!context.auth || context.auth.token.email_verified !== true) {
        throw new functions.https.HttpsError('unauthenticated', 'Authenticated & verified account required.');
    }
    const uid = context.auth.uid;
    const { questionId, active, topicTitle, questionText, optionsJson, correctOptionId, explanation, hint } = data;
    if (!questionId) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing questionId.');
    }
    const bookmarkRef = db.collection('users').doc(uid).collection('questionBookmarks').doc(questionId);
    return db.runTransaction(async (transaction) => {
        const doc = await transaction.get(bookmarkRef);
        let currentRevision = 0;
        if (doc.exists) {
            currentRevision = doc.data()?.serverRevision || 0;
        }
        const nextRevision = currentRevision + 1;
        const bookmarkData = {
            questionId: questionId,
            active: active === true,
            topicTitle: topicTitle || '',
            questionText: questionText || '',
            optionsJson: optionsJson || '[]',
            correctOptionId: correctOptionId || '',
            explanation: explanation || '',
            hint: hint || '',
            serverRevision: nextRevision,
            serverUpdateTimestamp: admin.firestore.FieldValue.serverTimestamp()
        };
        transaction.set(bookmarkRef, bookmarkData, { merge: true });
        return { status: 'SUCCESS', questionId: questionId, newRevision: nextRevision };
    });
});
/**
 * 3. Request Account Deletion (Re-authentication & Wiping)
 */
exports.requestAccountDeletion = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated to request account deletion.');
    }
    const uid = context.auth.uid;
    const authTime = context.auth.token.auth_time; // Timestamp in seconds
    const nowInSeconds = Math.floor(Date.now() / 1000);
    // Require Recent Authentication (< 15 Minutes = 900 seconds)
    if (!authTime || (nowInSeconds - authTime) > 900) {
        throw new functions.https.HttpsError('unauthenticated', 'Recent re-authentication is required to delete your account.');
    }
    const lifecycleRef = db.collection('accountLifecycle').doc(uid);
    // Set Deletion Lock
    await lifecycleRef.set({
        uid: uid,
        status: 'DELETION_PENDING',
        requestedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
    try {
        // Delete Subcollections (attempts & questionBookmarks)
        const attemptsSnap = await db.collection('users').doc(uid).collection('attempts').get();
        const attemptDeletions = attemptsSnap.docs.map(doc => doc.ref.delete());
        await Promise.all(attemptDeletions);
        const bookmarksSnap = await db.collection('users').doc(uid).collection('questionBookmarks').get();
        const bookmarkDeletions = bookmarksSnap.docs.map(doc => doc.ref.delete());
        await Promise.all(bookmarkDeletions);
        // Delete User Profile
        await db.collection('users').doc(uid).delete();
        // Delete Firebase Auth Account
        await admin.auth().deleteUser(uid);
        // Mark Lifecycle Completed
        await lifecycleRef.set({
            status: 'DELETION_COMPLETED',
            completedAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        return { status: 'ACCEPTED', message: 'Account and associated cloud data successfully deleted.' };
    }
    catch (err) {
        await lifecycleRef.set({
            status: 'DELETION_FAILED',
            error: err.message || 'Unknown error during deletion.'
        }, { merge: true });
        throw new functions.https.HttpsError('internal', 'Account deletion process encountered an error: ' + err.message);
    }
});
//# sourceMappingURL=index.js.map