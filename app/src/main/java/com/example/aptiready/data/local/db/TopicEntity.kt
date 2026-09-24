package com.example.aptiready.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val categoryName: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val sampleQuestionCount: Int,
    val formulaPreview: String,
    val sortOrder: Int,
    val published: Boolean
)