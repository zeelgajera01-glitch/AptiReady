package com.aptirise.app.data.repository

import android.content.Context
import com.aptirise.app.data.model.FreeHintClaimResult
import com.aptirise.app.data.model.FreeHintStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FreeHintRepositoryImpl(context: Context) : FreeHintRepository {
    private val prefs = context.getSharedPreferences("local_hint_allowance", Context.MODE_PRIVATE)
    override suspend fun getFreeHintStatus(ownerId: String, currentSessionId: String, currentQuestionId: String): FreeHintStatus = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val session = prefs.getString("session", null)
            val question = prefs.getString("question", null)
            when {
                session == null -> FreeHintStatus.Available
                session == currentSessionId && question == currentQuestionId -> FreeHintStatus.UnlockedForCurrent(ownerId, currentSessionId, currentQuestionId)
                else -> FreeHintStatus.Exhausted(session, question.orEmpty())
            }
        }
    }
    override suspend fun claimFreeHint(ownerId: String, sessionId: String, questionId: String): FreeHintClaimResult = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val session = prefs.getString("session", null)
            val question = prefs.getString("question", null)
            when {
                sessionId.isBlank() || questionId.isBlank() -> FreeHintClaimResult.NetworkError("Could not identify this question.")
                session == sessionId && question == questionId -> FreeHintClaimResult.AllowedReopen(ownerId, sessionId, questionId)
                session != null -> FreeHintClaimResult.Exhausted(session, question.orEmpty())
                prefs.edit().putString("session", sessionId).putString("question", questionId).commit() -> FreeHintClaimResult.Granted(ownerId, sessionId, questionId)
                else -> FreeHintClaimResult.NetworkError("Could not save the hint. Please try again.")
            }
        }
    }
    private companion object { val lock = Any() }
}
