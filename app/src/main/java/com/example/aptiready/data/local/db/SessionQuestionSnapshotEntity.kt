package com.example.aptiready.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_question_snapshots")
data class SessionQuestionSnapshotEntity(
    @PrimaryKey val id: String, // "${sessionId}_${position}"
    val sessionId: String,
    val questionId: String,
    val position: Int,
    val questionText: String,
    val optionsJson: String,
    val correctOptionId: String,
    val explanation: String,
    val hint: String,
    @ColumnInfo(name = "extraHint", defaultValue = "") val extraHint: String = "",
    @ColumnInfo(name = "isExtraHintUnlocked", defaultValue = "0") val isExtraHintUnlocked: Boolean = false,
    val selectedOptionId: String?,
    val isSubmitted: Boolean,
    val isBookmarked: Boolean
)
