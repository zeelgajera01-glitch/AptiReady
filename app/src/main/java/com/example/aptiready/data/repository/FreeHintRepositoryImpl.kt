package com.example.aptiready.data.repository

import android.content.Context
import android.util.Log
import com.example.aptiready.data.model.FreeHintClaimResult
import com.example.aptiready.data.model.FreeHintStatus
import com.example.aptiready.data.remote.FirebaseConfigManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FreeHintRepositoryImpl(
    private val context: Context,
    private val authRepository: AuthRepository
) : FreeHintRepository {

    private val firestore: FirebaseFirestore? by lazy {
        if (FirebaseConfigManager.isConfigured(context)) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    override suspend fun getFreeHintStatus(
        ownerId: String,
        currentSessionId: String,
        currentQuestionId: String
    ): FreeHintStatus = withContext(Dispatchers.IO) {
        if (!authRepository.isFirebaseConfigured || firestore == null) {
            return@withContext FreeHintStatus.Unverified("Firebase is not configured.")
        }

        if (ownerId.isBlank() || ownerId == "guest" ||
            currentSessionId.isBlank() || currentSessionId.length > 100 ||
            currentQuestionId.isBlank() || currentQuestionId.length > 100
        ) {
            return@withContext FreeHintStatus.Error("Invalid session or question identifier.")
        }

        val user = try { FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
        if (user == null || user.uid != ownerId) {
            return@withContext FreeHintStatus.Unverified("Verified account required to use free hint.")
        }

        if (!user.isEmailVerified) {
            return@withContext FreeHintStatus.Unverified("Email verification required to use free hint.")
        }

        val docRef = firestore!!.collection("users")
            .document(ownerId)
            .collection("hintEntitlements")
            .document("lifetimeFreeHint")

        try {
            val snapshot = docRef.get(Source.SERVER).await()
            if (!snapshot.exists()) {
                FreeHintStatus.Available
            } else {
                val existingSessionId = snapshot.getString("sessionId") ?: ""
                val existingQuestionId = snapshot.getString("questionId") ?: ""
                if (existingSessionId.isBlank() || existingQuestionId.isBlank() ||
                    existingSessionId.length > 100 || existingQuestionId.length > 100
                ) {
                    FreeHintStatus.Error("Malformed existing hint claim detected.")
                } else if (existingSessionId == currentSessionId && existingQuestionId == currentQuestionId) {
                    FreeHintStatus.UnlockedForCurrent(ownerId, currentSessionId, currentQuestionId, isVisible = false)
                } else {
                    FreeHintStatus.Exhausted(existingSessionId, existingQuestionId)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            Log.w("FreeHintRepo", "Firestore error: ${e.code}", e)
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    FreeHintStatus.Unverified("Authentication or permission validation failed.")
                else ->
                    FreeHintStatus.Error("Network error checking free hint status. Please check your connection.")
            }
        } catch (e: Exception) {
            Log.w("FreeHintRepo", "Fetch status failed: ${e.message}", e)
            FreeHintStatus.Error("Failed to check free hint allowance. Please check your network connection.")
        }
    }

    override suspend fun claimFreeHint(
        ownerId: String,
        sessionId: String,
        questionId: String
    ): FreeHintClaimResult = withContext(Dispatchers.IO) {
        if (!authRepository.isFirebaseConfigured || firestore == null) {
            return@withContext FreeHintClaimResult.Unverified("Firebase is not configured.")
        }

        if (ownerId.isBlank() || ownerId == "guest" ||
            sessionId.isBlank() || sessionId.length > 100 ||
            questionId.isBlank() || questionId.length > 100
        ) {
            return@withContext FreeHintClaimResult.NetworkError("Invalid session or question identifier.")
        }

        val user = try { FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
        if (user == null || user.uid != ownerId) {
            return@withContext FreeHintClaimResult.Unverified("Verified account required.")
        }

        if (!user.isEmailVerified) {
            return@withContext FreeHintClaimResult.Unverified("Email verification required.")
        }

        val docRef = firestore!!.collection("users")
            .document(ownerId)
            .collection("hintEntitlements")
            .document("lifetimeFreeHint")

        try {
            val result = firestore!!.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    val data = hashMapOf(
                        "sessionId" to sessionId,
                        "questionId" to questionId,
                        "claimedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(docRef, data)
                    FreeHintClaimResult.Granted(ownerId, sessionId, questionId)
                } else {
                    val existingSessionId = snapshot.getString("sessionId") ?: ""
                    val existingQuestionId = snapshot.getString("questionId") ?: ""
                    if (existingSessionId.isBlank() || existingQuestionId.isBlank() ||
                        existingSessionId.length > 100 || existingQuestionId.length > 100
                    ) {
                        FreeHintClaimResult.NetworkError("Malformed existing hint claim detected.")
                    } else if (existingSessionId == sessionId && existingQuestionId == questionId) {
                        FreeHintClaimResult.AllowedReopen(ownerId, sessionId, questionId)
                    } else {
                        FreeHintClaimResult.Exhausted(existingSessionId, existingQuestionId)
                    }
                }
            }.await()
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            Log.e("FreeHintRepo", "Transaction Firestore error: ${e.code}", e)
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    FreeHintClaimResult.Unverified("Authentication or permission validation failed.")
                else ->
                    FreeHintClaimResult.NetworkError("Failed to claim free hint. Please check your network connection.")
            }
        } catch (e: Exception) {
            Log.e("FreeHintRepo", "Claim transaction failed: ${e.message}", e)
            FreeHintClaimResult.NetworkError("Failed to claim free hint. Please check your network connection.")
        }
    }
}
