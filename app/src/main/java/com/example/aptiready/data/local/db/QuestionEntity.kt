package com.example.aptiready.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val categoryId: String,
    val difficulty: String,
    val languageCode: String,
    val questionText: String,
    val optionsJson: String,
    val correctOptionId: String,
    val explanation: String,
    val hint: String,
    @ColumnInfo(name = "extraHint", defaultValue = "") val extraHint: String = "",
    val published: Boolean,
    val version: Int,
    val updatedAt: Long
)
