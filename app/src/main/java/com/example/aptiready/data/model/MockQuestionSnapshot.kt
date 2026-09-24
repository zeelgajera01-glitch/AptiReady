package com.example.aptiready.data.model

data class MockQuestionSnapshot(
    val id: String,
    val attemptId: String,
    val questionId: String,
    val position: Int,
    val questionText: String,
    val options: List<QuestionOption>,
    val correctOptionId: String,
    val explanation: String,
    val hint: String,
    val selectedOptionId: String?,
    val isMarkedForReview: Boolean,
    val isVisited: Boolean,
    val isBookmarked: Boolean
) {
    fun isCorrect(): Boolean = selectedOptionId == correctOptionId
    fun isIncorrect(): Boolean = selectedOptionId != null && selectedOptionId != correctOptionId
    fun isUnanswered(): Boolean = selectedOptionId == null
}