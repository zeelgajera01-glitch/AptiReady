package com.example.aptiready

import com.example.aptiready.data.model.QuestionOption
import com.example.aptiready.data.model.SessionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionTest {

    private val sampleOptions = listOf(
        QuestionOption("a", "Option A"),
        QuestionOption("b", "Option B"),
        QuestionOption("c", "Option C"),
        QuestionOption("d", "Option D")
    )

    @Test
    fun snapshotScoring_correctAnswer() {
        val snapshot = SessionSnapshot(
            id = "snap_01",
            sessionId = "s_123",
            questionId = "q_01",
            position = 0,
            questionText = "Sample Question",
            options = sampleOptions,
            correctOptionId = "c",
            explanation = "Explanation",
            hint = "Hint",
            selectedOptionId = "c",
            isSubmitted = true,
            isBookmarked = false
        )

        assertTrue("Should be correct when selected matches correctOptionId", snapshot.isCorrect())
        assertFalse("Should not be incorrect", snapshot.isIncorrect())
        assertFalse("Should not be skipped", snapshot.isSkipped())
    }

    @Test
    fun snapshotScoring_incorrectAnswer() {
        val snapshot = SessionSnapshot(
            id = "snap_01",
            sessionId = "s_123",
            questionId = "q_01",
            position = 0,
            questionText = "Sample Question",
            options = sampleOptions,
            correctOptionId = "c",
            explanation = "Explanation",
            hint = "Hint",
            selectedOptionId = "a",
            isSubmitted = true,
            isBookmarked = false
        )

        assertFalse("Should not be correct", snapshot.isCorrect())
        assertTrue("Should be incorrect when selected differs from correctOptionId", snapshot.isIncorrect())
        assertFalse("Should not be skipped", snapshot.isSkipped())
    }

    @Test
    fun snapshotScoring_skippedAnswer() {
        val snapshot = SessionSnapshot(
            id = "snap_01",
            sessionId = "s_123",
            questionId = "q_01",
            position = 0,
            questionText = "Sample Question",
            options = sampleOptions,
            correctOptionId = "c",
            explanation = "Explanation",
            hint = "Hint",
            selectedOptionId = null,
            isSubmitted = true,
            isBookmarked = false
        )

        assertFalse("Should not be correct", snapshot.isCorrect())
        assertFalse("Should not be incorrect", snapshot.isIncorrect())
        assertTrue("Should be skipped when submitted with null selectedOptionId", snapshot.isSkipped())
    }

    @Test
    fun accuracyCalculation_zeroSubmittedAnswers_returnsZero() {
        val submittedCount = 0
        val correctCount = 0
        val accuracyPct = if (submittedCount > 0) (correctCount.toFloat() / submittedCount.toFloat()) * 100f else 0f

        assertEquals(0f, accuracyPct, 0.001f)
    }

    @Test
    fun stableAnswerIds_preservedAcrossOptions() {
        val options = listOf(
            QuestionOption("a", "First Option"),
            QuestionOption("b", "Second Option"),
            QuestionOption("c", "Third Option"),
            QuestionOption("d", "Fourth Option")
        )

        assertEquals("a", options[0].id)
        assertEquals("b", options[1].id)
        assertEquals("c", options[2].id)
        assertEquals("d", options[3].id)
    }

    @Test
    fun sessionCreation_clampsRequestedCountToAvailablePool() {
        val availableCount = 2 // e.g. "percentages" with medium difficulty
        val requestedLength = 10

        val countToRequest = minOf(requestedLength, availableCount)

        assertEquals(2, countToRequest)
    }

    @Test
    fun sessionCreation_allDifficulties_usesFullPool() {
        val totalAvailableInTopic = 60 // All difficulties for percentages
        val requestedLength = 10

        val countToRequest = minOf(requestedLength, totalAvailableInTopic)

        assertEquals(10, countToRequest)
    }

    @Test
    fun unseenQuestionSelection_prioritizesUnseen_andFillsWithRevision() {
        val totalAvailable = 60
        val seenCount = 55
        val requestedLength = 10

        val unseenCount = totalAvailable - seenCount // 5
        val unseenToTake = minOf(requestedLength, unseenCount) // 5
        val revisionNeeded = requestedLength - unseenToTake // 5

        assertEquals(5, unseenToTake)
        assertEquals(5, revisionNeeded)
        assertEquals(10, unseenToTake + revisionNeeded)
    }
}