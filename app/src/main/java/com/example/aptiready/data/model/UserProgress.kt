package com.example.aptiready.data.model

data class UserProgress(
    val isSampleData: Boolean,
    val accuracyPercentage: Int,
    val streakDays: Int,
    val topicsCompleted: Int,
    val totalQuestionsAnswered: Int,
    val quantMastery: Int,
    val logicalMastery: Int,
    val verbalMastery: Int
)