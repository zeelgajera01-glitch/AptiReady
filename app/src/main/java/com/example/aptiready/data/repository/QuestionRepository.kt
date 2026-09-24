package com.example.aptiready.data.repository

import com.example.aptiready.data.model.Category
import com.example.aptiready.data.model.TestSummary
import com.example.aptiready.data.model.Topic
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    fun getCategories(): Flow<List<Category>>
    fun getTopics(categoryId: String? = null, searchQuery: String? = null): Flow<List<Topic>>
    fun getTopicById(topicId: String): Flow<Topic?>
    fun getTestCatalog(): Flow<List<TestSummary>>
    fun getTestById(testId: String): Flow<TestSummary?>
}