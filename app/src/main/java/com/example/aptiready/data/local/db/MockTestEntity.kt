package com.example.aptiready.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mock_tests")
data class MockTestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val questionIdsJson: String,
    val durationSeconds: Int,
    val marksPerCorrect: Int,
    val published: Boolean,
    val version: Int
)