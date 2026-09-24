package com.example.aptiready.data.model

data class SessionSnapshot(
    val id: String,
    val sessionId: String,
    val questionId: String,
    val position: Int,
    val questionText: String,
    val options: List<QuestionOption>,
    val correctOptionId: String,
    val explanation: String,
    val hint: String,
    val extraHint: String = "",
    val isExtraHintUnlocked: Boolean = false,
    val selectedOptionId: String?,
    val isSubmitted: Boolean,
    val isBookmarked: Boolean
) {
    fun isCorrect(): Boolean = isSubmitted && selectedOptionId == correctOptionId
    fun isIncorrect(): Boolean = isSubmitted && selectedOptionId != null && selectedOptionId != correctOptionId
    fun isSkipped(): Boolean = isSubmitted && selectedOptionId == null
}
