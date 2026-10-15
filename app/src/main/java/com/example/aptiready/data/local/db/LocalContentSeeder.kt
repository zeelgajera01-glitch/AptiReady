package com.example.aptiready.data.local.db

import android.content.Context
import android.util.JsonReader
import androidx.room.withTransaction
import com.example.aptiready.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader

object LocalContentSeeder {
    const val QUESTION_BANK_VERSION = 2
    const val EXPECTED_QUESTION_COUNT = 48_000
    private const val BATCH_SIZE = 1_000

    suspend fun seedIfNeeded(context: Context, database: AppDatabase) = withContext(Dispatchers.IO) {
        val contentDao = database.contentDao()

        // FAST PATH: If the database is already fully seeded, return in 1ms!
        if (contentDao.getQuestionCountAtLeastVersion(QUESTION_BANK_VERSION) >= EXPECTED_QUESTION_COUNT) {
            return@withContext
        }

        database.withTransaction {
            val categories = readAssetArray(context, "starter_categories.json")
            contentDao.insertCategories((0 until categories.length()).map { index ->
                val obj = categories.getJSONObject(index)
                val id = obj.getString("id")
                CategoryEntity(
                    id = id,
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    iconRes = when (id) {
                        "quant" -> R.drawable.ic_category_quant
                        "logical" -> R.drawable.ic_category_logical
                        else -> R.drawable.ic_category_verbal
                    },
                    sortOrder = obj.getInt("sortOrder"),
                    published = obj.getBoolean("published")
                )
            })

            val topics = readAssetArray(context, "starter_topics.json")
            contentDao.insertTopics((0 until topics.length()).map { index ->
                val obj = topics.getJSONObject(index)
                TopicEntity(
                    id = obj.getString("id"),
                    categoryId = obj.getString("categoryId"),
                    categoryName = obj.getString("categoryName"),
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    difficulty = obj.getString("difficulty"),
                    sampleQuestionCount = obj.getInt("sampleQuestionCount"),
                    formulaPreview = obj.getString("formulaPreview"),
                    sortOrder = obj.getInt("sortOrder"),
                    published = obj.getBoolean("published")
                )
            })

            if (contentDao.getQuestionCountAtLeastVersion(QUESTION_BANK_VERSION) < EXPECTED_QUESTION_COUNT) {
                val now = System.currentTimeMillis()
                var imported = 0
                // Stream questions directly using JsonReader for fast IO and low memory allocation
                JsonReader(InputStreamReader(context.assets.open("starter_questions.json"), Charsets.UTF_8)).use { reader ->
                    val batch = ArrayList<QuestionEntity>(BATCH_SIZE)
                    reader.beginArray()
                    while (reader.hasNext()) {
                        batch.add(streamQuestionDirect(reader, now))
                        imported += 1
                        if (batch.size == BATCH_SIZE) {
                            contentDao.insertQuestions(batch)
                            batch.clear()
                        }
                    }
                    reader.endArray()
                    if (batch.isNotEmpty()) contentDao.insertQuestions(batch)
                }
                check(imported == EXPECTED_QUESTION_COUNT) {
                    "Question bank contains $imported records; expected $EXPECTED_QUESTION_COUNT"
                }
            }

            val tests = readAssetArray(context, "starter_tests.json")
            database.mockTestDao().insertMockTests((0 until tests.length()).map { index ->
                val obj = tests.getJSONObject(index)
                MockTestEntity(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    questionIdsJson = obj.getJSONArray("questionIds").toString(),
                    durationSeconds = obj.getInt("durationSeconds"),
                    marksPerCorrect = obj.getInt("marksPerCorrect"),
                    published = obj.getBoolean("published"),
                    version = obj.getInt("version")
                )
            })
        }
    }

    private fun readAssetArray(context: Context, name: String): JSONArray =
        context.assets.open(name).bufferedReader(Charsets.UTF_8).use { JSONArray(it.readText()) }

    private fun streamQuestionDirect(reader: JsonReader, now: Long): QuestionEntity {
        var id = ""
        var topicId = ""
        var categoryId = ""
        var difficulty = ""
        var languageCode = "en"
        var questionText = ""
        var optionsJson = "[]"
        var correctOptionId = ""
        var explanation = ""
        var hint = ""
        var extraHint = ""
        var published = true
        var version = 1

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "topicId" -> topicId = reader.nextString()
                "categoryId" -> categoryId = reader.nextString()
                "difficulty" -> difficulty = reader.nextString()
                "languageCode" -> languageCode = reader.nextString()
                "questionText" -> questionText = reader.nextString()
                "options" -> optionsJson = parseOptionsDirect(reader)
                "correctOptionId" -> correctOptionId = reader.nextString()
                "explanation" -> explanation = reader.nextString()
                "hint" -> hint = reader.nextString()
                "extraHint" -> extraHint = reader.nextString()
                "published" -> published = reader.nextBoolean()
                "version" -> version = reader.nextInt()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return QuestionEntity(
            id = id,
            topicId = topicId,
            categoryId = categoryId,
            difficulty = difficulty,
            languageCode = languageCode,
            questionText = questionText,
            optionsJson = optionsJson,
            correctOptionId = correctOptionId,
            explanation = explanation,
            hint = hint,
            extraHint = extraHint.trim(),
            published = published,
            version = version,
            updatedAt = now
        )
    }

    private fun parseOptionsDirect(reader: JsonReader): String {
        val array = JSONArray()
        reader.beginArray()
        while (reader.hasNext()) {
            var optId = ""
            var optText = ""
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> optId = reader.nextString()
                    "text" -> optText = reader.nextString()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            array.put(JSONObject().apply {
                put("id", optId)
                put("text", optText)
            })
        }
        reader.endArray()
        return array.toString()
    }
}