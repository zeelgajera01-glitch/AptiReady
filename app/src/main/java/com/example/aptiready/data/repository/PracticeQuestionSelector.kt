package com.example.aptiready.data.repository

import com.example.aptiready.data.model.QuestionText
import kotlin.random.Random

/** Selects each prompt once even when the bank contains multiple IDs or option orders for it. */
object PracticeQuestionSelector {
    fun <T> uniqueQuestions(questions: List<T>, textOf: (T) -> String): List<T> =
        questions.filter { QuestionText.identity(textOf(it)).isNotEmpty() }
            .distinctBy { QuestionText.identity(textOf(it)) }

    fun <T> unseenCount(
        questions: List<T>,
        seenIds: Set<String>,
        reservedIds: Set<String>,
        idOf: (T) -> String,
        textOf: (T) -> String
    ): Int = groups(questions, idOf, textOf).values.count { aliases ->
        aliases.none { idOf(it) in seenIds || idOf(it) in reservedIds }
    }

    fun <T> select(
        questions: List<T>,
        seenIds: Set<String>,
        reservedIds: Set<String>,
        requestedCount: Int,
        idOf: (T) -> String,
        textOf: (T) -> String,
        random: Random = Random.Default
    ): List<T> {
        if (requestedCount <= 0) return emptyList()
        val uniqueGroups = groups(questions, idOf, textOf).values.shuffled(random)
        val (unseen, previouslyUsed) = uniqueGroups.partition { aliases ->
            aliases.none { idOf(it) in seenIds || idOf(it) in reservedIds }
        }
        // If the unique pool is exhausted, use old prompts once; never pad with copies.
        return (unseen + previouslyUsed).take(requestedCount).map { aliases ->
            aliases[random.nextInt(aliases.size)]
        }
    }

    private fun <T> groups(
        questions: List<T>,
        idOf: (T) -> String,
        textOf: (T) -> String
    ): Map<String, List<T>> = questions.distinctBy(idOf)
        .filter { QuestionText.identity(textOf(it)).isNotEmpty() }
        .groupBy { QuestionText.identity(textOf(it)) }
}
