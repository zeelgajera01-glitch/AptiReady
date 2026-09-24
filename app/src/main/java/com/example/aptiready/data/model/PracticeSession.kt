package com.example.aptiready.data.model

data class PracticeSession(
    val id: String,
    val ownerId: String,
    val topicId: String,
    val topicTitle: String,
    val categoryName: String,
    val difficultyFilter: String,
    val requestedQuestionCount: Int,
    val status: String,
    val currentQuestionIndex: Int,
    val score: Int,
    val accuracyPercentage: Float,
    val createdAt: Long,
    val completedAt: Long?,
    val snapshots: List<SessionSnapshot> = emptyList()
) {
    fun totalQuestions(): Int = snapshots.size
    fun submittedCount(): Int = snapshots.count { it.isSubmitted && it.selectedOptionId != null }
    fun correctCount(): Int = snapshots.count { it.isCorrect() }
    fun incorrectCount(): Int = snapshots.count { it.isIncorrect() }
    fun unansweredCount(): Int = snapshots.count { !it.isSubmitted || it.selectedOptionId == null }
}