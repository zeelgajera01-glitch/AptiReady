package com.example.aptiready.data.repository

import android.content.Context
import android.os.SystemClock
import com.example.aptiready.data.local.db.AppDatabase
import com.example.aptiready.data.local.db.MockQuestionSnapshotEntity
import com.example.aptiready.data.local.db.MockTestAttemptEntity
import com.example.aptiready.data.local.db.MockTestEntity
import com.example.aptiready.data.local.db.SyncOutboxEntity
import androidx.room.withTransaction
import com.example.aptiready.data.model.AttemptHistoryItem
import com.example.aptiready.data.model.MockQuestionSnapshot
import com.example.aptiready.data.model.MockTest
import com.example.aptiready.data.model.MockTestAttempt
import com.example.aptiready.data.model.QuestionOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MockTestRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase,
    private val cloudSyncRepositoryProvider: (() -> CloudSyncRepository)? = null
) : MockTestRepository {

    private val mockTestDao = database.mockTestDao()
    private val contentDao = database.contentDao()
    private val sessionDao = database.practiceSessionDao()
    private val bookmarkDao = database.bookmarkDao()

    override fun getMockTestsFlow(): Flow<List<MockTest>> {
        return mockTestDao.getMockTestsFlow().map { list ->
            list.map { mapTestToDomain(it) }
        }.distinctUntilChanged().flowOn(Dispatchers.IO)
    }

    override suspend fun getMockTestById(testId: String): MockTest? = withContext(Dispatchers.IO) {
        val entity = mockTestDao.getMockTestById(testId) ?: return@withContext null
        mapTestToDomain(entity)
    }

    override suspend fun createMockAttempt(ownerId: String, testId: String): Result<MockTestAttempt> = withContext(Dispatchers.IO) {
        val test = getMockTestById(testId) ?: return@withContext Result.failure(IllegalArgumentException("Mock test '$testId' not found."))

        val resolvedQuestions = mutableListOf<com.example.aptiready.data.local.db.QuestionEntity>()
        for (qId in test.questionIds) {
            val q = contentDao.getQuestionById(qId)

            if (q == null) {
                return@withContext Result.failure(IllegalStateException("Referenced question '$qId' is missing from local bank."))
            }
            resolvedQuestions.add(q)
        }

        if (resolvedQuestions.size != test.questionIds.size) {
            return@withContext Result.failure(IllegalStateException("Failed to resolve all ${test.questionIds.size} test questions."))
        }

        val attemptId = "mock_attempt_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val bootTime = SystemClock.elapsedRealtime()

        val attemptEntity = MockTestAttemptEntity(
            id = attemptId,
            ownerId = ownerId,
            testId = test.id,
            testTitle = test.title,
            testDescription = test.description,
            testVersion = test.version,
            durationSeconds = test.durationSeconds,
            marksPerCorrect = test.marksPerCorrect,
            status = "IN_PROGRESS",
            currentQuestionIndex = 0,
            elapsedSeconds = 0,
            startTimeMillis = now,
            bootTimeMarker = bootTime,
            lastSavedTimeMillis = now,
            remainingSeconds = test.durationSeconds,
            earnedMarks = 0,
            maxMarks = test.questionIds.size * test.marksPerCorrect,
            scorePercentage = 0f,
            accuracyPercentage = 0f,
            finishReason = null,
            createdAt = now,
            completedAt = null
        )

        val snapshotEntities = resolvedQuestions.mapIndexed { index, q ->
            val isBookmarked = bookmarkDao.isBookmarked(ownerId, q.id)
            MockQuestionSnapshotEntity(
                id = "${attemptId}_$index",
                attemptId = attemptId,
                questionId = q.id,
                position = index,
                questionText = q.questionText,
                optionsJson = q.optionsJson,
                correctOptionId = q.correctOptionId,
                explanation = q.explanation,
                hint = q.hint,
                selectedOptionId = null,
                isMarkedForReview = false,
                isVisited = index == 0,
                isBookmarked = isBookmarked
            )
        }

        database.withTransaction {
            mockTestDao.insertAttempt(attemptEntity)
            mockTestDao.insertSnapshots(snapshotEntities)
        }

        return@withContext Result.success(mapAttemptToDomain(attemptEntity, snapshotEntities))
    }

    override suspend fun getInProgressAttempt(ownerId: String): MockTestAttempt? = withContext(Dispatchers.IO) {
        val entity = mockTestDao.getInProgressAttempt(ownerId) ?: return@withContext null
        val snapshots = mockTestDao.getSnapshotsForAttempt(entity.id)
        val recalculatedEntity = recalculateTimerOnRecovery(entity)
        return@withContext mapAttemptToDomain(recalculatedEntity, snapshots)
    }

    override fun getInProgressAttemptFlow(ownerId: String): Flow<MockTestAttempt?> {
        return mockTestDao.getInProgressAttemptFlow(ownerId).map { entity ->
            if (entity == null) null
            else {
                val snapshots = mockTestDao.getSnapshotsForAttempt(entity.id)
                val recalculatedEntity = recalculateTimerOnRecovery(entity)
                mapAttemptToDomain(recalculatedEntity, snapshots)
            }
        }.distinctUntilChanged().flowOn(Dispatchers.IO)
    }

    override suspend fun getAttemptById(attemptId: String): MockTestAttempt? = withContext(Dispatchers.IO) {
        val entity = mockTestDao.getAttemptById(attemptId) ?: return@withContext null
        val snapshots = mockTestDao.getSnapshotsForAttempt(attemptId)
        val recalculatedEntity = if (entity.status == "IN_PROGRESS") recalculateTimerOnRecovery(entity) else entity
        return@withContext mapAttemptToDomain(recalculatedEntity, snapshots)
    }

    override fun getAttemptSnapshotsFlow(attemptId: String): Flow<List<MockQuestionSnapshot>> {
        return mockTestDao.getSnapshotsForAttemptFlow(attemptId).map { list ->
            list.map { mapSnapshotToDomain(it) }
        }.distinctUntilChanged().flowOn(Dispatchers.IO)
    }

    override suspend fun saveOptionSelection(attemptId: String, position: Int, selectedOptionId: String?) = withContext(Dispatchers.IO) {
        mockTestDao.saveSelection(attemptId, position, selectedOptionId)
        Unit
    }

    override suspend fun toggleMarkForReview(attemptId: String, position: Int): Boolean = withContext(Dispatchers.IO) {
        mockTestDao.toggleReview(attemptId, position)
        mockTestDao.getSnapshotsForAttempt(attemptId).find { it.position == position }?.isMarkedForReview ?: false
    }

    override suspend fun markPositionVisited(attemptId: String, position: Int) = withContext(Dispatchers.IO) {
        mockTestDao.markVisited(attemptId, position)
        Unit
    }

    override suspend fun updateCurrentPosition(attemptId: String, position: Int) = withContext(Dispatchers.IO) {
        mockTestDao.savePosition(attemptId, position)
        Unit
    }

    override suspend fun saveTimerProgress(attemptId: String, remainingSeconds: Int, elapsedSeconds: Int) = withContext(Dispatchers.IO) {
        mockTestDao.saveTimer(attemptId, maxOf(0, remainingSeconds), elapsedSeconds, System.currentTimeMillis(), SystemClock.elapsedRealtime())
        Unit
    }

    override suspend fun finalizeAttempt(attemptId: String, finishReason: String): MockTestAttempt? = withContext(Dispatchers.IO) {
        database.withTransaction completion@ {
        val storedAttempt = mockTestDao.getAttemptById(attemptId)
            ?: return@completion null

        val attempt = recalculateTimerOnRecovery(storedAttempt)
        if (attempt.status == "COMPLETED") {
            return@completion getAttemptById(attemptId)
        }

        val snapshots = mockTestDao.getSnapshotsForAttempt(attemptId)
        var earnedMarks = 0
        var answeredCount = 0

        snapshots.forEach { snap ->
            if (snap.selectedOptionId != null) {
                answeredCount++
                if (snap.selectedOptionId == snap.correctOptionId) {
                    earnedMarks += attempt.marksPerCorrect
                }
            }
        }

        val maxMarks = attempt.maxMarks
        val scorePct = if (maxMarks > 0) (earnedMarks.toFloat() / maxMarks.toFloat()) * 100f else 0f
        val accuracyPct = if (answeredCount > 0) ((earnedMarks / attempt.marksPerCorrect).toFloat() / answeredCount.toFloat()) * 100f else 0f
        val now = System.currentTimeMillis()

        val payloadObj = if (attempt.ownerId != "guest" && attempt.ownerId.isNotEmpty()) {
            val totalQs = snapshots.size
            val wrong = answeredCount - (earnedMarks / attempt.marksPerCorrect)
            val skipped = totalQs - answeredCount
            JSONObject().apply {
                put("attemptId", attemptId)
                put("ownerId", attempt.ownerId)
                put("attemptType", "MOCK_TEST")
                put("testId", attempt.testId)
                put("testTitle", attempt.testTitle)
                put("testDescription", attempt.testDescription)
                put("durationSeconds", attempt.durationSeconds)
                put("elapsedSeconds", attempt.elapsedSeconds)
                put("startTimeMillis", attempt.startTimeMillis)
                put("createdAt", attempt.createdAt)
                put("finishReason", finishReason)
                put("completionTime", now)
                put("requestedCount", totalQs)
                put("presentedCount", totalQs)
                put("answeredCount", answeredCount)
                put("correctCount", earnedMarks / attempt.marksPerCorrect)
                put("wrongCount", if (wrong >= 0) wrong else 0)
                put("skippedCount", if (skipped >= 0) skipped else 0)
                put("earnedMarks", earnedMarks)
                put("maxMarks", maxMarks)
                put("scorePercentage", scorePct)
                put("accuracyPercentage", accuracyPct)
                put("questionSnapshots", JSONArray().apply {
                    snapshots.forEach { snap ->
                        put(JSONObject().apply {
                            put("questionId", snap.questionId)
                            put("selectedOptionId", snap.selectedOptionId ?: "")
                            put("correctOptionId", snap.correctOptionId)
                            put("isCorrect", snap.selectedOptionId == snap.correctOptionId)
                        })
                    }
                })
            }
        } else null

        database.withTransaction {
            val att = mockTestDao.getAttemptById(attemptId) ?: return@withTransaction
            if (att.status == "COMPLETED") return@withTransaction

            val updated = att.copy(
                status = "COMPLETED",
                elapsedSeconds = attempt.elapsedSeconds,
                lastSavedTimeMillis = now,
                earnedMarks = earnedMarks,
                maxMarks = maxMarks,
                scorePercentage = scorePct,
                accuracyPercentage = accuracyPct,
                finishReason = finishReason,
                completedAt = now,
                remainingSeconds = 0
            )
            mockTestDao.updateAttempt(updated)

            if (payloadObj != null) {
                val outboxItem = SyncOutboxEntity(
                    id = "${attempt.ownerId}_MOCK_TEST_$attemptId",
                    ownerId = attempt.ownerId,
                    recordId = attemptId,
                    recordType = "MOCK_TEST",
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

        return@completion getAttemptById(attemptId)
        }
    }

    override suspend fun discardAttempt(attemptId: String) = withContext(Dispatchers.IO) {
        mockTestDao.discardActiveAttempt(attemptId)
        Unit
    }

    override fun getCompletedAttemptsForOwner(ownerId: String): Flow<List<MockTestAttempt>> {
        return mockTestDao.getCompletedAttemptsForOwner(ownerId).map { list ->
            list.map { entity ->
                val snapshots = mockTestDao.getSnapshotsForAttempt(entity.id)
                mapAttemptToDomain(entity, snapshots)
            }
        }.distinctUntilChanged().flowOn(Dispatchers.IO)
    }

    override suspend fun getLastCompletedAttemptForTest(ownerId: String, testId: String): MockTestAttempt? = withContext(Dispatchers.IO) {
        val entity = mockTestDao.getLastCompletedAttemptForTest(ownerId, testId) ?: return@withContext null
        val snapshots = mockTestDao.getSnapshotsForAttempt(entity.id)
        return@withContext mapAttemptToDomain(entity, snapshots)
    }

    override fun getUnifiedAttemptHistory(ownerId: String): Flow<List<AttemptHistoryItem>> {
        val practiceFlow = sessionDao.getCompletedSessionsForOwner(ownerId)
        val mockFlow = mockTestDao.getCompletedAttemptsForOwner(ownerId)

        return combine(practiceFlow, mockFlow) { practiceList, mockList ->
            val items = mutableListOf<AttemptHistoryItem>()

            practiceList.forEach { p ->
                val total = p.requestedQuestionCount
                val scorePct = p.accuracyPercentage
                items.add(
                    AttemptHistoryItem(
                        id = p.id,
                        ownerId = p.ownerId,
                        title = p.topicTitle,
                        categoryOrSubtitle = "${p.categoryName} • Practice Session",
                        type = "PRACTICE",
                        scorePercentage = p.accuracyPercentage,
                        accuracyPercentage = p.accuracyPercentage,
                        earnedSummary = "${p.score}/$total Correct",
                        completedAt = p.completedAt ?: p.createdAt,
                        finishReason = "Completed"
                    )
                )
            }

            mockList.forEach { m ->
                val reasonText = when (m.finishReason) {
                    "TIME_EXPIRED" -> "Time Expired"
                    "MANUAL_SUBMISSION" -> "Submitted"
                    "SYSTEM" -> "System Completion"
                    else -> "Completion Reason Unknown"
                }
                items.add(
                    AttemptHistoryItem(
                        id = m.id,
                        ownerId = m.ownerId,
                        title = m.testTitle,
                        categoryOrSubtitle = "${m.durationSeconds / 60} min limit • Mock Test ($reasonText)",
                        type = "MOCK_TEST",
                        scorePercentage = m.scorePercentage,
                        accuracyPercentage = m.accuracyPercentage,
                        earnedSummary = "${m.earnedMarks}/${m.maxMarks} Marks",
                        completedAt = m.completedAt ?: m.createdAt,
                        finishReason = m.finishReason
                    )
                )
            }

            items.sortedByDescending { it.completedAt }
        }.distinctUntilChanged().flowOn(Dispatchers.IO)
    }

    private fun recalculateTimerOnRecovery(entity: MockTestAttemptEntity): MockTestAttemptEntity {
        if (entity.status != "IN_PROGRESS" || entity.remainingSeconds <= 0) return entity

        val now = System.currentTimeMillis()
        val currentBoot = SystemClock.elapsedRealtime()

        val elapsedSeconds: Int
        if (currentBoot >= entity.bootTimeMarker) {
            // Monotonic clock recovery within same boot
            val elapsedMs = currentBoot - entity.bootTimeMarker
            elapsedSeconds = (elapsedMs / 1000).toInt()
        } else {
            // System rebooted -> fallback to wall clock
            val elapsedMs = maxOf(0L, now - entity.lastSavedTimeMillis)
            elapsedSeconds = (elapsedMs / 1000).toInt()
        }

        val newRemaining = maxOf(0, entity.remainingSeconds - elapsedSeconds)
        val newElapsed = entity.durationSeconds - newRemaining

        return entity.copy(
            remainingSeconds = newRemaining,
            elapsedSeconds = newElapsed,
            lastSavedTimeMillis = now,
            bootTimeMarker = currentBoot
        )
    }

    private fun mapTestToDomain(entity: MockTestEntity): MockTest {
        val qIds = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(entity.questionIdsJson)
            for (i in 0 until jsonArray.length()) {
                qIds.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            // Fallback
        }
        return MockTest(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            questionIds = qIds,
            durationSeconds = entity.durationSeconds,
            marksPerCorrect = entity.marksPerCorrect,
            published = entity.published,
            version = entity.version
        )
    }

    private fun mapAttemptToDomain(
        entity: MockTestAttemptEntity,
        snapshots: List<MockQuestionSnapshotEntity>
    ): MockTestAttempt {
        val domainSnapshots = snapshots.map { mapSnapshotToDomain(it) }
        return MockTestAttempt(
            id = entity.id,
            ownerId = entity.ownerId,
            testId = entity.testId,
            testTitle = entity.testTitle,
            testDescription = entity.testDescription,
            testVersion = entity.testVersion,
            durationSeconds = entity.durationSeconds,
            marksPerCorrect = entity.marksPerCorrect,
            status = entity.status,
            currentQuestionIndex = entity.currentQuestionIndex,
            elapsedSeconds = entity.elapsedSeconds,
            startTimeMillis = entity.startTimeMillis,
            bootTimeMarker = entity.bootTimeMarker,
            lastSavedTimeMillis = entity.lastSavedTimeMillis,
            remainingSeconds = entity.remainingSeconds,
            earnedMarks = entity.earnedMarks,
            maxMarks = entity.maxMarks,
            scorePercentage = entity.scorePercentage,
            accuracyPercentage = entity.accuracyPercentage,
            finishReason = entity.finishReason,
            createdAt = entity.createdAt,
            completedAt = entity.completedAt,
            snapshots = domainSnapshots
        )
    }

    private fun mapSnapshotToDomain(entity: MockQuestionSnapshotEntity): MockQuestionSnapshot {
        val options = parseOptionsJson(entity.optionsJson)
        return MockQuestionSnapshot(
            id = entity.id,
            attemptId = entity.attemptId,
            questionId = entity.questionId,
            position = entity.position,
            questionText = entity.questionText,
            options = options,
            correctOptionId = entity.correctOptionId,
            explanation = entity.explanation,
            hint = entity.hint,
            selectedOptionId = entity.selectedOptionId,
            isMarkedForReview = entity.isMarkedForReview,
            isVisited = entity.isVisited,
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