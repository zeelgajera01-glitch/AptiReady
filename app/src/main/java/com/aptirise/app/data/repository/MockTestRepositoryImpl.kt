package com.aptirise.app.data.repository

import android.content.Context
import android.os.SystemClock
import com.aptirise.app.data.local.db.AppDatabase
import com.aptirise.app.data.local.db.MockQuestionSnapshotEntity
import com.aptirise.app.data.local.db.MockTestAttemptEntity
import com.aptirise.app.data.local.db.MockTestEntity
import androidx.room.withTransaction
import com.aptirise.app.data.model.MockQuestionSnapshot
import com.aptirise.app.data.model.MockTest
import com.aptirise.app.data.model.MockTestAttempt
import com.aptirise.app.data.model.QuestionOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MockTestRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase
) : MockTestRepository {

    private val mockTestDao = database.mockTestDao()
    private val contentDao = database.contentDao()
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

        val resolvedQuestions = mutableListOf<com.aptirise.app.data.local.db.QuestionEntity>()
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