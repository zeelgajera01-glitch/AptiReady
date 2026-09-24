package com.example.aptiready.data.model

data class MockTestAttempt(
    val id: String,
    val ownerId: String,
    val testId: String,
    val testTitle: String,
    val testDescription: String,
    val testVersion: Int,
    val durationSeconds: Int,
    val marksPerCorrect: Int,
    val status: String, // "IN_PROGRESS", "COMPLETED", "DISCARDED"
    val currentQuestionIndex: Int,
    val elapsedSeconds: Int,
    val startTimeMillis: Long,
    val bootTimeMarker: Long,
    val lastSavedTimeMillis: Long,
    val remainingSeconds: Int,
    val earnedMarks: Int,
    val maxMarks: Int,
    val scorePercentage: Float,
    val accuracyPercentage: Float,
    val finishReason: String?, // "MANUAL_SUBMISSION", "TIME_EXPIRED", "SYSTEM"
    val createdAt: Long,
    val completedAt: Long?,
    val snapshots: List<MockQuestionSnapshot> = emptyList()
) {
    fun totalQuestions(): Int = snapshots.size
    fun answeredCount(): Int = snapshots.count { it.selectedOptionId != null }
    fun unansweredCount(): Int = snapshots.count { it.selectedOptionId == null }
    fun markedForReviewCount(): Int = snapshots.count { it.isMarkedForReview }
    fun correctCount(): Int = snapshots.count { it.isCorrect() }
    fun incorrectCount(): Int = snapshots.count { it.isIncorrect() }
}