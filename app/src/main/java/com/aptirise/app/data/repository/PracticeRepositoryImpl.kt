package com.aptirise.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.aptirise.app.data.local.db.AppDatabase
import com.aptirise.app.data.local.db.BookmarkEntity
import com.aptirise.app.data.local.db.PracticeSessionEntity
import com.aptirise.app.data.local.db.QuestionEntity
import com.aptirise.app.data.local.db.SessionQuestionSnapshotEntity
import com.aptirise.app.data.model.BookmarkItem
import com.aptirise.app.data.model.Difficulty
import com.aptirise.app.data.model.PracticeSession
import com.aptirise.app.data.model.QuestionOption
import com.aptirise.app.data.model.SessionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PracticeRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase
) : PracticeRepository {

    private val contentDao = database.contentDao()
    private val sessionDao = database.practiceSessionDao()
    private val bookmarkDao = database.bookmarkDao()

    override suspend fun getAvailableQuestionCount(topicId: String, difficulty: Difficulty?): Int = withContext(Dispatchers.IO) {
        val questions = contentDao.getPracticeCandidates(topicId, difficulty?.name?.lowercase())
        PracticeQuestionSelector.uniqueQuestions(questions) { it.questionText }.size
    }

    override suspend fun getUnseenQuestionCount(ownerId: String, topicId: String, difficulty: Difficulty?): Int = withContext(Dispatchers.IO) {
        val availableQuestions = contentDao.getPracticeCandidates(topicId, difficulty?.name?.lowercase())

        val seenIds = sessionDao.getSeenQuestionIdsForTopic(ownerId, topicId).toSet()
        val reservedIds = sessionDao.getActiveReservedQuestionIdsForTopic(ownerId, topicId).toSet()

        PracticeQuestionSelector.unseenCount(
            availableQuestions, seenIds, reservedIds,
            idOf = { it.id }, textOf = { it.questionText }
        )
    }

    override suspend fun getTopicQuestionCounts(
        ownerId: String,
        topicId: String,
        difficulty: Difficulty?
    ): TopicQuestionCounts = withContext(Dispatchers.IO) {
        val availableQuestions = contentDao.getPracticeCandidates(topicId, difficulty?.name?.lowercase())
        val availableCount = PracticeQuestionSelector.uniqueQuestions(availableQuestions) { it.questionText }.size

        val seenIds = sessionDao.getSeenQuestionIdsForTopic(ownerId, topicId).toSet()
        val reservedIds = sessionDao.getActiveReservedQuestionIdsForTopic(ownerId, topicId).toSet()

        val unseen = PracticeQuestionSelector.unseenCount(
            availableQuestions, seenIds, reservedIds,
            idOf = { it.id }, textOf = { it.questionText }
        )

        TopicQuestionCounts(available = availableCount, unseen = unseen)
    }

    override suspend fun createPracticeSession(
        ownerId: String,
        topicId: String,
        difficulty: Difficulty?,
        requestedCount: Int
    ): PracticeSession = withContext(Dispatchers.IO) {
        database.withTransaction {
            val topic = contentDao.getTopicById(topicId)
            val topicTitle = topic?.title ?: "Practice Topic"
            val categoryName = topic?.categoryName ?: "General Aptitude"
    
            val availableQuestions = contentDao.getPracticeCandidates(topicId, difficulty?.name?.lowercase())

            val seenIds = sessionDao.getSeenQuestionIdsForTopic(ownerId, topicId).toSet()
            val reservedIds = sessionDao.getActiveReservedQuestionIdsForTopic(ownerId, topicId).toSet()
    
            val selectedCandidates = PracticeQuestionSelector.select(
                questions = availableQuestions,
                seenIds = seenIds,
                reservedIds = reservedIds,
                requestedCount = requestedCount,
                idOf = { it.id },
                textOf = { it.questionText }
            )

            val selectedIds = selectedCandidates.map { it.id }
            val questionsMap = contentDao.getQuestionsByIds(selectedIds).associateBy { it.id }
            val selectedQuestions = selectedIds.mapNotNull { questionsMap[it] }
    
            require(selectedQuestions.isNotEmpty()) { "No questions available. Please try again after loading finishes." }

            val sessionId = "session_${UUID.randomUUID()}"
            val now = System.currentTimeMillis()
    
            val sessionEntity = PracticeSessionEntity(
                id = sessionId,
                ownerId = ownerId,
                topicId = topicId,
                topicTitle = topicTitle,
                categoryName = categoryName,
                difficultyFilter = difficulty?.name?.lowercase() ?: "all",
                requestedQuestionCount = selectedQuestions.size,
                status = "IN_PROGRESS",
                currentQuestionIndex = 0,
                score = 0,
                accuracyPercentage = 0f,
                createdAt = now,
                completedAt = null
            )
    
            val questionIds = selectedQuestions.map { it.id }
            val bookmarkedQuestionIds = bookmarkDao.getBookmarkedQuestionIds(ownerId, questionIds).toSet()

            val snapshotEntities = selectedQuestions.mapIndexed { index, q ->
                val isBookmarked = bookmarkedQuestionIds.contains(q.id)
                SessionQuestionSnapshotEntity(
                    id = "${sessionId}_$index",
                    sessionId = sessionId,
                    questionId = q.id,
                    position = index,
                    questionText = q.questionText,
                    optionsJson = q.optionsJson,
                    correctOptionId = q.correctOptionId,
                    explanation = q.explanation,
                    hint = q.hint,
                    extraHint = q.extraHint,
                    isExtraHintUnlocked = false,
                    selectedOptionId = null,
                    isSubmitted = false,
                    isBookmarked = isBookmarked
                )
            }
    
            sessionDao.insertSession(sessionEntity)
            sessionDao.insertSnapshots(snapshotEntities)
    
            mapSessionToDomain(sessionEntity, snapshotEntities)
        }
    }

    override suspend fun createRetrySession(ownerId: String, originalSessionId: String): PracticeSession? = withContext(Dispatchers.IO) {
        val originalSession = sessionDao.getSessionById(originalSessionId) ?: return@withContext null
        val originalSnapshots = sessionDao.getSnapshotsForSession(originalSessionId)
        val incorrectSnapshots = PracticeQuestionSelector.uniqueQuestions(
            originalSnapshots.filter { it.isSubmitted && it.selectedOptionId != null && it.selectedOptionId != it.correctOptionId }
        ) { it.questionText }

        if (incorrectSnapshots.isEmpty()) return@withContext null

        val newSessionId = "session_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        val newSessionEntity = PracticeSessionEntity(
            id = newSessionId,
            ownerId = ownerId,
            topicId = originalSession.topicId,
            topicTitle = "${originalSession.topicTitle} (Retry)",
            categoryName = originalSession.categoryName,
            difficultyFilter = originalSession.difficultyFilter,
            requestedQuestionCount = incorrectSnapshots.size,
            status = "IN_PROGRESS",
            currentQuestionIndex = 0,
            score = 0,
            accuracyPercentage = 0f,
            createdAt = now,
            completedAt = null
        )

        val newSnapshots = incorrectSnapshots.mapIndexed { index, snap ->
            val isBookmarked = bookmarkDao.isBookmarked(ownerId, snap.questionId)
            SessionQuestionSnapshotEntity(
                id = "${newSessionId}_$index",
                sessionId = newSessionId,
                questionId = snap.questionId,
                position = index,
                questionText = snap.questionText,
                optionsJson = snap.optionsJson,
                correctOptionId = snap.correctOptionId,
                explanation = snap.explanation,
                hint = snap.hint,
                extraHint = snap.extraHint,
                isExtraHintUnlocked = false,
                selectedOptionId = null,
                isSubmitted = false,
                isBookmarked = isBookmarked
            )
        }

        database.withTransaction {
            sessionDao.insertSession(newSessionEntity)
            sessionDao.insertSnapshots(newSnapshots)
        }

        return@withContext mapSessionToDomain(newSessionEntity, newSnapshots)
    }

    override suspend fun getInProgressSession(ownerId: String): PracticeSession? = withContext(Dispatchers.IO) {
        val sessionEntity = sessionDao.getInProgressSession(ownerId) ?: return@withContext null
        val snapshots = sessionDao.getSnapshotsForSession(sessionEntity.id)
        return@withContext mapSessionToDomain(sessionEntity, snapshots)
    }

    override fun getInProgressSessionFlow(ownerId: String): Flow<PracticeSession?> {
        return sessionDao.getInProgressSessionFlow(ownerId).map { sessionEntity ->
            if (sessionEntity == null) null
            else {
                val snapshots = sessionDao.getSnapshotsForSession(sessionEntity.id)
                mapSessionToDomain(sessionEntity, snapshots)
            }
        }.distinctUntilChanged().flowOn(Dispatchers.IO)
    }

    override suspend fun getSessionById(sessionId: String): PracticeSession? = withContext(Dispatchers.IO) {
        val sessionEntity = sessionDao.getSessionById(sessionId) ?: return@withContext null
        val snapshots = sessionDao.getSnapshotsForSession(sessionId)
        return@withContext mapSessionToDomain(sessionEntity, snapshots)
    }

    override fun getSessionSnapshotsFlow(sessionId: String): Flow<List<SessionSnapshot>> {
        return sessionDao.getSnapshotsForSessionFlow(sessionId).map { list ->
            list.map { mapSnapshotToDomain(it) }
        }.distinctUntilChanged().flowOn(Dispatchers.IO)
    }

    override suspend fun saveAnswerSelection(sessionId: String, position: Int, selectedOptionId: String?) = withContext(Dispatchers.IO) {
        sessionDao.saveSelection(sessionId, position, selectedOptionId)
        Unit
    }

    override suspend fun submitQuestionAnswer(sessionId: String, position: Int) = withContext(Dispatchers.IO) {
        sessionDao.submitAnswer(sessionId, position)
        Unit
    }

    override suspend fun updateCurrentPosition(sessionId: String, position: Int) = withContext(Dispatchers.IO) {
        sessionDao.savePosition(sessionId, position)
        Unit
    }

    override suspend fun finalizeSession(sessionId: String, ownerId: String): PracticeSession? = withContext(Dispatchers.IO) {
        database.withTransaction completion@ {
        val session = sessionDao.getSessionById(sessionId) ?: return@completion null
        if (session.status == "COMPLETED") {
            return@completion getSessionById(sessionId)
        }

        val snapshots = sessionDao.getSnapshotsForSession(sessionId)
        
        val finalizedSnapshots = snapshots.map { snap ->
            if (!snap.isSubmitted) snap.copy(isSubmitted = true) else snap
        }
        sessionDao.insertSnapshots(finalizedSnapshots)

        val total = finalizedSnapshots.size
        val correct = finalizedSnapshots.count { it.isSubmitted && it.selectedOptionId == it.correctOptionId }
        val answered = finalizedSnapshots.count { it.isSubmitted && it.selectedOptionId != null }
        
        val score = correct
        val accuracy = if (answered > 0) (correct.toFloat() / answered.toFloat()) * 100f else 0f
        val now = System.currentTimeMillis()

        database.withTransaction {
            val s = sessionDao.getSessionById(sessionId) ?: return@withTransaction
            if (s.status == "COMPLETED") return@withTransaction
            val updated = s.copy(
                status = "COMPLETED",
                score = score,
                accuracyPercentage = accuracy,
                completedAt = now
            )
            sessionDao.updateSession(updated)

        }

        return@completion getSessionById(sessionId)
        }
    }

    override suspend fun discardSession(sessionId: String) = withContext(Dispatchers.IO) {
        sessionDao.discardActiveSession(sessionId)
        Unit
    }

    override fun getCompletedSessionsForOwner(ownerId: String): Flow<List<PracticeSession>> {
        return sessionDao.getCompletedSessionsForOwner(ownerId).map { list ->
            list.map { sessionEntity ->
                val snapshots = sessionDao.getSnapshotsForSession(sessionEntity.id)
                mapSessionToDomain(sessionEntity, snapshots)
            }
        }.distinctUntilChanged().flowOn(Dispatchers.IO)
    }

    override suspend fun toggleBookmark(ownerId: String, snapshot: SessionSnapshot, topicTitle: String): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
        val isCurrentlyBookmarked = bookmarkDao.isBookmarked(ownerId, snapshot.questionId)
        val newBookmarkState = !isCurrentlyBookmarked

        if (newBookmarkState) {
            val optionsJson = JSONArray().apply {
                snapshot.options.forEach { opt ->
                    put(JSONObject().apply {
                        put("id", opt.id)
                        put("text", opt.text)
                    })
                }
            }.toString()

            val bookmarkEntity = BookmarkEntity(
                id = "${ownerId}_${snapshot.questionId}",
                ownerId = ownerId,
                questionId = snapshot.questionId,
                topicId = snapshot.id,
                topicTitle = topicTitle,
                questionText = snapshot.questionText,
                optionsJson = optionsJson,
                correctOptionId = snapshot.correctOptionId,
                explanation = snapshot.explanation,
                hint = snapshot.hint,
                extraHint = snapshot.extraHint,
                createdAt = System.currentTimeMillis()
            )
            bookmarkDao.insertBookmark(bookmarkEntity)
        } else {
            bookmarkDao.deleteBookmark(ownerId, snapshot.questionId)
        }

        val snapshots = sessionDao.getSnapshotsForSession(snapshot.sessionId)
        val match = snapshots.find { it.position == snapshot.position }
        if (match != null) {
            sessionDao.saveBookmarkState(snapshot.sessionId, snapshot.position, newBookmarkState)
        }

        newBookmarkState
        }
    }

    override fun getBookmarksForOwner(ownerId: String): Flow<List<BookmarkItem>> {
        return bookmarkDao.getBookmarksForOwner(ownerId).map { list ->
            list.map { entity ->
                val options = parseOptionsJson(entity.optionsJson)
                BookmarkItem(
                    id = entity.id,
                    ownerId = entity.ownerId,
                    questionId = entity.questionId,
                    topicId = entity.topicId,
                    topicTitle = entity.topicTitle,
                    questionText = entity.questionText,
                    options = options,
                    correctOptionId = entity.correctOptionId,
                    explanation = entity.explanation,
                    hint = entity.hint,
                    extraHint = entity.extraHint,
                    createdAt = entity.createdAt
                )
            }
        }.distinctUntilChanged().flowOn(Dispatchers.IO)
    }

    override suspend fun removeBookmark(ownerId: String, questionId: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(ownerId, questionId)
        Unit
    }

    private fun mapSessionToDomain(
        entity: PracticeSessionEntity,
        snapshots: List<SessionQuestionSnapshotEntity>
    ): PracticeSession {
        val domainSnapshots = snapshots.map { mapSnapshotToDomain(it) }
        return PracticeSession(
            id = entity.id,
            ownerId = entity.ownerId,
            topicId = entity.topicId,
            topicTitle = entity.topicTitle,
            categoryName = entity.categoryName,
            difficultyFilter = entity.difficultyFilter,
            requestedQuestionCount = entity.requestedQuestionCount,
            status = entity.status,
            currentQuestionIndex = entity.currentQuestionIndex,
            score = entity.score,
            accuracyPercentage = entity.accuracyPercentage,
            createdAt = entity.createdAt,
            completedAt = entity.completedAt,
            snapshots = domainSnapshots
        )
    }

    private fun mapSnapshotToDomain(entity: SessionQuestionSnapshotEntity): SessionSnapshot {
        val options = parseOptionsJson(entity.optionsJson)
        return SessionSnapshot(
            id = entity.id,
            sessionId = entity.sessionId,
            questionId = entity.questionId,
            position = entity.position,
            questionText = entity.questionText,
            options = options,
            correctOptionId = entity.correctOptionId,
            explanation = entity.explanation,
            hint = entity.hint,
            extraHint = entity.extraHint,
            isExtraHintUnlocked = entity.isExtraHintUnlocked,
            selectedOptionId = entity.selectedOptionId,
            isSubmitted = entity.isSubmitted,
            isBookmarked = entity.isBookmarked
        )
    }

    private fun parseOptionsJson(optionsJson: String): List<QuestionOption> {
        val list = mutableListOf<QuestionOption>()
        try {
            val jsonArray = JSONArray(optionsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(QuestionOption(obj.getString("id"), obj.getString("text")))
            }
        } catch (e: Exception) {
            // Fallback
        }
        return list
    }
}
