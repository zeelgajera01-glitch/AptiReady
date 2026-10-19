package com.aptirise.app.data.repository

import com.aptirise.app.data.model.Category
import com.aptirise.app.data.model.TestSummary
import com.aptirise.app.data.model.Topic
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    fun getCategories(): Flow<List<Category>>
    fun getTopics(categoryId: String? = null, searchQuery: String? = null): Flow<List<Topic>>
    fun getTopicById(topicId: String): Flow<Topic?>
    fun getTestCatalog(): Flow<List<TestSummary>>
    fun getTestById(testId: String): Flow<TestSummary?>
}