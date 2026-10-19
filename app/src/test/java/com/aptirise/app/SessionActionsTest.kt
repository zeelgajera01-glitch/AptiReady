package com.aptirise.app

import com.aptirise.app.ui.common.SessionActions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionActionsTest {
    @Test fun finishingWaitsForAnswerAndRejectsDuplicateFinish() = runTest {
        val events = mutableListOf<String>()
        val actions = SessionActions(backgroundScope)
        actions.submit { delay(10); events.add("answer") }
        actions.submit(terminal = true) { events.add("finish") }
        actions.submit(terminal = true) { events.add("duplicate") }
        actions.submit { events.add("late answer") }
        runCurrent()
        testScheduler.advanceTimeBy(10)
        runCurrent()
        assertEquals(listOf("answer", "finish"), events)
        assertFalse(actions.busy.value)
    }

    @Test fun repeatedBookmarkIsCoalescedWhileWriteIsPending() = runTest {
        val actions = SessionActions(backgroundScope)
        var changes = 0
        repeat(10) { actions.submit(key = "bookmark:0") { delay(5); changes++ } }
        runCurrent()
        testScheduler.advanceTimeBy(5)
        runCurrent()
        assertEquals(1, changes)
        actions.submit(key = "bookmark:0") { changes++ }
        runCurrent()
        assertEquals(2, changes)
    }

    @Test fun failedFinishCanBeRetriedAndDoesNotKillWorker() = runTest {
        val actions = SessionActions(backgroundScope)
        actions.submit(terminal = true) { throw IllegalStateException("disk full") }
        runCurrent()
        assertNotNull(actions.error.value)
        assertFalse(actions.busy.value)
        var completed = false
        actions.submit(terminal = true) { completed = true }
        runCurrent()
        assertTrue(completed)
        assertNull(actions.error.value)
    }
}
