package com.example.aptiready.data.model

data class BookmarkItem(
    val id: String,
    val ownerId: String,
    val questionId: String,
    val topicId: String,
    val topicTitle: String,
    val questionText: String,
    val options: List<QuestionOption>,
    val correctOptionId: String,
    val explanation: String,
    val hint: String,
    val extraHint: String = "",
    val createdAt: Long,
    var isRevealed: Boolean = false
)
