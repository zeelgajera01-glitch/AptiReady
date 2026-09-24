package com.example.aptiready.data.model

data class TestSummary(
    val id: String,
    val title: String,
    val categoryName: String,
    val description: String,
    val durationMinutes: Int,
    val questionCount: Int,
    val difficulty: Difficulty
)