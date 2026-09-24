package com.example.aptiready.data.model

data class MockTest(
    val id: String,
    val title: String,
    val description: String,
    val questionIds: List<String>,
    val durationSeconds: Int,
    val marksPerCorrect: Int,
    val published: Boolean,
    val version: Int
)