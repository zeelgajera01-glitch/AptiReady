import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

admin.initializeApp();
const db = admin.firestore();

/**
 * Delete the account and any existing server records after recent authentication.
 */
export const requestAccountDeletion = functions.https.onCall(async (data, context) => {
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
  } catch (err: any) {
    await lifecycleRef.set({
      status: 'DELETION_FAILED',
      error: err.message || 'Unknown error during deletion.'
    }, { merge: true });

    throw new functions.https.HttpsError('internal', 'Account deletion process encountered an error: ' + err.message);
  }
});

/**
 * 4. Send Email OTP (Generate & Dispatch 6-digit Code)
 */
export const sendEmailOtp = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated.');
  }

  const uid = context.auth.uid;
  const email = context.auth.token.email || 'user';
  const otpRef = db.collection('users').doc(uid).collection('security').doc('email_otp');
  const existingDoc = await otpRef.get();

  const nowMs = Date.now();
  if (existingDoc.exists) {
    const existingData = existingDoc.data();
    const createdMs = existingData?.createdMs || 0;
    if (nowMs - createdMs < 60000) { // 60s cooldown rate limit
      const remainingSec = Math.ceil((60000 - (nowMs - createdMs)) / 1000);
      throw new functions.https.HttpsError('resource-exhausted', `Please wait ${remainingSec}s before requesting a new code.`);
    }
  }

  // Generate 6-digit OTP code
  const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
  const expiresAtMs = nowMs + 10 * 60 * 1000; // 10 minutes valid

  await otpRef.set({
    code: otpCode,
    createdMs: nowMs,
    expiresAtMs: expiresAtMs,
    attemptsLeft: 5,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  functions.logger.info(`[Email OTP] Verification code for ${email} (${uid}): ${otpCode}`);

  return {
    status: 'SUCCESS',
    message: `Verification code generated: ${otpCode}`,
    otpCode: otpCode
  };
});

/**
 * 5. Verify Email OTP (Validate 6-digit Code & Mark Email Verified in Auth)
 */
export const verifyEmailOtp = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated.');
  }

  const uid = context.auth.uid;
  const rawCode = data?.otpCode;
  const otpCode = rawCode ? rawCode.toString().trim() : '';

  if (!otpCode || otpCode.length !== 6 || !/^\d{6}$/.test(otpCode)) {
    throw new functions.https.HttpsError('invalid-argument', 'Please enter a valid 6-digit verification code.');
  }

  const otpRef = db.collection('users').doc(uid).collection('security').doc('email_otp');
  const doc = await otpRef.get();

  if (!doc.exists) {
    throw new functions.https.HttpsError('not-found', 'No active verification code found. Please request a new code.');
  }

  const docData = doc.data()!;
  const nowMs = Date.now();

  if (nowMs > (docData.expiresAtMs || 0)) {
    await otpRef.delete();
    throw new functions.https.HttpsError('deadline-exceeded', 'Verification code has expired. Please request a new code.');
  }

  let attemptsLeft = docData.attemptsLeft ?? 5;
  if (attemptsLeft <= 0) {
    await otpRef.delete();
    throw new functions.https.HttpsError('resource-exhausted', 'Too many invalid attempts. Please request a new code.');
  }

  if (otpCode !== docData.code) {
    attemptsLeft -= 1;
    if (attemptsLeft <= 0) {
      await otpRef.delete();
      throw new functions.https.HttpsError('resource-exhausted', 'Too many invalid attempts. Please request a new code.');
    } else {
      await otpRef.update({ attemptsLeft: attemptsLeft });
      throw new functions.https.HttpsError('invalid-argument', `Incorrect code. You have ${attemptsLeft} attempt(s) left.`);
    }
  }

  // Code matches! Mark user's email as verified in Firebase Auth
  await admin.auth().updateUser(uid, { emailVerified: true });

  // Clean up used OTP doc
  await otpRef.delete();

  return {
    status: 'SUCCESS',
    message: 'Email verified successfully!'
  };
});
