package com.example.aptiready.data.repository

import com.example.aptiready.data.local.db.AppDatabase
import com.example.aptiready.data.model.Category
import com.example.aptiready.data.model.Difficulty
import com.example.aptiready.data.model.TestSummary
import com.example.aptiready.data.model.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class RoomQuestionRepositoryImpl(
    private val database: AppDatabase
) : QuestionRepository {

    private val contentDao = database.contentDao()
    private val mockTestDao = database.mockTestDao()

    override fun getCategories(): Flow<List<Category>> {
        return contentDao.getCategories().map { list ->
            list.map { entity ->
                Category(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    iconRes = entity.iconRes,
                    topicCount = if (entity.id == "quant") 5 else if (entity.id == "logical") 3 else 2
                )
            }
        }
    }

    override fun getTopics(categoryId: String?, searchQuery: String?): Flow<List<Topic>> {
        return contentDao.getTopics().map { list ->
            val query = searchQuery?.trim()
            list.filter { entity ->
                val matchesCategory = categoryId.isNullOrEmpty() || categoryId == "all" || entity.categoryId.equals(categoryId, ignoreCase = true)
                val matchesSearch = query.isNullOrEmpty() ||
                        entity.title.contains(query, ignoreCase = true) ||
                        entity.description.contains(query, ignoreCase = true) ||
                        entity.categoryName.contains(query, ignoreCase = true)
                matchesCategory && matchesSearch
            }.map { entity ->
                Topic(
                    id = entity.id,
                    categoryId = entity.categoryId,
                    categoryName = entity.categoryName,
                    title = entity.title,
                    description = entity.description,
                    difficulty = parseDifficulty(entity.difficulty),
                    sampleQuestionCount = 60,
                    formulaPreview = entity.formulaPreview
                )
            }
        }
    }

    override fun getTopicById(topicId: String): Flow<Topic?> {
        return contentDao.getTopics().map { list ->
            val match = list.find { it.id == topicId } ?: list.firstOrNull()
            match?.let { entity ->
                Topic(
                    id = entity.id,
                    categoryId = entity.categoryId,
                    categoryName = entity.categoryName,
                    title = entity.title,
                    description = entity.description,
                    difficulty = parseDifficulty(entity.difficulty),
                    sampleQuestionCount = 60,
                    formulaPreview = entity.formulaPreview
                )
            }
        }
    }

    override fun getTestCatalog(): Flow<List<TestSummary>> {
        return mockTestDao.getMockTestsFlow().map { list ->
            list.map { entity ->
                val qIds = parseJsonArray(entity.questionIdsJson)
                TestSummary(
                    id = entity.id,
                    title = entity.title,
                    categoryName = "Timed Assessment",
                    description = entity.description,
                    durationMinutes = entity.durationSeconds / 60,
                    questionCount = qIds.size,
                    difficulty = Difficulty.MEDIUM
                )
            }
        }
    }

    override fun getTestById(testId: String): Flow<TestSummary?> {
        return mockTestDao.getMockTestsFlow().map { list ->
            val match = list.find { it.id == testId } ?: list.firstOrNull()
            match?.let { entity ->
                val qIds = parseJsonArray(entity.questionIdsJson)
                TestSummary(
                    id = entity.id,
                    title = entity.title,
                    categoryName = "Timed Assessment",
                    description = entity.description,
                    durationMinutes = entity.durationSeconds / 60,
                    questionCount = qIds.size,
                    difficulty = Difficulty.MEDIUM
                )
            }
        }
    }

    private fun parseDifficulty(diff: String): Difficulty {
        return when (diff.lowercase()) {
            "easy" -> Difficulty.EASY
            "hard" -> Difficulty.HARD
            else -> Difficulty.MEDIUM
        }
    }

    private fun parseJsonArray(json: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                result.add(array.getString(i))
            }
        } catch (e: Exception) {
            // Fallback
        }
        return result
    }
}