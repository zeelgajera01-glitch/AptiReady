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

    @Query("SELECT id, questionText FROM questions WHERE topicId = :topicId AND published = 1 AND (:difficulty IS NULL OR difficulty = :difficulty)")
    suspend fun getPracticeCandidates(topicId: String, difficulty: String?): List<PracticeCandidate>

    @Query("SELECT COUNT(*) FROM questions WHERE published = 1")
    suspend fun getTotalQuestionCount(): Int
    @Query("SELECT COUNT(*) FROM questions WHERE version >= :minimumVersion")
    suspend fun getQuestionCountAtLeastVersion(minimumVersion: Int): Int

    @Query("SELECT * FROM questions WHERE id = :questionId AND published = 1 LIMIT 1")
    suspend fun getQuestionById(questionId: String): QuestionEntity?

    @Query("SELECT * FROM questions WHERE id IN (:questionIds) AND published = 1")
    suspend fun getQuestionsByIds(questionIds: List<String>): List<QuestionEntity>

    @Query("SELECT topicId, COUNT(DISTINCT LOWER(TRIM(questionText))) AS questionCount FROM questions WHERE published = 1 GROUP BY topicId")
    fun observeQuestionCounts(): Flow<List<TopicQuestionCount>>
}

data class TopicQuestionCount(val topicId: String, val questionCount: Int)

data class PracticeCandidate(val id: String, val questionText: String)
