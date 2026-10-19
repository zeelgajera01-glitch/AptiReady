package com.aptirise.app

import androidx.lifecycle.ViewModelStore
import com.aptirise.app.data.model.MockQuestionSnapshot
import com.aptirise.app.data.model.MockTestAttempt
import com.aptirise.app.data.model.QuestionOption
import com.aptirise.app.data.repository.LearningOwner
import com.aptirise.app.data.repository.MockTestRepository
import com.aptirise.app.data.repository.PracticeRepository
import com.aptirise.app.ui.practice.PracticeReviewViewModel
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression: a completed mock ID must not be treated as a practice-session ID. */
@OptIn(ExperimentalCoroutinesApi::class)
class AtelierReviewViewModelTest {
    private val options = listOf(QuestionOption("a", "Correct"), QuestionOption("b", "Other"))

    private fun attempt(owner: String) = MockTestAttempt(
        id = "mock-1", ownerId = owner, testId = "test-1", testTitle = "Full Aptitude Mock",
        testDescription = "Timed test", testVersion = 1, durationSeconds = 900,
        marksPerCorrect = 1, status = "COMPLETED", currentQuestionIndex = 2,
        elapsedSeconds = 300, startTimeMillis = 1L, bootTimeMarker = 1L,
        lastSavedTimeMillis = 1L, remainingSeconds = 600, earnedMarks = 1,
        maxMarks = 3, scorePercentage = 33.3f, accuracyPercentage = 50f,
        finishReason = "MANUAL_SUBMISSION", createdAt = 1L, completedAt = 2L
    )

    private fun snapshot(position: Int, selected: String?) = MockQuestionSnapshot(
        id = "snapshot-$position", attemptId = "mock-1", questionId = "question-$position",
        position = position, questionText = "Question $position", options = options,
        correctOptionId = "a", explanation = "Worked solution", hint = "Hint",
        selectedOptionId = selected, isMarkedForReview = true, isVisited = true,
        isBookmarked = false
    )

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> fake(noinline call: (String) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            call(method.name)
        } as T

    @Test
    fun completedMockLoadsItsAnswersAndFiltersCorrectIncorrectSkipped() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val practice = fake<PracticeRepository> { method ->
                when (method) {
                    "getSessionById" -> null
                    "getBookmarksForOwner" -> flowOf(emptyList<com.aptirise.app.data.model.BookmarkItem>())
                    else -> error("Unexpected practice call: $method")
                }
            }
            val mock = fake<MockTestRepository> { method ->
                when (method) {
                    "getAttemptById" -> attempt("user-1")
                    "getAttemptSnapshotsFlow" -> flowOf(listOf(snapshot(0, "a"), snapshot(1, "b"), snapshot(2, null)))
                    else -> error("Unexpected mock call: $method")
                }
            }
            val auth = fake<LearningOwner> { method ->
                check(method == "getOwnerId")
                "user-1"
            }
            val vm = PracticeReviewViewModel("mock-1", practice, auth, mock)
            store.put("review", vm)
            runCurrent()
            assertEquals("Full Aptitude Mock", vm.reviewTitle.value)
            assertEquals(3, vm.filteredSnapshots.value.size)
            vm.filterSnapshots("correct")
            assertEquals(listOf("question-0"), vm.filteredSnapshots.value.map { it.questionId })
            vm.filterSnapshots("incorrect")
            assertEquals(listOf("question-1"), vm.filteredSnapshots.value.map { it.questionId })
            vm.filterSnapshots("skipped")
            assertEquals(listOf("question-2"), vm.filteredSnapshots.value.map { it.questionId })
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun anotherOwnersCompletedMockDoesNotExposeReviewAnswers() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val practice = fake<PracticeRepository> { method ->
                check(method == "getSessionById")
                null
            }
            val mock = fake<MockTestRepository> { method ->
                check(method == "getAttemptById")
                attempt("another-user")
            }
            val auth = fake<LearningOwner> { method ->
                check(method == "getOwnerId")
                "user-1"
            }
            val vm = PracticeReviewViewModel("mock-1", practice, auth, mock)
            store.put("review", vm)
            runCurrent()
            assertTrue(vm.filteredSnapshots.value.isEmpty())
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
