package com.example.aptiready.data.repository

import com.example.aptiready.data.model.Topic
import com.example.aptiready.data.model.UserProgress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun getUserProgress(includeSampleData: Boolean): Flow<UserProgress>
    fun getRecentTopic(): Flow<Topic?>
}