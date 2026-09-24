package com.example.aptiready.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("SELECT * FROM categories WHERE published = 1 ORDER BY sortOrder ASC")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM topics WHERE published = 1 ORDER BY sortOrder ASC")
    fun getTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
    suspend fun getTopicById(topicId: String): TopicEntity?

    @Query("SELECT * FROM questions WHERE topicId = :topicId AND published = 1")
    suspend fun getQuestionsForTopic(topicId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE topicId = :topicId AND difficulty = :difficulty AND published = 1")
    suspend fun getQuestionsForTopicAndDifficulty(topicId: String, difficulty: String): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions WHERE published = 1")
    suspend fun getTotalQuestionCount(): Int
}