package com.example.aptiready.data.model

data class PracticeQuestion(
    val id: String,
    val topicId: String,
    val categoryId: String,
    val difficulty: Difficulty,
    val languageCode: String,
    val questionText: String,
    val options: List<QuestionOption>,
    val correctOptionId: String,
    val explanation: String,
    val hint: String,
    val extraHint: String = ""
)
