package com.example.aptiready.data.model

sealed class FreeHintStatus {
    object Loading : FreeHintStatus()
    object Available : FreeHintStatus()
    data class UnlockedForCurrent(
        val ownerId: String,
        val sessionId: String,
        val questionId: String,
        val isVisible: Boolean = false
    ) : FreeHintStatus()
    data class Exhausted(
        val claimedSessionId: String,
        val claimedQuestionId: String
    ) : FreeHintStatus()
    data class Unverified(val reason: String) : FreeHintStatus()
    data class Error(val message: String) : FreeHintStatus()
}

sealed class FreeHintClaimResult {
    data class Granted(
        val ownerId: String,
        val sessionId: String,
        val questionId: String
    ) : FreeHintClaimResult()
    data class AllowedReopen(
        val ownerId: String,
        val sessionId: String,
        val questionId: String
    ) : FreeHintClaimResult()
    data class Exhausted(
        val claimedSessionId: String,
        val claimedQuestionId: String
    ) : FreeHintClaimResult()
    data class Unverified(val reason: String) : FreeHintClaimResult()
    data class NetworkError(val message: String) : FreeHintClaimResult()
}
