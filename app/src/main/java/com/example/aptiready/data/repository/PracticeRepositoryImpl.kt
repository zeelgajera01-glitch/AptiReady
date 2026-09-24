package com.example.aptiready.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.aptiready.data.local.db.AppDatabase
import com.example.aptiready.data.local.db.BookmarkEntity
import com.example.aptiready.data.local.db.PracticeSessionEntity
import com.example.aptiready.data.local.db.QuestionEntity
import com.example.aptiready.data.local.db.SessionQuestionSnapshotEntity
import com.example.aptiready.data.local.db.SyncOutboxEntity
import com.example.aptiready.data.model.BookmarkItem
import com.example.aptiready.data.model.Difficulty
import com.example.aptiready.data.model.PracticeSession
import com.example.aptiready.data.model.QuestionOption
import com.example.aptiready.data.model.SessionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PracticeRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase,
    private val cloudSyncRepositoryProvider: (() -> CloudSyncRepository)? = null
) : PracticeRepository {

    private val contentDao = database.contentDao()
    private val sessionDao = database.practiceSessionDao()
    private val bookmarkDao = database.bookmarkDao()

    override suspend fun getAvailableQuestionCount(topicId: String, difficulty: Difficulty?): Int = withContext(Dispatchers.IO) {
        val questions = if (difficulty == null) {
            contentDao.getQuestionsForTopic(topicId)
        } else {
            contentDao.getQuestionsForTopicAndDifficulty(topicId, difficulty.name.lowercase())
        }
        questions.size
    }

    override suspend fun getUnseenQuestionCount(ownerId: String, topicId: String, difficulty: Difficulty?): Int = withContext(Dispatchers.IO) {
        val availableQuestions = if (difficulty == null) {
            contentDao.getQuestionsForTopic(topicId)
        } else {
            contentDao.getQuestionsForTopicAndDifficulty(topicId, difficulty.name.lowercase())
        }

        val seenIds = sessionDao.getSeenQuestionIdsForTopic(ownerId, topicId).toSet()
        val reservedIds = sessionDao.getActiveReservedQuestionIdsForTopic(ownerId, topicId).toSet()

        val unseen = availableQuestions.filter { it.id !in seenIds && it.id !in reservedIds }
        unseen.size
    }

    override suspend fun createPracticeSession(
        ownerId: String,
        topicId: String,
        difficulty: Difficulty?,
        requestedCount: Int
    ): PracticeSession = withContext(Dispatchers.IO) {
        val topic = contentDao.getTopicById(topicId)
        val topicTitle = topic?.title ?: "Practice Topic"
        val categoryName = topic?.categoryName ?: "General Aptitude"

        val availableQuestions = if (difficulty == null) {
            contentDao.getQuestionsForTopic(topicId)
        } else {
            contentDao.getQuestionsForTopicAndDifficulty(topicId, difficulty.name.lowercase())
        }

        val seenIds = sessionDao.getSeenQuestionIdsForTopic(ownerId, topicId).toSet()
        val reservedIds = sessionDao.getActiveReservedQuestionIdsForTopic(ownerId, topicId).toSet()

        val unseenQuestions = availableQuestions.filter { it.id !in seenIds && it.id !in reservedIds }.shuffled()
        val seenQuestions = availableQuestions.filter { it.id in seenIds || it.id in reservedIds }.shuffled()

        val selectedQuestions = mutableListOf<QuestionEntity>()

        val unseenToTake = unseenQuestions.take(requestedCount)
        selectedQuestions.addAll(unseenToTake)

        if (selectedQuestions.size < requestedCount) {
            val needed = requestedCount - selectedQuestions.size
            val selectedIds = selectedQuestions.map { q -> q.id }.toSet()
            val availableSeen = seenQuestions.filter { it.id !in selectedIds }
            selectedQuestions.addAll(availableSeen.take(needed))
        }

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

        val snapshotEntities = selectedQuestions.mapIndexed { index, q ->
            val isBookmarked = bookmarkDao.isBookmarked(ownerId, q.id)
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

        return@withContext mapSessionToDomain(sessionEntity, snapshotEntities)
    }

    override suspend fun createRetrySession(ownerId: String, originalSessionId: String): PracticeSession? = withContext(Dispatchers.IO) {
        val originalSession = sessionDao.getSessionById(originalSessionId) ?: return@withContext null
        val originalSnapshots = sessionDao.getSnapshotsForSession(originalSessionId)
        val incorrectSnapshots = originalSnapshots.filter { it.isSubmitted && it.selectedOptionId != null && it.selectedOptionId != it.correctOptionId }

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

        sessionDao.insertSession(newSessionEntity)
        sessionDao.insertSnapshots(newSnapshots)

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
        }
    }

    override suspend fun getSessionById(sessionId: String): PracticeSession? = withContext(Dispatchers.IO) {
        val sessionEntity = sessionDao.getSessionById(sessionId) ?: return@withContext null
        val snapshots = sessionDao.getSnapshotsForSession(sessionId)
        return@withContext mapSessionToDomain(sessionEntity, snapshots)
    }

    override fun getSessionSnapshotsFlow(sessionId: String): Flow<List<SessionSnapshot>> {
        return sessionDao.getSnapshotsForSessionFlow(sessionId).map { list ->
            list.map { mapSnapshotToDomain(it) }
        }
    }

    override suspend fun saveAnswerSelection(sessionId: String, position: Int, selectedOptionId: String?) = withContext(Dispatchers.IO) {
        val snapshots = sessionDao.getSnapshotsForSession(sessionId)
        val snapshot = snapshots.find { it.position == position } ?: return@withContext
        if (snapshot.isSubmitted) return@withContext

        val updated = snapshot.copy(selectedOptionId = selectedOptionId)
        sessionDao.updateSnapshot(updated)
    }

    override suspend fun submitQuestionAnswer(sessionId: String, position: Int) = withContext(Dispatchers.IO) {
        val snapshots = sessionDao.getSnapshotsForSession(sessionId)
        val snapshot = snapshots.find { it.position == position } ?: return@withContext
        if (snapshot.isSubmitted) return@withContext

        val updated = snapshot.copy(isSubmitted = true)
        sessionDao.updateSnapshot(updated)
    }

    override suspend fun updateCurrentPosition(sessionId: String, position: Int) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionById(sessionId) ?: return@withContext
        if (session.currentQuestionIndex != position) {
            val updated = session.copy(currentQuestionIndex = position)
            sessionDao.updateSession(updated)
        }
    }

    override suspend fun unlockExtraHint(
        sessionId: String,
        position: Int,
        questionId: String,
        ownerId: String
    ): Unit = withContext(Dispatchers.IO) {
        sessionDao.unlockExtraHintOwned(
            sessionId = sessionId,
            position = position,
            questionId = questionId,
            ownerId = ownerId
        )
        Unit
    }

    override suspend fun finalizeSession(sessionId: String, ownerId: String): PracticeSession? = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionById(sessionId) ?: return@withContext null
        if (session.status == "COMPLETED") {
            return@withContext getSessionById(sessionId)
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

        val payloadObj = if (ownerId != "guest" && ownerId.isNotEmpty()) {
            JSONObject().apply {
                put("attemptId", sessionId)
                put("ownerId", ownerId)
                put("attemptType", "PRACTICE")
                put("topicId", session.topicId)
                put("topicTitle", session.topicTitle)
                put("categoryName", session.categoryName)
                put("difficultyFilter", session.difficultyFilter)
                put("completionTime", now)
                put("requestedCount", total)
                put("presentedCount", total)
                put("answeredCount", answered)
                put("correctCount", correct)
                put("wrongCount", answered - correct)
                put("skippedCount", total - answered)
                put("score", score)
                put("maxMarks", total)
                put("scorePercentage", if (total > 0) (score.toFloat() / total) * 100f else 0f)
                put("accuracyPercentage", accuracy)
                put("questionSnapshots", JSONArray().apply {
                    finalizedSnapshots.forEach { snap ->
                        put(JSONObject().apply {
                            put("questionId", snap.questionId)
                            put("selectedOptionId", snap.selectedOptionId ?: "")
                            put("correctOptionId", snap.correctOptionId)
                            put("isCorrect", snap.selectedOptionId == snap.correctOptionId)
                            put("isSubmitted", snap.isSubmitted)
                        })
                    }
                })
            }
        } else null

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

            if (payloadObj != null) {
                val outboxItem = SyncOutboxEntity(
                    id = "${ownerId}_PRACTICE_$sessionId",
                    ownerId = ownerId,
                    recordId = sessionId,
                    recordType = "PRACTICE",
                    action = "UPSERT",
                    payloadJson = payloadObj.toString(),
                    status = "PENDING",
                    retryCount = 0,
                    createdAt = now,
                    lastAttemptedAt = null
                )
                database.syncOutboxDao().insertOutboxItem(outboxItem)
            }
        }

        if (payloadObj != null) {
            cloudSyncRepositoryProvider?.invoke()?.triggerSyncNow()
        }

        return@withContext getSessionById(sessionId)
    }

    override suspend fun discardSession(sessionId: String) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionById(sessionId) ?: return@withContext
        val updated = session.copy(status = "DISCARDED")
        sessionDao.updateSession(updated)
    }

    override fun getCompletedSessionsForOwner(ownerId: String): Flow<List<PracticeSession>> {
        return sessionDao.getCompletedSessionsForOwner(ownerId).map { list ->
            list.map { sessionEntity ->
                val snapshots = sessionDao.getSnapshotsForSession(sessionEntity.id)
                mapSessionToDomain(sessionEntity, snapshots)
            }
        }
    }

    override suspend fun toggleBookmark(ownerId: String, snapshot: SessionSnapshot, topicTitle: String): Boolean = withContext(Dispatchers.IO) {
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
            sessionDao.updateSnapshot(match.copy(isBookmarked = newBookmarkState))
        }

        val action = if (newBookmarkState) "UPSERT" else "DELETE"
        val payloadObj = JSONObject().apply {
            put("questionId", snapshot.questionId)
            put("active", newBookmarkState)
            put("topicTitle", topicTitle)
            put("questionText", snapshot.questionText)
            put("optionsJson", JSONArray().apply {
                snapshot.options.forEach { opt ->
                    put(JSONObject().apply {
                        put("id", opt.id)
                        put("text", opt.text)
                    })
                }
            }.toString())
            put("correctOptionId", snapshot.correctOptionId)
            put("explanation", snapshot.explanation)
            put("hint", snapshot.hint)
            put("extraHint", snapshot.extraHint)
        }

        cloudSyncRepositoryProvider?.invoke()?.enqueueBookmarkForSync(ownerId, snapshot.questionId, action, payloadObj.toString())

        return@withContext newBookmarkState
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
        }
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
