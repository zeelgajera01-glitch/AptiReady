package com.example.aptiready.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mock_test_attempts")
data class MockTestAttemptEntity(
    @PrimaryKey val id: String,
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
    val finishReason: String?,
    val createdAt: Long,
    val completedAt: Long?
)