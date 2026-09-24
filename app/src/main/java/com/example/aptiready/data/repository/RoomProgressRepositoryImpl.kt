package com.example.aptiready.data.repository

import com.example.aptiready.data.local.db.AppDatabase
import com.example.aptiready.data.local.db.SessionQuestionSnapshotEntity
import com.example.aptiready.data.model.Difficulty
import com.example.aptiready.data.model.Topic
import com.example.aptiready.data.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RoomProgressRepositoryImpl(
    private val database: AppDatabase,
    private val authRepository: AuthRepository
) : ProgressRepository {

    private val sessionDao = database.practiceSessionDao()
    private val mockTestDao = database.mockTestDao()
    private val contentDao = database.contentDao()

    override fun getUserProgress(
        includeSampleData: Boolean
    ): Flow<UserProgress> {
        val ownerId = authRepository.currentUserId ?: "guest"

        return combine(
            sessionDao.getCompletedSessionsForOwner(ownerId),
            sessionDao.observeCompletedSnapshotsForOwner(ownerId),
            mockTestDao.observeCompletedSnapshotsForOwner(ownerId)
        ) { sessions, practiceSnapshots, mockSnapshots ->

            fun hasSelectedAnswer(value: String?): Boolean {
                return !value.isNullOrBlank() &&
                    !value.equals("null", ignoreCase = true)
            }

            // Overall Stats
            val answeredPractice = practiceSnapshots.filter {
                it.isSubmitted && hasSelectedAnswer(it.selectedOptionId)
            }
            val answeredMocks = mockSnapshots.filter {
                hasSelectedAnswer(it.selectedOptionId)
            }

            val totalAnswered = answeredPractice.size + answeredMocks.size
            val totalCorrect = answeredPractice.count { it.selectedOptionId == it.correctOptionId } +
                    answeredMocks.count { it.selectedOptionId == it.correctOptionId }

            val accuracy = if (totalAnswered == 0) 0 else ((totalCorrect.toDouble() / totalAnswered.toDouble()) * 100.0).toInt().coerceIn(0, 100)

            val topicsPracticed = sessions.map { it.topicId }.filter { it.isNotBlank() }.distinct().size

            // Category Mastery Calculation
            val quantMastery = calculateCategoryMastery("quant", sessions, practiceSnapshots)
            val logicalMastery = calculateCategoryMastery("logical", sessions, practiceSnapshots)
            val verbalMastery = calculateCategoryMastery("verbal", sessions, practiceSnapshots)
            
            // Streak Calculation
            val allAttemptTimes = (sessions.mapNotNull { it.completedAt } + mockSnapshots.map { 0L }).filter { it > 0 } // Mock snapshots don't have time, need mock sessions
            val mockAttemptTimes = database.mockTestDao().getCompletedAttemptsListForOwner(ownerId).mapNotNull { it.completedAt }
            val streak = calculateStreak(allAttemptTimes + mockAttemptTimes)

            UserProgress(
                isSampleData = false,
                accuracyPercentage = accuracy,
                streakDays = streak,
                topicsCompleted = topicsPracticed,
                totalQuestionsAnswered = totalAnswered,
                quantMastery = quantMastery,
                logicalMastery = logicalMastery,
                verbalMastery = verbalMastery
            )
        }
    }

    private fun calculateStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        
        val uniqueDays = timestamps.map { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()
        
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val todayMs = today.timeInMillis
        
        var streak = 0
        var currentDayCheck = todayMs
        
        // If last active day is not today or yesterday, streak is broken
        if (uniqueDays.isEmpty() || (todayMs - uniqueDays.first()) > TimeUnit.DAYS.toMillis(1)) {
            return 0
        }
        
        for (day in uniqueDays) {
            if (day == currentDayCheck || day == currentDayCheck - TimeUnit.DAYS.toMillis(1)) {
                streak++
                currentDayCheck = day
            } else {
                break
            }
        }
        
        return streak
    }

    private fun calculateCategoryMastery(
        categoryId: String,
        sessions: List<com.example.aptiready.data.local.db.PracticeSessionEntity>,
        practiceSnaps: List<SessionQuestionSnapshotEntity>
    ): Int {
        val sessionIds = sessions.filter { 
            it.categoryName.contains(categoryId, ignoreCase = true) || 
            it.topicId.contains(categoryId, ignoreCase = true) 
        }.map { it.id }.toSet()
        
        val categoryPractice = practiceSnaps.filter { 
            it.sessionId in sessionIds && 
            it.isSubmitted && 
            !it.selectedOptionId.isNullOrBlank() &&
            it.selectedOptionId != "null"
        }
        
        val answered = categoryPractice.size
        if (answered == 0) return 0
        
        val correct = categoryPractice.count { it.selectedOptionId == it.correctOptionId }
        return ((correct.toFloat() / answered.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    override fun getRecentTopic(): Flow<Topic?> {
        return contentDao.getTopics().map { list ->
            list.firstOrNull()?.let { entity ->
                Topic(
                    id = entity.id,
                    categoryId = entity.categoryId,
                    categoryName = entity.categoryName,
                    title = entity.title,
                    description = entity.description,
                    difficulty = parseDifficulty(entity.difficulty),
                    sampleQuestionCount = 60,
                    formulaPreview = entity.formulaPreview
                )
            }
        }
    }

    private fun parseDifficulty(diff: String): Difficulty {
        return when (diff.lowercase()) {
            "easy" -> Difficulty.EASY
            "hard" -> Difficulty.HARD
            else -> Difficulty.MEDIUM
        }
    }
}