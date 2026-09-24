package com.example.aptiready.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_sessions")
data class PracticeSessionEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val topicId: String,
    val topicTitle: String,
    val categoryName: String,
    val difficultyFilter: String,
    val requestedQuestionCount: Int,
    val status: String, // "IN_PROGRESS", "COMPLETED", "DISCARDED"
    val currentQuestionIndex: Int,
    val score: Int,
    val accuracyPercentage: Float,
    val createdAt: Long,
    val completedAt: Long?
)