package com.example.aptiready.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mock_question_snapshots")
data class MockQuestionSnapshotEntity(
    @PrimaryKey val id: String, // "${attemptId}_${position}"
    val attemptId: String,
    val questionId: String,
    val position: Int,
    val questionText: String,
    val optionsJson: String,
    val correctOptionId: String,
    val explanation: String,
    val hint: String,
    val selectedOptionId: String?,
    val isMarkedForReview: Boolean,
    val isVisited: Boolean,
    val isBookmarked: Boolean
)