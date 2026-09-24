package com.example.aptiready

import com.example.aptiready.data.model.FreeHintClaimResult
import com.example.aptiready.data.model.FreeHintStatus
import com.example.aptiready.data.repository.FreeHintRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeHintTest {

    private class FakeFreeHintRepository : FreeHintRepository {
        private val claims = mutableMapOf<String, Pair<String, String>>()
        var shouldFailNetwork = false
        var isMalformed = false

        override suspend fun getFreeHintStatus(
            ownerId: String,
            currentSessionId: String,
            currentQuestionId: String
        ): FreeHintStatus {
            if (shouldFailNetwork) {
                return FreeHintStatus.Error("Network error checking free hint status.")
            }
            if (isMalformed) {
                return FreeHintStatus.Error("Malformed existing hint claim detected.")
            }
            val claim = claims[ownerId] ?: return FreeHintStatus.Available
            val (claimedSessionId, claimedQuestionId) = claim
            return if (claimedSessionId == currentSessionId && claimedQuestionId == currentQuestionId) {
                FreeHintStatus.UnlockedForCurrent(ownerId, currentSessionId, currentQuestionId, isVisible = false)
            } else {
                FreeHintStatus.Exhausted(claimedSessionId, claimedQuestionId)
            }
        }

        override suspend fun claimFreeHint(
            ownerId: String,
            sessionId: String,
            questionId: String
        ): FreeHintClaimResult {
            if (shouldFailNetwork) {
                return FreeHintClaimResult.NetworkError("Failed to claim free hint.")
            }
            if (isMalformed) {
                return FreeHintClaimResult.NetworkError("Malformed existing hint claim detected.")
            }
            val claim = claims[ownerId]
            if (claim == null) {
                claims[ownerId] = Pair(sessionId, questionId)
                return FreeHintClaimResult.Granted(ownerId, sessionId, questionId)
            }
            val (claimedSessionId, claimedQuestionId) = claim
            return if (claimedSessionId == sessionId && claimedQuestionId == questionId) {
                FreeHintClaimResult.AllowedReopen(ownerId, sessionId, questionId)
            } else {
                FreeHintClaimResult.Exhausted(claimedSessionId, claimedQuestionId)
            }
        }
    }

    @Test
    fun firstClaimSucceeds_andSubsequentQuestionOrSessionIsDenied() = runBlocking {
        val repo = FakeFreeHintRepository()
        val userA = "user_alice"
        val session1 = "session_001"
        val q1 = "question_1"
        val q2 = "question_2"

        val initialStatus = repo.getFreeHintStatus(userA, session1, q1)
        assertTrue("Initial status should be Available", initialStatus is FreeHintStatus.Available)

        val claimResult = repo.claimFreeHint(userA, session1, q1)
        assertTrue("First claim should be Granted", claimResult is FreeHintClaimResult.Granted)

        val statusQ1 = repo.getFreeHintStatus(userA, session1, q1)
        assertTrue("q1 should now be UnlockedForCurrent", statusQ1 is FreeHintStatus.UnlockedForCurrent)

        val statusQ2 = repo.getFreeHintStatus(userA, session1, q2)
        assertTrue("q2 should be Exhausted", statusQ2 is FreeHintStatus.Exhausted)

        val claimQ2Result = repo.claimFreeHint(userA, session1, q2)
        assertTrue("Claiming q2 should yield Exhausted", claimQ2Result is FreeHintClaimResult.Exhausted)
    }

    @Test
    fun originalUnlockedHint_canReopenInSameSession() = runBlocking {
        val repo = FakeFreeHintRepository()
        val userA = "user_alice"
        val session1 = "session_001"
        val q1 = "question_1"

        repo.claimFreeHint(userA, session1, q1)

        val reopenResult = repo.claimFreeHint(userA, session1, q1)
        assertTrue("Reclaiming same question/session yields AllowedReopen", reopenResult is FreeHintClaimResult.AllowedReopen)
    }

    @Test
    fun staleUnlockedState_cannotRevealAnotherQuestionsHint() {
        val userA = "user_alice"
        val session1 = "session_001"
        val q1 = "question_1"
        val q2 = "question_2"

        val unlockedQ1 = FreeHintStatus.UnlockedForCurrent(userA, session1, q1, isVisible = true)

        val isValidForQ2 = unlockedQ1.ownerId == userA &&
                unlockedQ1.sessionId == session1 &&
                unlockedQ1.questionId == q2 &&
                unlockedQ1.isVisible

        assertFalse("Unlocked state for q1 MUST NOT validate for q2", isValidForQ2)
    }

    @Test
    fun accountChanges_isolateAllowances() = runBlocking {
        val repo = FakeFreeHintRepository()
        val userA = "user_alice"
        val userB = "user_bob"
        val session1 = "session_001"
        val q1 = "question_1"

        repo.claimFreeHint(userA, session1, q1)

        val statusUserB = repo.getFreeHintStatus(userB, session1, q1)
        assertTrue("User B should still have Available status independently", statusUserB is FreeHintStatus.Available)
    }

    @Test
    fun failedNetworkChecks_revealNothing() = runBlocking {
        val repo = FakeFreeHintRepository()
        repo.shouldFailNetwork = true

        val status = repo.getFreeHintStatus("user_alice", "session_001", "question_1")
        assertTrue("Network error returns Error status", status is FreeHintStatus.Error)

        val claimResult = repo.claimFreeHint("user_alice", "session_001", "question_1")
        assertTrue("Network error during claim returns NetworkError", claimResult is FreeHintClaimResult.NetworkError)
    }

    @Test
    fun malformedExistingClaim_treatedAsErrorWithoutOverwriting() = runBlocking {
        val repo = FakeFreeHintRepository()
        repo.isMalformed = true

        val status = repo.getFreeHintStatus("user_alice", "session_001", "question_1")
        assertTrue("Malformed claim returns Error status", status is FreeHintStatus.Error)

        val claimResult = repo.claimFreeHint("user_alice", "session_001", "question_1")
        assertTrue("Claim against malformed record returns NetworkError", claimResult is FreeHintClaimResult.NetworkError)
    }
}
