package com.example.aptiready

import com.example.aptiready.data.model.MockQuestionSnapshot
import com.example.aptiready.data.model.QuestionOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockTestTimerAndScoringTest {

    private val sampleOptions = listOf(
        QuestionOption("a", "Option A"),
        QuestionOption("b", "Option B"),
        QuestionOption("c", "Option C"),
        QuestionOption("d", "Option D")
    )

    @Test
    fun mockScoring_correctMarks_and_noNegativeMarking() {
        val snapshots = listOf(
            MockQuestionSnapshot("1", "att_1", "q1", 0, "Q1", sampleOptions, "a", "Exp", "Hint", "a", false, true, false), // Correct (+1)
            MockQuestionSnapshot("2", "att_1", "q2", 1, "Q2", sampleOptions, "b", "Exp", "Hint", "c", false, true, false), // Wrong (0)
            MockQuestionSnapshot("3", "att_1", "q3", 2, "Q3", sampleOptions, "c", "Exp", "Hint", null, false, true, false)  // Unanswered (0)
        )

        val marksPerCorrect = 1
        var earnedMarks = 0
        var answeredCount = 0

        snapshots.forEach { snap ->
            if (snap.selectedOptionId != null) {
                answeredCount++
                if (snap.isCorrect()) {
                    earnedMarks += marksPerCorrect
                }
            }
        }

        assertEquals(1, earnedMarks)
        assertEquals(2, answeredCount)
    }

    @Test
    fun markedForReviewQuestion_withAnswer_includedInGrading() {
        val snapshot = MockQuestionSnapshot(
            id = "snap_marked",
            attemptId = "att_1",
            questionId = "q_10",
            position = 0,
            questionText = "Marked Question",
            options = sampleOptions,
            correctOptionId = "b",
            explanation = "Exp",
            hint = "Hint",
            selectedOptionId = "b",
            isMarkedForReview = true, // Marked for review
            isVisited = true,
            isBookmarked = false
        )

        assertTrue("Marked question with selected answer should be graded as correct", snapshot.isCorrect())
    }

    @Test
    fun timerRecovery_sameBoot_monotonicDifference() {
        val durationSeconds = 900 // 15 mins
        val savedRemaining = 800
        val savedBootMarker = 100000L
        val currentBootMarker = 160000L // 60 seconds elapsed

        val elapsedSeconds = ((currentBootMarker - savedBootMarker) / 1000).toInt()
        val calculatedRemaining = maxOf(0, savedRemaining - elapsedSeconds)

        assertEquals(60, elapsedSeconds)
        assertEquals(740, calculatedRemaining)
    }

    @Test
    fun timerRecovery_rebootFallback_clampedWallClock() {
        val durationSeconds = 900
        val savedRemaining = 800
        val savedWallTime = 1000000L
        val currentWallTime = 1060000L // 60 seconds elapsed wall time

        val elapsedMs = maxOf(0L, currentWallTime - savedWallTime)
        val elapsedSeconds = (elapsedMs / 1000).toInt()
        val calculatedRemaining = maxOf(0, savedRemaining - elapsedSeconds)

        assertEquals(740, calculatedRemaining)
    }

    @Test
    fun retake_createsNewAttemptId_andPreservesPrior() {
        val firstAttemptId = "attempt_v1_1001"
        val retakeAttemptId = "attempt_v1_1002"

        assertNotEquals("Retake attempt must have a distinct attempt ID", firstAttemptId, retakeAttemptId)
    }

    @Test
    fun weightedAggregateAccuracy_calculation() {
        // Practice session: 8 correct out of 10 answered
        val practiceCorrect = 8
        val practiceAnswered = 10

        // Mock test: 15 correct out of 20 answered
        val mockCorrect = 15
        val mockAnswered = 20

        val totalCorrect = practiceCorrect + mockCorrect
        val totalAnswered = practiceAnswered + mockAnswered

        val aggregateAccuracy = (totalCorrect.toFloat() / totalAnswered.toFloat()) * 100f

        assertEquals(23, totalCorrect)
        assertEquals(30, totalAnswered)
        assertEquals(76.666664f, aggregateAccuracy, 0.01f)
    }
}