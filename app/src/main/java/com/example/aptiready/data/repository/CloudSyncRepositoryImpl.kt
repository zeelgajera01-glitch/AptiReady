package com.example.aptiready.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.aptiready.data.local.SyncPreferencesRepository
import com.example.aptiready.data.local.db.AppDatabase
import com.example.aptiready.data.local.db.MockQuestionSnapshotEntity
import com.example.aptiready.data.local.db.MockTestAttemptEntity
import com.example.aptiready.data.local.db.PracticeSessionEntity
import com.example.aptiready.data.local.db.SessionQuestionSnapshotEntity
import com.example.aptiready.data.local.db.SyncOutboxEntity
import com.example.aptiready.data.local.db.SyncedAttemptEntity
import com.example.aptiready.data.model.AuthState
import com.example.aptiready.data.model.SyncExecutionResult
import com.example.aptiready.data.model.SyncStatus
import com.example.aptiready.data.remote.CloudSyncWorker
import com.example.aptiready.data.remote.FirebaseConfigManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class CloudSyncRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase,
    private val syncPrefs: SyncPreferencesRepository,
    private val authRepository: AuthRepository
) : CloudSyncRepository {

    private val outboxDao = database.syncOutboxDao()
    private val syncedAttemptDao = database.syncedAttemptDao()
    private val sessionDao = database.practiceSessionDao()
    private val mockTestDao = database.mockTestDao()
    private val contentDao = database.contentDao()
    private val workManager = WorkManager.getInstance(context)

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var accountJob: Job? = null
    @Volatile
    private var activeSessionToken: String = UUID.randomUUID().toString()

    @Volatile
    private var runningSyncToken: String? = null

    @Volatile
    private var successfulSyncToken: String? = null

    @Volatile
    private var failedSyncToken: String? = null

    private val _syncStatus = MutableStateFlow(SyncStatus.INITIALIZING)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _pendingOutboxCount = MutableStateFlow(0)
    override val pendingOutboxCount: StateFlow<Int> = _pendingOutboxCount.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(0L)
    override val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _isBackupEnabled = MutableStateFlow(false)
    override val isBackupEnabled: StateFlow<Boolean> = _isBackupEnabled.asStateFlow()

    private val isConfigured = FirebaseConfigManager.isConfigured(context)

    init {
        repositoryScope.launch {
            var observedUid: String? = null

            authRepository.authState.collectLatest { authState ->
                val accountUid = when (authState) {
                    is AuthState.SignedInVerified -> authState.uid
                    is AuthState.SignedInUnverified ->
                        authRepository.currentUserId
                    else -> null
                }

                val previousUid = observedUid
                observedUid = accountUid

                accountJob?.cancel()

                val currentToken = UUID.randomUUID().toString()
                activeSessionToken = currentToken

                runningSyncToken = null
                successfulSyncToken = null
                failedSyncToken = null

                if (previousUid != null && previousUid != accountUid) {
                    workManager.cancelUniqueWork("cloud_sync_$previousUid")
                }

                _isBackupEnabled.value = false
                _lastSyncTimestamp.value = 0L
                _pendingOutboxCount.value = 0
                _syncStatus.value = SyncStatus.INITIALIZING

                val uid = accountUid ?: "guest"

                if (uid != "guest" && uid.isNotEmpty()) {
                    accountJob = launch {
                        var previousBackupState: Boolean? = null

                        launch {
                            syncPrefs.isBackupEnabled(uid).distinctUntilChanged().collectLatest { enabled ->
                                val backupJustEnabled = previousBackupState == false && enabled
                                previousBackupState = enabled
                                _isBackupEnabled.value = enabled
                                updateSyncStatus()

                                if ((enabled && authRepository.authState.value is AuthState.SignedInVerified) || backupJustEnabled) {
                                    triggerSyncNow()
                                }
                            }
                        }

                        launch {
                            syncPrefs.lastSuccessfulSyncTime(uid).collectLatest { timestamp ->
                                _lastSyncTimestamp.value = timestamp
                            }
                        }

                        launch {
                            outboxDao.getPendingCountFlow(uid).collectLatest {
                                updateSyncStatus()
                            }
                        }

                        launch {
                            outboxDao.getPermanentFailureCountFlow(uid).collectLatest {
                                updateSyncStatus()
                            }
                        }
                    }
                } else {
                    _isBackupEnabled.value = false
                    _lastSyncTimestamp.value = 0L
                    _pendingOutboxCount.value = 0
                    updateSyncStatus()
                }
            }
        }
    }

    private suspend fun updateSyncStatus() {
        val token = activeSessionToken
        val auth = authRepository.authState.value

        if (auth is AuthState.Initializing) {
            withContext(Dispatchers.Main.immediate) {
                if (activeSessionToken == token) {
                    _syncStatus.value = SyncStatus.INITIALIZING
                }
            }
            return
        }

        if (auth !is AuthState.SignedInVerified) {
            val status = if (auth is AuthState.SignedInUnverified) {
                SyncStatus.WAITING_FOR_VERIFICATION
            } else {
                SyncStatus.OFF
            }

            withContext(Dispatchers.Main.immediate) {
                if (activeSessionToken == token) {
                    _syncStatus.value = status
                }
            }
            return
        }

        val ownerId = auth.uid

        // Read the real account preference, not a default StateFlow value
        // that may still be waiting for its first emission.
        val backupEnabled = syncPrefs.isBackupEnabled(ownerId).first()

        val queueCounts = database.withTransaction {
            val records = outboxDao.getAllOutboxItemsForOwner(ownerId)

            val pending = records.count {
                it.status == "PENDING" ||
                    it.status == "FAILED" ||
                    it.status == "SYNCING"
            }

            val blocked = records.count {
                it.status == "FAILED_PERMANENT" ||
                    it.status == "BLOCKED_CONFLICT"
            }

            pending to blocked
        }

        withContext(Dispatchers.Main.immediate) {
            val latestAuth = authRepository.authState.value

            if (
                activeSessionToken != token ||
                latestAuth !is AuthState.SignedInVerified ||
                latestAuth.uid != ownerId ||
                authRepository.currentUserId != ownerId
            ) {
                return@withContext
            }

            _isBackupEnabled.value = backupEnabled
            _pendingOutboxCount.value = queueCounts.first

            _syncStatus.value = when {
                !backupEnabled -> SyncStatus.OFF

                queueCounts.second > 0 -> SyncStatus.ERROR

                runningSyncToken == token -> SyncStatus.SYNCING

                failedSyncToken == token -> SyncStatus.ERROR

                queueCounts.first > 0 -> SyncStatus.PENDING

                successfulSyncToken == token -> SyncStatus.UP_TO_DATE

                // Empty queue, but this session has not restored successfully.
                else -> SyncStatus.INITIALIZING
            }
        }
    }

    override suspend fun setBackupEnabled(enabled: Boolean) {
        val uid = authRepository.currentUserId ?: return
        val sessionToken = activeSessionToken

        if (!enabled) {
            _isBackupEnabled.value = false
            workManager.cancelUniqueWork("cloud_sync_$uid")
        }

        syncPrefs.setBackupEnabled(uid, enabled)

        if (
            activeSessionToken != sessionToken ||
            authRepository.currentUserId != uid
        ) {
            return
        }

        _isBackupEnabled.value = enabled
        updateSyncStatus()
    }

    override fun triggerSyncNow() {
        val uid = authRepository.currentUserId ?: return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("ownerId", uid)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork("cloud_sync_$uid", ExistingWorkPolicy.APPEND_OR_REPLACE, syncWork)
    }

    override suspend fun retryPermanentFailures() =
        withContext(Dispatchers.IO) {
            val auth = authRepository.authState.value

            if (auth !is AuthState.SignedInVerified) {
                return@withContext
            }

            val ownerId = auth.uid

            // Convert unresolved duplicate conflicts to BLOCKED_CONFLICT first.
            repairOutboxDuplicates(ownerId)

            if (authRepository.currentUserId != ownerId) {
                return@withContext
            }

            outboxDao.resetPermanentFailuresForOwner(ownerId)

            if (
                _isBackupEnabled.value &&
                authRepository.currentUserId == ownerId
            ) {
                triggerSyncNow()
            }
        }

    private fun requireActiveSyncSession(
        ownerId: String,
        sessionToken: String
    ) {
        val auth = authRepository.authState.value

        if (
            activeSessionToken != sessionToken ||
            auth !is AuthState.SignedInVerified ||
            auth.uid != ownerId ||
            authRepository.currentUserId != ownerId ||
            !_isBackupEnabled.value
        ) {
            throw CancellationException(
                "Sync stopped because the account or backup setting changed."
            )
        }
    }

    override suspend fun performSync(
        expectedOwnerId: String?,
        workerId: String?
    ): SyncExecutionResult {
        val token = activeSessionToken
        val auth = authRepository.authState.value

        if (auth !is AuthState.SignedInVerified) {
            updateSyncStatus()
            return SyncExecutionResult.PERMANENT_FAILURE
        }

        val ownerId = auth.uid

        // Older scheduled work without an owner must not select a new account.
        if (expectedOwnerId.isNullOrBlank() || expectedOwnerId != ownerId) {
            return SyncExecutionResult.COMPLETE
        }

        val enabled = syncPrefs.isBackupEnabled(ownerId).first()

        if (
            activeSessionToken != token ||
            authRepository.currentUserId != ownerId
        ) {
            return SyncExecutionResult.COMPLETE
        }

        _isBackupEnabled.value = enabled

        if (!enabled) {
            updateSyncStatus()
            return SyncExecutionResult.COMPLETE
        }

        if (!isConfigured) {
            failedSyncToken = token
            successfulSyncToken = null
            updateSyncStatus()
            return SyncExecutionResult.PERMANENT_FAILURE
        }

        runningSyncToken = token
        successfulSyncToken = null
        failedSyncToken = null

        try {
            updateSyncStatus()
            requireActiveSyncSession(ownerId, token)

            val result = performSyncInternal(
                expectedOwnerId = ownerId,
                workerId = workerId
            )

            requireActiveSyncSession(ownerId, token)

            when (result) {
                SyncExecutionResult.COMPLETE -> {
                    // The internal implementation must reach COMPLETE only
                    // after its restore and outstanding-record checks.
                    successfulSyncToken = token
                    failedSyncToken = null
                }

                SyncExecutionResult.MORE_WORK_REMAINING -> {
                    successfulSyncToken = null
                    failedSyncToken = null
                }

                SyncExecutionResult.TRANSIENT_FAILURE,
                SyncExecutionResult.PERMANENT_FAILURE -> {
                    successfulSyncToken = null
                    failedSyncToken = token
                }
            }

            return result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (
                activeSessionToken == token &&
                authRepository.currentUserId == ownerId
            ) {
                successfulSyncToken = null
                failedSyncToken = token

                Log.e("CloudSyncRepo", "Sync execution failed", e)
            }

            return SyncExecutionResult.TRANSIENT_FAILURE
        } finally {
            if (activeSessionToken == token) {
                runningSyncToken = null

                // Do not keep the cancelled worker alive just to update UI.
                // Recalculate from the repository's account-scoped state.
                repositoryScope.launch {
                    if (activeSessionToken == token) {
                        updateSyncStatus()
                    }
                }
            }
        }
    }

    private suspend fun performSyncInternal(
        expectedOwnerId: String?,
        workerId: String?
    ): SyncExecutionResult = withContext(Dispatchers.IO) {
        val currentToken = activeSessionToken
        val ownerId = expectedOwnerId!!

        try {
            // Reconcile un-enqueued completed attempts & clean duplicate legacy outbox items
            reconcileUnqueuedCompletedAttempts(ownerId)

            var batchCount = 0
            val maxBatches = 10
            var hasMoreItems = true

            while (hasMoreItems && batchCount < maxBatches) {
                requireActiveSyncSession(ownerId, currentToken)

                val now = System.currentTimeMillis()
                val staleThresholdMs = now - 120000L // 2 mins stale threshold
                val claimedCount = outboxDao.claimPendingItemsForWorker(ownerId, workerId!!, now, staleThresholdMs)

                if (claimedCount == 0) {
                    hasMoreItems = false
                    break
                }

                val claimedItems = outboxDao.getClaimedItemsForWorker(ownerId, workerId)
                if (claimedItems.isEmpty()) {
                    hasMoreItems = false
                    break
                }

                batchCount++

                if (isConfigured) {
                    val firestore = FirebaseFirestore.getInstance()

                    for (item in claimedItems) {
                        requireActiveSyncSession(ownerId, currentToken)

                        try {
                            val payloadObj = JSONObject(item.payloadJson)
                            val map = jsonToMap(payloadObj)
                            map["serverUploadTimestamp"] = FieldValue.serverTimestamp()

                            when (item.recordType) {
                                "PRACTICE" -> {
                                    val docRef = firestore.collection("users").document(ownerId)
                                        .collection("practiceAttempts").document(item.recordId)
                                    docRef.set(map, SetOptions.merge()).await()
                                }
                                "MOCK_TEST" -> {
                                    val docRef = firestore.collection("users").document(ownerId)
                                        .collection("mockAttempts").document(item.recordId)
                                    docRef.set(map, SetOptions.merge()).await()
                                }
                                "BOOKMARK" -> {
                                    val docRef = firestore.collection("users").document(ownerId)
                                        .collection("questionBookmarks").document(item.recordId)
                                    docRef.set(map, SetOptions.merge()).await()
                                }
                            }

                            // Atomic acknowledgement matching owner, worker claim & revision
                            database.withTransaction {
                                requireActiveSyncSession(ownerId, currentToken)

                                val rows = outboxDao.deleteClaimedOutboxItemWithRevision(
                                    item.id,
                                    ownerId,
                                    workerId,
                                    item.revision
                                )

                                if (
                                    rows > 0 &&
                                    (item.recordType == "PRACTICE" ||
                                        item.recordType == "MOCK_TEST")
                                ) {
                                    syncedAttemptDao.insertSyncedAttempt(
                                        SyncedAttemptEntity(
                                            item.recordId,
                                            ownerId,
                                            System.currentTimeMillis()
                                        )
                                    )
                                }

                                requireActiveSyncSession(ownerId, currentToken)
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            requireActiveSyncSession(ownerId, currentToken)

                            if (e is FirebaseFirestoreException && (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED || e.code == FirebaseFirestoreException.Code.INVALID_ARGUMENT)) {
                                Log.e("CloudSyncRepo", "Permanent upload failure for item ${item.id}: ${e.message}")
                                outboxDao.updateItemStatusWithRevision(item.id, ownerId, workerId, item.revision, "FAILED_PERMANENT", System.currentTimeMillis())
                            } else {
                                Log.w("CloudSyncRepo", "Transient upload failure for item ${item.id}: ${e.message}")
                                outboxDao.updateItemStatusWithRevision(item.id, ownerId, workerId, item.revision, "FAILED", System.currentTimeMillis())
                                throw e
                            }
                        }
                    }
                }
            }

            requireActiveSyncSession(ownerId, currentToken)

            val restoreResult = restoreCloudRecordsForSession(
                ownerId,
                currentToken
            )

            requireActiveSyncSession(ownerId, currentToken)

            if (restoreResult.isFailure) {
                val e = restoreResult.exceptionOrNull() ?: Exception("Restore failed")
                syncPrefs.recordSyncError(ownerId, e.localizedMessage ?: "Failed to restore cloud records.")
                return@withContext when (e) {
                    is IllegalArgumentException,
                    is IllegalStateException ->
                        SyncExecutionResult.PERMANENT_FAILURE

                    is FirebaseFirestoreException ->
                        when (e.code) {
                            FirebaseFirestoreException.Code.PERMISSION_DENIED,
                            FirebaseFirestoreException.Code.INVALID_ARGUMENT ->
                                SyncExecutionResult.PERMANENT_FAILURE

                            else -> SyncExecutionResult.TRANSIENT_FAILURE
                        }

                    else -> SyncExecutionResult.TRANSIENT_FAILURE
                }
            }

            // Check if items still remain after processing
            val remainingPending = outboxDao.getAllOutboxItemsForOwner(ownerId).filter { it.status in listOf("PENDING", "FAILED", "SYNCING") }
            val permanentFailures = outboxDao.getPermanentFailureCount(ownerId)

            if (permanentFailures > 0) {
                syncPrefs.recordSyncError(ownerId, "Sync incomplete due to permanent upload errors.")
                return@withContext SyncExecutionResult.PERMANENT_FAILURE
            }

            if (remainingPending.isNotEmpty()) {
                return@withContext SyncExecutionResult.MORE_WORK_REMAINING
            }

            val syncTime = System.currentTimeMillis()
            syncPrefs.recordSuccessfulSync(ownerId, syncTime)
            SyncExecutionResult.COMPLETE
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            requireActiveSyncSession(ownerId, currentToken)

            Log.e("CloudSyncRepo", "Sync error: ${e.message}", e)
            syncPrefs.recordSyncError(ownerId, e.localizedMessage ?: "Sync error occurred.")
            SyncExecutionResult.TRANSIENT_FAILURE
        }
    }

    private fun validateCloudAttempt(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        ownerId: String,
        attemptType: String
    ): List<Map<*, *>> {
        fun invalid(message: String): Nothing {
            throw IllegalArgumentException(
                "Cannot restore ${doc.id}: $message"
            )
        }

        if (doc.getString("ownerId") != ownerId) {
            invalid("owner mismatch")
        }

        if (doc.getString("attemptId") != doc.id) {
            invalid("attempt ID mismatch")
        }

        if (doc.getString("attemptType") != attemptType) {
            invalid("attempt type mismatch")
        }

        val completionTime = doc.getLong("completionTime")
            ?: invalid("missing completion time")

        if (completionTime <= 0L) {
            invalid("invalid completion time")
        }

        val requestedCount = doc.getLong("requestedCount")
            ?: invalid("missing question count")

        if (requestedCount <= 0L || requestedCount > Int.MAX_VALUE.toLong()) {
            invalid("invalid question count")
        }

        val raw = doc.get("questionSnapshots") as? List<*>
            ?: invalid("missing question snapshots")

        if (raw.size.toLong() != requestedCount) {
            invalid("snapshot count does not match question count")
        }

        val seenQuestionIds = mutableSetOf<String>()
        var answered = 0L
        var correct = 0L

        val snapshots = raw.mapIndexed { index, value ->
            val snapshot = value as? Map<*, *>
                ?: invalid("snapshot $index is malformed")

            val questionId = snapshot["questionId"] as? String
                ?: invalid("snapshot $index has no question ID")

            if (questionId.isBlank() || !seenQuestionIds.add(questionId)) {
                invalid("blank or repeated question ID at snapshot $index")
            }

            val correctOptionId = snapshot["correctOptionId"] as? String
                ?: invalid("snapshot $index has no historical answer key")

            if (correctOptionId.isBlank()) {
                invalid("snapshot $index has an empty answer key")
            }

            val rawSelected = snapshot["selectedOptionId"]

            if (rawSelected != null && rawSelected !is String) {
                invalid("snapshot $index has an invalid selected answer")
            }

            val selected = (rawSelected as? String)?.takeUnless {
                it.isBlank() || it == "null"
            }

            val submitted = if (attemptType == "PRACTICE") {
                snapshot["isSubmitted"] as? Boolean
                    ?: invalid("snapshot $index has no submission state")
            } else {
                true
            }

            if (submitted && selected != null) {
                answered++
                if (selected == correctOptionId) {
                    correct++
                }
            }

            snapshot
        }

        if (doc.getLong("answeredCount") != answered) {
            invalid("answered count disagrees with recorded answers")
        }

        if (doc.getLong("correctCount") != correct) {
            invalid("correct count disagrees with historical answer keys")
        }

        if (doc.getLong("wrongCount") != answered - correct) {
            invalid("wrong count disagrees with recorded answers")
        }

        if (doc.getLong("skippedCount") != requestedCount - answered) {
            invalid("skipped count disagrees with recorded answers")
        }

        return snapshots
    }

    override suspend fun restoreCloudRecords(
        ownerId: String
    ): Result<Unit> {
        return restoreCloudRecordsForSession(
            ownerId,
            activeSessionToken
        )
    }

    private suspend fun restoreCloudRecordsForSession(
        ownerId: String,
        sessionToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        requireActiveSyncSession(ownerId, sessionToken)

        if (!isConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Firebase is not configured.")
            )
        }

        try {
            val firestore = FirebaseFirestore.getInstance()

            requireActiveSyncSession(ownerId, sessionToken)

            // 1. Restore Practice Attempts
            val practiceSnap = firestore.collection("users").document(ownerId)
                .collection("practiceAttempts").get().await()

            for (doc in practiceSnap.documents) {
                requireActiveSyncSession(ownerId, sessionToken)

                val attemptId = doc.id

                val validatedSnapshots = validateCloudAttempt(
                    doc = doc,
                    ownerId = ownerId,
                    attemptType = "PRACTICE"
                )

                val topicId = doc.getString("topicId") ?: "percentages"
                val topicTitle = doc.getString("topicTitle") ?: "Practice Topic"
                val categoryName = doc.getString("categoryName") ?: "General Aptitude"
                val diffFilter = doc.getString("difficultyFilter") ?: "all"
                val completionTime = doc.getLong("completionTime") ?: System.currentTimeMillis()
                val reqCount = (doc.getLong("requestedCount") ?: 5L).toInt()
                val score = (doc.getLong("score") ?: 0L).toInt()
                val accuracy = (doc.getDouble("accuracyPercentage") ?: 0.0).toFloat()

                val rawSnapshots = validatedSnapshots
                val snapshotEntities = mutableListOf<SessionQuestionSnapshotEntity>()

                rawSnapshots.forEachIndexed { index, snapObj ->
                    val map = snapObj as? Map<*, *> ?: return@forEachIndexed
                    val qId = map["questionId"] as? String ?: "q_$index"
                    
                    val rawSelectedOpt = map["selectedOptionId"] as? String
                    val selectedOpt = if (rawSelectedOpt.isNullOrEmpty() || rawSelectedOpt == "null") null else rawSelectedOpt
                    val correctOpt = map["correctOptionId"] as? String ?: "a"
                    val isSubmitted = (map["isSubmitted"] as? Boolean) ?: true

                    val localQuestion = resolveQuestionContent(qId)

                    snapshotEntities.add(
                        SessionQuestionSnapshotEntity(
                            id = "${attemptId}_$index",
                            sessionId = attemptId,
                            questionId = qId,
                            position = index,
                            questionText = localQuestion?.questionText ?: "Question #$qId (Content Unavailable)",
                            optionsJson = localQuestion?.optionsJson ?: "[]",
                            correctOptionId = correctOpt, // Preserve historical recorded answer key!
                            explanation = localQuestion?.explanation ?: "Restored from cloud record.",
                            hint = localQuestion?.hint ?: "",
                            selectedOptionId = selectedOpt,
                            isSubmitted = isSubmitted,
                            isBookmarked = false
                        )
                    )
                }

                val sessionEntity = PracticeSessionEntity(
                    id = attemptId,
                    ownerId = ownerId,
                    topicId = topicId,
                    topicTitle = topicTitle,
                    categoryName = categoryName,
                    difficultyFilter = diffFilter,
                    requestedQuestionCount = reqCount,
                    status = "COMPLETED",
                    currentQuestionIndex = maxOf(0, reqCount - 1),
                    score = score,
                    accuracyPercentage = accuracy,
                    createdAt = completionTime,
                    completedAt = completionTime
                )

                database.withTransaction {
                    requireActiveSyncSession(ownerId, sessionToken)

                    val queuedRecord = outboxDao.getOutboxItemByRecord(
                        ownerId,
                        "PRACTICE",
                        attemptId
                    )

                    // Preserve pending uploads, failures, claims, and unresolved conflicts.
                    if (queuedRecord != null) {
                        return@withTransaction
                    }

                    val existing = sessionDao.getSessionById(attemptId)

                    if (existing != null) {
                        check(existing.ownerId == ownerId) {
                            "Practice attempt ID belongs to another local account."
                        }

                        check(existing.status == "COMPLETED") {
                            "Cloud restore conflicts with an unfinished local practice attempt."
                        }

                        check(
                            existing.completedAt == completionTime &&
                                existing.score == score &&
                                existing.topicId == topicId &&
                                existing.requestedQuestionCount == reqCount
                        ) {
                            "Cloud practice metadata conflicts with local history."
                        }

                        val localSnapshots = sessionDao.getSnapshotsForSession(attemptId)

                        for (local in localSnapshots) {
                            val remote = snapshotEntities.getOrNull(local.position)
                                ?: throw IllegalStateException(
                                    "Local practice history contains an unexpected position."
                                )

                            check(
                                local.questionId == remote.questionId &&
                                    local.selectedOptionId
                                        ?.takeUnless { it.isBlank() || it == "null" } ==
                                        remote.selectedOptionId &&
                                    local.correctOptionId == remote.correctOptionId &&
                                    local.isSubmitted == remote.isSubmitted
                            ) {
                                "Cloud practice answers conflict with local history."
                            }
                        }
                    }

                    if (existing == null) {
                        sessionDao.insertSession(sessionEntity)
                        sessionDao.insertSnapshots(snapshotEntities)
                    } else {
                        val localSnapshots = sessionDao.getSnapshotsForSession(attemptId)

                        check(
                            localSnapshots.map { it.position }.distinct().size ==
                                localSnapshots.size
                        ) {
                            "Local practice history contains duplicate positions."
                        }

                        check(
                            localSnapshots.map { it.questionId }.distinct().size ==
                                localSnapshots.size
                        ) {
                            "Local practice history contains duplicate question IDs."
                        }

                        val localByQuestionId = localSnapshots.associateBy { it.questionId }

                        val mergedSnapshots = snapshotEntities.map { remote ->
                            val local = localByQuestionId[remote.questionId]

                            if (local == null) {
                                remote
                            } else {
                                val keepLocalContent = hasUsableSnapshotContent(
                                    local.questionText,
                                    local.optionsJson
                                )

                                remote.copy(
                                    questionText = if (keepLocalContent) local.questionText else remote.questionText,
                                    optionsJson = if (keepLocalContent) local.optionsJson else remote.optionsJson,
                                    explanation = if (keepLocalContent) local.explanation else remote.explanation,
                                    hint = if (keepLocalContent) local.hint else remote.hint,
                                    isBookmarked = local.isBookmarked
                                )
                            }
                        }

                        if (localSnapshots != mergedSnapshots) {
                            sessionDao.deleteSnapshotsForSession(attemptId)
                            sessionDao.insertSnapshots(mergedSnapshots)
                        }
                    }

                    syncedAttemptDao.insertSyncedAttempt(SyncedAttemptEntity(attemptId, ownerId, System.currentTimeMillis()))
                    requireActiveSyncSession(ownerId, sessionToken)
                }
            }

            requireActiveSyncSession(ownerId, sessionToken)

            // 2. Restore Mock Attempts
            val mockSnap = firestore.collection("users").document(ownerId)
                .collection("mockAttempts").get().await()

            for (doc in mockSnap.documents) {
                requireActiveSyncSession(ownerId, sessionToken)

                val attemptId = doc.id

                val validatedSnapshots = validateCloudAttempt(
                    doc = doc,
                    ownerId = ownerId,
                    attemptType = "MOCK_TEST"
                )

                val testId = doc.getString("testId") ?: "quant_sprint_01"
                val testTitle = doc.getString("testTitle") ?: "Mock Test"
                val testDesc = doc.getString("testDescription") ?: "Timed Assessment"
                val durationSecs = (doc.getLong("durationSeconds") ?: 900L).toInt()

                val completionTime = requireNotNull(doc.getLong("completionTime")) {
                    "Mock attempt ${doc.id} has no completion time."
                }

                val restoredElapsedSeconds = doc.getLong("elapsedSeconds")?.let { value ->
                    require(value >= 0L && value <= durationSecs.toLong()) {
                        "Mock attempt ${doc.id} has invalid elapsed time."
                    }
                    value.toInt()
                } ?: -1

                val restoredStartTime = doc.getLong("startTimeMillis")?.let { value ->
                    require(value > 0L && value <= completionTime) {
                        "Mock attempt ${doc.id} has invalid start time."
                    }
                    value
                } ?: -1L

                val restoredCreatedAt = doc.getLong("createdAt")?.let { value ->
                    require(value > 0L && value <= completionTime) {
                        "Mock attempt ${doc.id} has invalid creation time."
                    }
                    value
                } ?: -1L

                val restoredFinishReason = doc.getString("finishReason")
                    ?.takeUnless { it.isBlank() || it == "Cloud Restore" }
                    ?: "UNKNOWN"

                val earnedMarks = (doc.getLong("earnedMarks") ?: 0L).toInt()
                val maxMarks = (doc.getLong("maxMarks") ?: 10L).toInt()
                val scorePct = (doc.getDouble("scorePercentage") ?: 0.0).toFloat()
                val accuracyPct = (doc.getDouble("accuracyPercentage") ?: 0.0).toFloat()

                val rawSnapshots = validatedSnapshots
                val snapshotEntities = mutableListOf<MockQuestionSnapshotEntity>()

                rawSnapshots.forEachIndexed { index, snapObj ->
                    val map = snapObj as? Map<*, *> ?: return@forEachIndexed
                    val qId = map["questionId"] as? String ?: "q_$index"

                    val rawSelectedOpt = map["selectedOptionId"] as? String
                    val selectedOpt = if (rawSelectedOpt.isNullOrEmpty() || rawSelectedOpt == "null") null else rawSelectedOpt
                    val correctOpt = map["correctOptionId"] as? String ?: "a"

                    val localQuestion = resolveQuestionContent(qId)

                    snapshotEntities.add(
                        MockQuestionSnapshotEntity(
                            id = "${attemptId}_$index",
                            attemptId = attemptId,
                            questionId = qId,
                            position = index,
                            questionText = localQuestion?.questionText ?: "Question #$qId (Content Unavailable)",
                            optionsJson = localQuestion?.optionsJson ?: "[]",
                            correctOptionId = correctOpt, // Preserve historical recorded answer key!
                            explanation = localQuestion?.explanation ?: "Restored from cloud record.",
                            hint = localQuestion?.hint ?: "",
                            selectedOptionId = selectedOpt,
                            isMarkedForReview = false,
                            isVisited = true,
                            isBookmarked = false
                        )
                    )
                }

                val mockEntity = MockTestAttemptEntity(
                    id = attemptId,
                    ownerId = ownerId,
                    testId = testId,
                    testTitle = testTitle,
                    testDescription = testDesc,
                    testVersion = 1,
                    durationSeconds = durationSecs,
                    marksPerCorrect = 1,
                    status = "COMPLETED",
                    currentQuestionIndex = 0,
                    elapsedSeconds = restoredElapsedSeconds,
                    startTimeMillis = restoredStartTime,
                    bootTimeMarker = 0L,
                    lastSavedTimeMillis = completionTime,
                    remainingSeconds = 0,
                    earnedMarks = earnedMarks,
                    maxMarks = maxMarks,
                    scorePercentage = scorePct,
                    accuracyPercentage = accuracyPct,
                    finishReason = restoredFinishReason,
                    createdAt = restoredCreatedAt,
                    completedAt = completionTime
                )

                database.withTransaction {
                    requireActiveSyncSession(ownerId, sessionToken)

                    val queuedRecord = outboxDao.getOutboxItemByRecord(
                        ownerId,
                        "MOCK_TEST",
                        attemptId
                    )

                    if (queuedRecord != null) {
                        return@withTransaction
                    }

                    val existing = mockTestDao.getAttemptById(attemptId)

                    if (existing != null) {
                        check(existing.ownerId == ownerId) {
                            "Mock attempt ID belongs to another local account."
                        }

                        check(existing.status == "COMPLETED") {
                            "Cloud restore conflicts with an unfinished local mock attempt."
                        }

                        check(
                            existing.completedAt == completionTime &&
                                existing.testId == testId &&
                                existing.earnedMarks == earnedMarks &&
                                existing.maxMarks == maxMarks
                        ) {
                            "Cloud mock metadata conflicts with local history."
                        }

                        val localSnapshots = mockTestDao.getSnapshotsForAttempt(attemptId)

                        for (local in localSnapshots) {
                            val remote = snapshotEntities.getOrNull(local.position)
                                ?: throw IllegalStateException(
                                    "Local mock history contains an unexpected position."
                                )

                            check(
                                local.questionId == remote.questionId &&
                                    local.selectedOptionId
                                        ?.takeUnless { it.isBlank() || it == "null" } ==
                                        remote.selectedOptionId &&
                                    local.correctOptionId == remote.correctOptionId
                            ) {
                                "Cloud mock answers conflict with local history."
                            }
                        }
                    }

                    if (existing == null) {
                        mockTestDao.insertAttempt(mockEntity)
                        mockTestDao.insertSnapshots(snapshotEntities)
                    } else {
                        val localSnapshots = mockTestDao.getSnapshotsForAttempt(attemptId)

                        check(
                            localSnapshots.map { it.position }.distinct().size ==
                                localSnapshots.size
                        ) {
                            "Local mock history contains duplicate positions."
                        }

                        check(
                            localSnapshots.map { it.questionId }.distinct().size ==
                                localSnapshots.size
                        ) {
                            "Local mock history contains duplicate question IDs."
                        }

                        val localByQuestionId = localSnapshots.associateBy { it.questionId }

                        val mergedSnapshots = snapshotEntities.map { remote ->
                            val local = localByQuestionId[remote.questionId]

                            if (local == null) {
                                remote
                            } else {
                                val keepLocalContent = hasUsableSnapshotContent(
                                    local.questionText,
                                    local.optionsJson
                                )

                                remote.copy(
                                    questionText = if (keepLocalContent) local.questionText else remote.questionText,
                                    optionsJson = if (keepLocalContent) local.optionsJson else remote.optionsJson,
                                    explanation = if (keepLocalContent) local.explanation else remote.explanation,
                                    hint = if (keepLocalContent) local.hint else remote.hint,
                                    isBookmarked = local.isBookmarked,
                                    isMarkedForReview = local.isMarkedForReview,
                                    isVisited = local.isVisited
                                )
                            }
                        }

                        if (localSnapshots != mergedSnapshots) {
                            mockTestDao.deleteSnapshotsForAttempt(attemptId)
                            mockTestDao.insertSnapshots(mergedSnapshots)
                        }
                    }

                    if (
                        existing != null &&
                        existing.status == "COMPLETED" &&
                        existing.finishReason == "Cloud Restore"
                    ) {
                        mockTestDao.updateAttempt(
                            existing.copy(
                                elapsedSeconds = restoredElapsedSeconds,
                                startTimeMillis = restoredStartTime,
                                createdAt = restoredCreatedAt,
                                finishReason = restoredFinishReason
                            )
                        )
                    }

                    syncedAttemptDao.insertSyncedAttempt(SyncedAttemptEntity(attemptId, ownerId, System.currentTimeMillis()))
                    requireActiveSyncSession(ownerId, sessionToken)
                }
            }

            requireActiveSyncSession(ownerId, sessionToken)

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("CloudSyncRepo", "Restore error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun resolveQuestionContent(questionId: String): com.example.aptiready.data.local.db.QuestionEntity? {
        val topicIds = listOf("percentages", "ratios", "averages", "profit_loss", "time_work", "number_series", "directions", "syllogisms", "grammar_vocab", "reading_comp")
        for (tid in topicIds) {
            val match = contentDao.getQuestionsForTopic(tid).find { it.id == questionId }
            if (match != null) return match
        }
        return null
    }

    private fun hasUsableSnapshotContent(
        questionText: String,
        optionsJson: String
    ): Boolean {
        if (
            questionText.isBlank() ||
            questionText.startsWith("Question #") ||
            questionText.contains("Content Unavailable")
        ) {
            return false
        }

        return try {
            val options = JSONArray(optionsJson)
            if (options.length() != 4) {
                false
            } else {
                val ids = mutableSetOf<String>()

                (0 until options.length()).all { index ->
                    val option = options.optJSONObject(index)
                    val id = option?.optString("id").orEmpty()
                    val text = option?.optString("text").orEmpty()

                    id.isNotBlank() &&
                        text.isNotBlank() &&
                        ids.add(id)
                }
            }
        } catch (_: org.json.JSONException) {
            false
        }
    }

    private suspend fun repairOutboxDuplicates(ownerId: String) {
        database.withTransaction {
            val groups = outboxDao
                .getAllOutboxItemsForOwner(ownerId)
                .groupBy { it.recordType to it.recordId }

            for ((_, items) in groups) {
                // Cleanup must never take over an upload claim.
                // Interrupted claim recovery is handled separately.
                if (items.any { it.status == "SYNCING" }) {
                    continue
                }

                // Conflicts require explicit resolution, not automatic selection.
                if (items.any { it.status == "BLOCKED_CONFLICT" }) {
                    for (item in items) {
                        if (item.status != "BLOCKED_CONFLICT") {
                            outboxDao.insertOutboxItem(
                                item.copy(
                                    status = "BLOCKED_CONFLICT",
                                    workerId = null,
                                    claimedAt = null
                                )
                            )
                        }
                    }
                    continue
                }

                val firstItem = items.first()
                val recordType = firstItem.recordType
                val canonicalId =
                    "${ownerId}_${recordType}_${firstItem.recordId}"

                val payloads = mutableMapOf<String, JSONObject>()
                var conflict = false

                for (item in items) {
                    if (item.action != "UPSERT" && item.action != "DELETE") {
                        conflict = true
                        break
                    }

                    try {
                        payloads[item.id] = JSONObject(item.payloadJson)
                    } catch (_: org.json.JSONException) {
                        conflict = true
                        break
                    }
                }

                var selected: SyncOutboxEntity? = null

                if (!conflict) {
                    when (recordType) {
                        "PRACTICE", "MOCK_TEST" -> {
                            val firstPayload = payloads.getValue(firstItem.id)

                            // Completed attempts must have equivalent full payloads.
                            conflict = items.any { item ->
                                item.action != "UPSERT" ||
                                    !arePayloadsEquivalent(
                                        firstPayload,
                                        payloads.getValue(item.id)
                                    )
                            }

                            if (!conflict) {
                                selected = items.firstOrNull {
                                    it.id == canonicalId
                                } ?: firstItem
                            }
                        }

                        "BOOKMARK" -> {
                            val newestTime = items.maxOf { it.createdAt }
                            val newestItems = items.filter {
                                it.createdAt == newestTime
                            }

                            val candidate = newestItems.first()
                            val candidatePayload = payloads.getValue(candidate.id)

                            // Equal timestamps cannot resolve different intentions.
                            conflict = newestItems.any { item ->
                                item.action != candidate.action ||
                                    !arePayloadsEquivalent(
                                        candidatePayload,
                                        payloads.getValue(item.id)
                                    )
                            }

                            if (!conflict) {
                                selected = newestItems.firstOrNull {
                                    it.id == canonicalId
                                } ?: candidate
                            }
                        }

                        else -> conflict = true
                    }
                }

                if (conflict) {
                    // Preserve every original payload and ID for later resolution.
                    for (item in items) {
                        outboxDao.insertOutboxItem(
                            item.copy(
                                status = "BLOCKED_CONFLICT",
                                workerId = null,
                                claimedAt = null
                            )
                        )
                    }
                    continue
                }

                val chosen = selected ?: continue

                // Preserve permanent failures conservatively.
                val resultingStatus =
                    if (items.any { it.status == "FAILED_PERMANENT" }) {
                        "FAILED_PERMANENT"
                    } else {
                        chosen.status
                    }

                val canonicalItem = chosen.copy(
                    id = canonicalId,
                    status = resultingStatus,
                    revision = items.maxOf { it.revision },
                    workerId = null,
                    claimedAt = null
                )

                outboxDao.insertOutboxItem(canonicalItem)

                // The canonical payload is safely persisted before old IDs go away.
                for (item in items) {
                    if (item.id != canonicalId) {
                        outboxDao.deleteOutboxItem(item.id)
                    }
                }
            }
        }
    }

    private suspend fun reconcileUnqueuedCompletedAttempts(ownerId: String) {
        if (ownerId == "guest" || ownerId.isBlank()) return

        repairOutboxDuplicates(ownerId)

        database.withTransaction {
            // 2. Reconcile Completed Practice Sessions
            val practiceList = sessionDao.getCompletedSessionsListForOwner(ownerId)
            for (p in practiceList) {
                if (syncedAttemptDao.isSynced(p.id)) continue

                val existingRecord = outboxDao.getOutboxItemByRecord(ownerId, "PRACTICE", p.id)
                if (existingRecord == null) {
                    val snapshots = sessionDao.getSnapshotsForSession(p.id)
                    val total = snapshots.size
                    val correct = snapshots.count { it.isSubmitted && it.selectedOptionId == it.correctOptionId }
                    val answered = snapshots.count { it.isSubmitted && it.selectedOptionId != null }

                    val payloadObj = JSONObject().apply {
                        put("attemptId", p.id)
                        put("ownerId", ownerId)
                        put("attemptType", "PRACTICE")
                        put("topicId", p.topicId)
                        put("topicTitle", p.topicTitle)
                        put("categoryName", p.categoryName)
                        put("difficultyFilter", p.difficultyFilter)
                        put("completionTime", p.completedAt ?: p.createdAt)
                        put("requestedCount", total)
                        put("presentedCount", total)
                        put("answeredCount", answered)
                        put("correctCount", correct)
                        put("wrongCount", answered - correct)
                        put("skippedCount", total - answered)
                        put("score", p.score)
                        put("maxMarks", total)
                        put("scorePercentage", if (total > 0) (p.score.toFloat() / total) * 100f else 0f)
                        put("accuracyPercentage", p.accuracyPercentage)
                        put("questionSnapshots", JSONArray().apply {
                            snapshots.forEach { snap ->
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

                    val outboxItem = SyncOutboxEntity(
                        id = "${ownerId}_PRACTICE_${p.id}",
                        ownerId = ownerId,
                        recordId = p.id,
                        recordType = "PRACTICE",
                        action = "UPSERT",
                        payloadJson = payloadObj.toString(),
                        status = "PENDING",
                        retryCount = 0,
                        createdAt = p.completedAt ?: p.createdAt,
                        lastAttemptedAt = null
                    )
                    outboxDao.insertOutboxItem(outboxItem)
                }
            }

            // 3. Reconcile Completed Mock Attempts
            val mockList = mockTestDao.getCompletedAttemptsListForOwner(ownerId)
            for (m in mockList) {
                if (syncedAttemptDao.isSynced(m.id)) continue

                val existingRecord = outboxDao.getOutboxItemByRecord(ownerId, "MOCK_TEST", m.id)
                if (existingRecord == null) {
                    val snapshots = mockTestDao.getSnapshotsForAttempt(m.id)
                    val totalQs = snapshots.size
                    val answeredCount = snapshots.count { it.selectedOptionId != null }
                    val earnedMarks = m.earnedMarks
                    val marksPerCorrect = if (m.marksPerCorrect > 0) m.marksPerCorrect else 1
                    val wrong = answeredCount - (earnedMarks / marksPerCorrect)
                    val skipped = totalQs - answeredCount

                    val payloadObj = JSONObject().apply {
                        put("attemptId", m.id)
                        put("ownerId", ownerId)
                        put("attemptType", "MOCK_TEST")
                        put("testId", m.testId)
                        put("testTitle", m.testTitle)
                        put("testDescription", m.testDescription)
                        put("durationSeconds", m.durationSeconds)

                        // Older restored rows used fabricated timing. Do not upload it as fact.
                        val legacyRestored = m.finishReason == "Cloud Restore"

                        if (!legacyRestored) {
                            if (m.elapsedSeconds >= 0) {
                                put("elapsedSeconds", m.elapsedSeconds)
                            }

                            if (m.startTimeMillis > 0L) {
                                put("startTimeMillis", m.startTimeMillis)
                            }

                            if (m.createdAt > 0L) {
                                put("createdAt", m.createdAt)
                            }

                            m.finishReason?.let {
                                put("finishReason", it)
                            }
                        }

                        put("completionTime", m.completedAt ?: m.createdAt)
                        put("requestedCount", totalQs)
                        put("presentedCount", totalQs)
                        put("answeredCount", answeredCount)
                        put("correctCount", earnedMarks / marksPerCorrect)
                        put("wrongCount", if (wrong >= 0) wrong else 0)
                        put("skippedCount", if (skipped >= 0) skipped else 0)
                        put("earnedMarks", earnedMarks)
                        put("maxMarks", m.maxMarks)
                        put("scorePercentage", m.scorePercentage)
                        put("accuracyPercentage", m.accuracyPercentage)
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

                    val outboxItem = SyncOutboxEntity(
                        id = "${ownerId}_MOCK_TEST_${m.id}",
                        ownerId = ownerId,
                        recordId = m.id,
                        recordType = "MOCK_TEST",
                        action = "UPSERT",
                        payloadJson = payloadObj.toString(),
                        status = "PENDING",
                        retryCount = 0,
                        createdAt = m.completedAt ?: m.createdAt,
                        lastAttemptedAt = null
                    )
                    outboxDao.insertOutboxItem(outboxItem)
                }
            }
        }
    }

    private fun arePayloadsEquivalent(
        p1: JSONObject,
        p2: JSONObject
    ): Boolean {
        return jsonValuesEquivalent(p1, p2)
    }

    private fun jsonValuesEquivalent(left: Any?, right: Any?): Boolean {
        val leftIsNull = left == null || left === JSONObject.NULL
        val rightIsNull = right == null || right === JSONObject.NULL

        if (leftIsNull || rightIsNull) {
            return leftIsNull && rightIsNull
        }

        if (left is JSONObject && right is JSONObject) {
            val leftKeys = mutableSetOf<String>()
            val rightKeys = mutableSetOf<String>()

            val leftIterator = left.keys()
            while (leftIterator.hasNext()) {
                leftKeys.add(leftIterator.next())
            }

            val rightIterator = right.keys()
            while (rightIterator.hasNext()) {
                rightKeys.add(rightIterator.next())
            }

            if (leftKeys != rightKeys) return false

            return leftKeys.all { key ->
                jsonValuesEquivalent(left.get(key), right.get(key))
            }
        }

        if (left is JSONArray && right is JSONArray) {
            if (left.length() != right.length()) return false

            for (index in 0 until left.length()) {
                if (!jsonValuesEquivalent(left.get(index), right.get(index))) {
                    return false
                }
            }

            return true
        }

        if (left is Number && right is Number) {
            return try {
                java.math.BigDecimal(left.toString()).compareTo(
                    java.math.BigDecimal(right.toString())
                ) == 0
            } catch (_: NumberFormatException) {
                false
            }
        }

        return when {
            left is String && right is String -> left == right
            left is Boolean && right is Boolean -> left == right
            else -> false
        }
    }

    override suspend fun enqueueCompletedAttemptForSync(
        ownerId: String,
        attemptId: String,
        attemptType: String,
        payloadJson: String
    ) = withContext(Dispatchers.IO) {
        if (ownerId == "guest" || ownerId.isEmpty()) return@withContext

        val now = System.currentTimeMillis()
        val outboxItem = SyncOutboxEntity(
            id = "${ownerId}_${attemptType}_$attemptId",
            ownerId = ownerId,
            recordId = attemptId,
            recordType = attemptType,
            action = "UPSERT",
            payloadJson = payloadJson,
            status = "PENDING",
            retryCount = 0,
            createdAt = now,
            lastAttemptedAt = null
        )
        outboxDao.insertOutboxItem(outboxItem)

        if (_isBackupEnabled.value && authRepository.authState.value is AuthState.SignedInVerified) {
            triggerSyncNow()
        }
    }

    override suspend fun enqueueBookmarkForSync(
        ownerId: String,
        questionId: String,
        action: String,
        payloadJson: String
    ) = withContext(Dispatchers.IO) {
        if (ownerId.isBlank() || ownerId == "guest") {
            return@withContext
        }

        require(questionId.isNotBlank()) {
            "Bookmark question ID must not be blank."
        }

        require(action == "UPSERT" || action == "DELETE") {
            "Unsupported bookmark action: $action"
        }

        // Reject malformed JSON before changing the queue.
        JSONObject(payloadJson)

        val canonicalId = "${ownerId}_BOOKMARK_$questionId"

        database.withTransaction {
            // Include legacy IDs when selecting the next revision.
            val existingItems = outboxDao
                .getAllOutboxItemsForOwner(ownerId)
                .filter {
                    it.recordType == "BOOKMARK" &&
                        it.recordId == questionId
                }

            val highestRevision = existingItems.maxOfOrNull {
                it.revision
            } ?: 0

            check(highestRevision < Int.MAX_VALUE) {
                "Bookmark revision limit reached."
            }

            val latestCreatedAt = existingItems.maxOfOrNull {
                it.createdAt
            }

            val now = System.currentTimeMillis()

            // Preserve local operation ordering even if the clock moves backward.
            val operationTime = if (
                latestCreatedAt != null && latestCreatedAt >= now
            ) {
                check(latestCreatedAt < Long.MAX_VALUE) {
                    "Bookmark timestamp limit reached."
                }
                latestCreatedAt + 1L
            } else {
                now
            }

            outboxDao.insertOutboxItem(
                SyncOutboxEntity(
                    id = canonicalId,
                    ownerId = ownerId,
                    recordId = questionId,
                    recordType = "BOOKMARK",
                    action = action,
                    payloadJson = payloadJson,
                    status = "PENDING",
                    retryCount = 0,
                    createdAt = operationTime,
                    lastAttemptedAt = null,
                    workerId = null,
                    claimedAt = null,
                    revision = highestRevision + 1
                )
            )
        }

        val auth = authRepository.authState.value

        if (
            _isBackupEnabled.value &&
            auth is AuthState.SignedInVerified &&
            auth.uid == ownerId
        ) {
            triggerSyncNow()
        }
    }

    override suspend fun clearOutboxForOwner(ownerId: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            outboxDao.deleteOutboxForOwner(ownerId)
            syncedAttemptDao.clearSyncedAttemptsForOwner(ownerId)
        }
        Unit
    }

    private fun jsonToMap(jsonObj: JSONObject): HashMap<String, Any> {
        val map = hashMapOf<String, Any>()
        val keys = jsonObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            var value = jsonObj.get(key)
            if (value is JSONArray) {
                value = jsonToList(value)
            } else if (value is JSONObject) {
                value = jsonToMap(value)
            }
            map[key] = value
        }
        return map
    }

    private fun jsonToList(jsonArray: JSONArray): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until jsonArray.length()) {
            var value = jsonArray.get(i)
            if (value is JSONArray) {
                value = jsonToList(value)
            } else if (value is JSONObject) {
                value = jsonToMap(value)
            }
            list.add(value)
        }
        return list
    }
}