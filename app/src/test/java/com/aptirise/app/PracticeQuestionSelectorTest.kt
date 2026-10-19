package com.aptirise.app

import com.aptirise.app.data.model.QuestionText
import com.aptirise.app.data.repository.PracticeQuestionSelector
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeQuestionSelectorTest {
    private data class Candidate(val id: String, val prompt: String)

    private fun choose(pool: List<Candidate>, count: Int, seen: Set<String> = emptySet(), reserved: Set<String> = emptySet()): List<Candidate> =
        PracticeQuestionSelector.select(pool, seen, reserved, count, { it.id }, { it.prompt }, Random(19))

    @Test
    fun repeatedContentWithDifferentIdsAppearsOnlyOnce() {
        val pool = listOf(
            Candidate("old-1", "In percentage calculation #1, what is 10% of 200?"),
            Candidate("old-2", "In percentage calculation #82, what is 10% of 200?"),
            Candidate("new-1", "What is 10% of 300?")
        )
        val selected = choose(pool, 20)
        assertEquals(2, selected.size)
        assertEquals(2, selected.map { QuestionText.identity(it.prompt) }.toSet().size)
    }

    @Test
    fun anAliasOfASeenPromptIsNotTreatedAsUnseen() {
        val pool = listOf(Candidate("a", "Same question"), Candidate("b", " same   QUESTION "), Candidate("c", "Fresh question"))
        assertEquals("c", choose(pool, 1, seen = setOf("a")).single().id)
        assertEquals(1, PracticeQuestionSelector.unseenCount(pool, setOf("a"), emptySet(), { it.id }, { it.prompt }))
    }

    @Test
    fun freshContentIsPreferredToAnActiveReservationAlias() {
        val pool = listOf(Candidate("a", "Reserved question"), Candidate("b", "Reserved question"), Candidate("c", "Unused question"))
        assertEquals("c", choose(pool, 1, reserved = setOf("a")).single().id)
    }

    @Test
    fun exhaustedPoolReturnsAShorterSessionInsteadOfPaddingWithDuplicates() {
        val pool = listOf(Candidate("a", "One question"), Candidate("b", "One question"))
        assertEquals(1, choose(pool, 10, seen = setOf("a")).size)
    }

    @Test
    fun actualNumbersInTheProblemArePreserved() {
        val clean = QuestionText.displayText("In percentage calculation #302, what is 15% of ₹740?")
        assertEquals("What is 15% of ₹740?", clean)
        assertFalse(clean.contains("#302"))
        assertTrue(clean.contains("15%"))
        assertTrue(clean.contains("740"))
        assertFalse(QuestionText.identity("Find 10% of 200") == QuestionText.identity("Find 10% of 300"))
    }

    @Test
    fun duplicateIdsAndBlankPromptsAreIgnored() {
        val pool = listOf(Candidate("a", "Question"), Candidate("a", "Question"), Candidate("blank", "   "))
        assertEquals(1, choose(pool, 10).size)
        assertTrue(choose(pool, 0).isEmpty())
    }
}
