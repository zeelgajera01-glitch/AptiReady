package com.example.aptiready.data.model

data class AttemptHistoryItem(
    val id: String,
    val ownerId: String,
    val title: String,
    val categoryOrSubtitle: String,
    val type: String, // "PRACTICE" vs "MOCK_TEST"
    val scorePercentage: Float,
    val accuracyPercentage: Float,
    val earnedSummary: String,
    val completedAt: Long,
    val finishReason: String?
)