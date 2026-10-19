package com.aptirise.app.data.repository

import com.aptirise.app.data.model.Topic
import com.aptirise.app.data.model.UserProgress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun getUserProgress(includeSampleData: Boolean): Flow<UserProgress>
    fun getRecentTopic(): Flow<Topic?>
}