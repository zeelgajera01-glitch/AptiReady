package com.aptirise.app.data.model

data class Topic(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val title: String,
    val description: String,
    val difficulty: Difficulty,
    val sampleQuestionCount: Int,
    val formulaPreview: String
)