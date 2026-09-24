package com.example.aptiready.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String, // "${ownerId}_${questionId}"
    val ownerId: String,
    val questionId: String,
    val topicId: String,
    val topicTitle: String,
    val questionText: String,
    val optionsJson: String,
    val correctOptionId: String,
    val explanation: String,
    val hint: String,
    @ColumnInfo(name = "extraHint", defaultValue = "") val extraHint: String = "",
    val createdAt: Long
)
