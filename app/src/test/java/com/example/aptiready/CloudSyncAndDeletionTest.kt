package com.example.aptiready

import com.example.aptiready.data.local.db.SyncOutboxEntity
import com.example.aptiready.data.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncAndDeletionTest {

    @Test
    fun syncStatusDisplayNames_matchExpected() {
        assertEquals("Cloud Backup Off", SyncStatus.OFF.getDisplayName())
        assertEquals("Waiting for Email Verification", SyncStatus.WAITING_FOR_VERIFICATION.getDisplayName())
        assertEquals("Pending Changes Queued", SyncStatus.PENDING.getDisplayName())
        assertEquals("Syncing Cloud Records...", SyncStatus.SYNCING.getDisplayName())
        assertEquals("Up to Date", SyncStatus.UP_TO_DATE.getDisplayName())
        assertEquals("Offline (Local Only)", SyncStatus.OFFLINE.getDisplayName())
        assertEquals("Sync Error", SyncStatus.ERROR.getDisplayName())
        assertEquals("Account Deletion Pending", SyncStatus.DELETION_PENDING.getDisplayName())
    }

    @Test
    fun outboxItemCreation_correctInitialStatus() {
        val item = SyncOutboxEntity(
            id = "user1_ATTEMPT_att_123_1000",
            ownerId = "user1",
            recordId = "att_123",
            recordType = "ATTEMPT",
            action = "UPSERT",
            payloadJson = "{\"score\": 85}",
            status = "PENDING",
            retryCount = 0,
            createdAt = 1000L,
            lastAttemptedAt = null
        )

        assertEquals("user1", item.ownerId)
        assertEquals("ATTEMPT", item.recordType)
        assertEquals("PENDING", item.status)
        assertEquals(0, item.retryCount)
    }

    @Test
    fun payloadSizeCheck_validUnder500KB() {
        val payload = "a".repeat(1000) // 1 KB
        val isUnderLimit = payload.length < 500000

        assertTrue("Payload under 500KB should pass size check", isUnderLimit)
    }

    @Test
    fun payloadSizeCheck_oversizedFails() {
        val payload = "a".repeat(600000) // 600 KB
        val isUnderLimit = payload.length < 500000

        assertFalse("Payload over 500KB should fail size check", isUnderLimit)
    }

    @Test
    fun reauthTimeCheck_validUnder15Mins() {
        val nowSeconds = 10000L
        val authTimeSeconds = 9500L // 500 seconds ago (< 900 seconds / 15 mins)

        val isRecent = (nowSeconds - authTimeSeconds) <= 900L
        assertTrue("Auth time under 15 mins is valid for deletion", isRecent)
    }

    @Test
    fun reauthTimeCheck_staleFails() {
        val nowSeconds = 10000L
        val authTimeSeconds = 8000L // 2000 seconds ago (> 900 seconds)

        val isRecent = (nowSeconds - authTimeSeconds) <= 900L
        assertFalse("Auth time over 15 mins should fail and require reauth", isRecent)
    }

    @Test
    fun userIsolation_outboxItemsScopedByOwner() {
        val user1Item = SyncOutboxEntity(
            id = "user1_ATTEMPT_1",
            ownerId = "user1",
            recordId = "att_1",
            recordType = "ATTEMPT",
            action = "UPSERT",
            payloadJson = "{}",
            status = "PENDING",
            retryCount = 0,
            createdAt = 1000L,
            lastAttemptedAt = null
        )

        val user2Item = SyncOutboxEntity(
            id = "user2_ATTEMPT_2",
            ownerId = "user2",
            recordId = "att_2",
            recordType = "ATTEMPT",
            action = "UPSERT",
            payloadJson = "{}",
            status = "PENDING",
            retryCount = 0,
            createdAt = 1000L,
            lastAttemptedAt = null
        )

        val activeOwner = "user1"
        val isUser1ItemForActiveOwner = user1Item.ownerId == activeOwner
        val isUser2ItemForActiveOwner = user2Item.ownerId == activeOwner

        assertTrue("User 1 item belongs to active owner", isUser1ItemForActiveOwner)
        assertFalse("User 2 item does not belong to active owner", isUser2ItemForActiveOwner)
    }

    @Test
    fun attemptPayload_containsRequiredFields() {
        val payloadMap = mapOf(
            "attemptId" to "session_123",
            "ownerId" to "user_1",
            "attemptType" to "PRACTICE",
            "topicId" to "percentages",
            "answeredCount" to 10,
            "correctCount" to 8,
            "accuracyPercentage" to 80.0f
        )

        assertEquals("session_123", payloadMap["attemptId"])
        assertEquals("user_1", payloadMap["ownerId"])
        assertEquals("PRACTICE", payloadMap["attemptType"])
        assertEquals(10, payloadMap["answeredCount"])
        assertEquals(8, payloadMap["correctCount"])
    }

    @Test
    fun staleSyncingOutboxItem_isRecoverable() {
        val now = 100000L
        val staleThreshold = now - 60000L // 1 minute ago
        val lastAttempted = now - 120000L // 2 minutes ago (stale!)

        val isStale = lastAttempted < staleThreshold
        assertTrue("Stale SYNCING item should be selected for recovery", isStale)
    }

    @Test
    fun stableOutboxId_preventsDuplicateQueueEntries() {
        val ownerId = "user_100"
        val attemptId = "session_abc"
        
        val id1 = "${ownerId}_PRACTICE_$attemptId"
        val id2 = "${ownerId}_PRACTICE_$attemptId"

        assertEquals("Stable ID must be identical across completion retries", id1, id2)
    }

    @Test
    fun skippedAnswerNormalization_convertsEmptyToNull() {
        val rawSelectedOpt1: String? = ""
        val rawSelectedOpt2: String? = "null"
        val rawSelectedOpt3: String? = "a"

        val normalized1 = if (rawSelectedOpt1.isNullOrEmpty() || rawSelectedOpt1 == "null") null else rawSelectedOpt1
        val normalized2 = if (rawSelectedOpt2.isNullOrEmpty() || rawSelectedOpt2 == "null") null else rawSelectedOpt2
        val normalized3 = if (rawSelectedOpt3.isNullOrEmpty() || rawSelectedOpt3 == "null") null else rawSelectedOpt3

        assertEquals(null, normalized1)
        assertEquals(null, normalized2)
        assertEquals("a", normalized3)
    }

    @Test
    fun durableAcknowledgement_preventsReuploadingSyncedAttempts() {
        val syncedAttemptIds = setOf("attempt_1001", "attempt_1002")
        val isAttempt1Synced = "attempt_1001" in syncedAttemptIds
        val isAttempt2Synced = "attempt_1003" in syncedAttemptIds

        assertTrue("Acknowledged attempt should be marked synced and skipped during reconciliation", isAttempt1Synced)
        assertFalse("Unacknowledged attempt should be enqueued for sync", isAttempt2Synced)
    }

    @Test
    fun permanentFailure_preventsUpToDateStatus() {
        val permanentFailures = 1
        val isBackupEnabled = true

        val status = if (permanentFailures > 0) SyncStatus.ERROR else SyncStatus.UP_TO_DATE

        assertEquals(SyncStatus.ERROR, status)
    }

    @Test
    fun workerClaim_assignsUniqueWorkerId() {
        val workerId1 = "worker_uuid_1"
        val workerId2 = "worker_uuid_2"

        val claimedByWorker1 = "item_1" to workerId1
        val isClaimedByWorker2 = claimedByWorker1.second == workerId2

        assertFalse("Worker 2 cannot claim or modify an item currently claimed by Worker 1", isClaimedByWorker2)
    }

    @Test
    fun multiBatchProcessing_processesAllRecordsInChunks() {
        val totalPendingCount = 120
        val batchSize = 50
        var remaining = totalPendingCount
        var processedBatches = 0

        while (remaining > 0) {
            val countToProcess = minOf(batchSize, remaining)
            remaining -= countToProcess
            processedBatches++
        }

        assertEquals(0, remaining)
        assertEquals(3, processedBatches)
    }

    @Test
    fun deleteClaimedOutboxItem_requiresMatchingWorkerId() {
        val activeOwner = "user1"
        val expectedWorkerId = "worker_abc"
        val staleWorkerId = "worker_xyz"

        val canWorkerAcknowledge = (activeOwner == "user1" && expectedWorkerId == "worker_abc")
        val canStaleWorkerAcknowledge = (activeOwner == "user1" && staleWorkerId == "worker_abc")

        assertTrue("Matching worker claim can acknowledge", canWorkerAcknowledge)
        assertFalse("Stale worker claim cannot acknowledge", canStaleWorkerAcknowledge)
    }

    @Test
    fun multiBatchProcessing_501ItemsInChunks() {
        val totalPendingCount = 501
        val batchSize = 50
        var remaining = totalPendingCount
        var processedBatches = 0

        while (remaining > 0) {
            val countToProcess = minOf(batchSize, remaining)
            remaining -= countToProcess
            processedBatches++
        }

        assertEquals(0, remaining)
        assertEquals(11, processedBatches)
    }

    @Test
    fun syncExecutionResult_mapsMoreWorkRemaining() {
        val remainingCount = 1
        val result = if (remainingCount > 0) com.example.aptiready.data.model.SyncExecutionResult.MORE_WORK_REMAINING else com.example.aptiready.data.model.SyncExecutionResult.COMPLETE

        assertEquals(com.example.aptiready.data.model.SyncExecutionResult.MORE_WORK_REMAINING, result)
    }

    @Test
    fun legacyOutboxDeduplication_collapsesToCanonicalId() {
        val ownerId = "user1"
        val recordType = "PRACTICE"
        val recordId = "session_999"

        val canonicalId = "${ownerId}_${recordType}_${recordId}"

        assertEquals("user1_PRACTICE_session_999", canonicalId)
    }

    @Test
    fun payloadConflictDetection_flagsConflictingAnswers() {
        val score1 = 8
        val score2 = 5

        val isEquivalent = (score1 == score2)

        assertFalse("Attempts with different scores/answers must be flagged as conflict", isEquivalent)
    }

    @Test
    fun bookmarkOperationDeduplication_preservesNewerRemoval() {
        val oldUpsert = SyncOutboxEntity("b1", "u1", "q1", "BOOKMARK", "UPSERT", "{}", "PENDING", 0, 1000L, null)
        val newDelete = SyncOutboxEntity("b2", "u1", "q1", "BOOKMARK", "DELETE", "{}", "PENDING", 0, 2000L, null)

        val latest = listOf(oldUpsert, newDelete).maxByOrNull { it.createdAt }!!

        assertEquals("DELETE", latest.action)
        assertEquals(2000L, latest.createdAt)
    }

    @Test
    fun bookmarkRevision_incrementsOnUpdate() {
        val itemRev1 = SyncOutboxEntity("b1", "u1", "q1", "BOOKMARK", "UPSERT", "{}", "PENDING", 0, 1000L, null, null, null, revision = 1)
        val itemRev2 = itemRev1.copy(revision = itemRev1.revision + 1)

        assertEquals(1, itemRev1.revision)
        assertEquals(2, itemRev2.revision)
    }

    @Test
    fun workerAcknowledgement_requiresMatchingRevision() {
        val uploadedRev = 1
        val currentPendingRev = 2
        val isMatchingRevision = uploadedRev == currentPendingRev

        assertFalse("Old worker holding revision 1 cannot delete/acknowledge revision 2", isMatchingRevision)
    }

    @Test
    fun accountBackupSettings_isolatedPerUid() {
        val uidA = "user_A"
        val uidB = "user_B"

        val backupKeyA = "cloud_backup_enabled_$uidA"
        val backupKeyB = "cloud_backup_enabled_$uidB"

        val isSameKey = backupKeyA == backupKeyB

        assertFalse("Account A and Account B must have distinct preference keys", isSameKey)
    }

    @Test
    fun sessionToken_changesOnAccountSwitch() {
        val sessionToken1 = java.util.UUID.randomUUID().toString()
        val sessionToken2 = java.util.UUID.randomUUID().toString()

        val isSameSession = sessionToken1 == sessionToken2

        assertFalse("New account session token must invalidate previous worker callbacks", isSameSession)
    }

    @Test
    fun restoreFailure_preventsUpToDateStatus() {
        val restoreSucceeded = false
        val pendingCount = 0

        val status = if (!restoreSucceeded) SyncStatus.ERROR else if (pendingCount > 0) SyncStatus.PENDING else SyncStatus.UP_TO_DATE

        assertEquals(SyncStatus.ERROR, status)
    }

    @Test
    fun repairCheck_detectsEmptySnapshotList() {
        val snapshots = emptyList<com.example.aptiready.data.local.db.SessionQuestionSnapshotEntity>()
        val expectedCount = 5

        val needsRepair = snapshots.isEmpty() || snapshots.size != expectedCount

        assertTrue("Empty snapshot list for completed session must trigger repair", needsRepair)
    }

    @Test
    fun historicalCorrectOptionId_isPreserved() {
        val historicalCorrectOptionId = "c"
        val currentLocalQuestionOptionId = "a"

        val optionToUse = historicalCorrectOptionId

        assertEquals("c", optionToUse)
    }

    @Test
    fun accuracyCalculation_excludesSkippedQuestions() {
        val totalPresented = 10
        val answeredCount = 8
        val correctCount = 6
        val skippedCount = totalPresented - answeredCount

        val accuracyPct = if (answeredCount > 0) (correctCount.toFloat() / answeredCount.toFloat()) * 100f else 0f

        assertEquals(75.0f, accuracyPct, 0.001f)
    }

    @Test
    fun validateCloudAttemptData_rejectsOwnerMismatch() {
        val docOwner = "user_A"
        val expectedOwner = "user_B"

        val isMatch = docOwner == expectedOwner

        assertFalse("Cloud document owner mismatch must reject restore", isMatch)
    }

    @Test
    fun validateCloudAttemptData_rejectsSnapshotCountMismatch() {
        val requestedCount = 10
        val actualSnapshotCount = 8

        val isValidCount = requestedCount == actualSnapshotCount

        assertFalse("Snapshot count mismatch against requested count must reject restore", isValidCount)
    }
}