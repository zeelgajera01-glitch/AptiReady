package com.aptirise.app.data.repository

import com.aptirise.app.data.model.FreeHintClaimResult
import com.aptirise.app.data.model.FreeHintStatus

interface FreeHintRepository {
    suspend fun getFreeHintStatus(ownerId: String, currentSessionId: String, currentQuestionId: String): FreeHintStatus
    suspend fun claimFreeHint(ownerId: String, sessionId: String, questionId: String): FreeHintClaimResult
}
