package com.example.aptiready.data.repository

import com.example.aptiready.data.model.FreeHintClaimResult
import com.example.aptiready.data.model.FreeHintStatus

interface FreeHintRepository {
    suspend fun getFreeHintStatus(ownerId: String, currentSessionId: String, currentQuestionId: String): FreeHintStatus
    suspend fun claimFreeHint(ownerId: String, sessionId: String, questionId: String): FreeHintClaimResult
}
