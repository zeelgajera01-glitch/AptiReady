package com.example.aptiready.data.local.db

import android.content.Context
import com.example.aptiready.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object LocalContentSeeder {

    suspend fun seedIfNeeded(context: Context, database: AppDatabase) = withContext(Dispatchers.IO) {
        val contentDao = database.contentDao()

        val defaultCategories = listOf(
            CategoryEntity("quant", "Quantitative Aptitude", "Numerical ability, arithmetic, percentages & algebra", R.drawable.ic_category_quant, 1, true),
            CategoryEntity("logical", "Logical Reasoning", "Patterns, series, blood relations & syllogisms", R.drawable.ic_category_logical, 2, true),
            CategoryEntity("verbal", "Verbal Ability", "Vocabulary, grammar, reading comprehension & synonyms", R.drawable.ic_category_verbal, 3, true)
        )

        val defaultTopics = listOf(
            TopicEntity("percentages", "quant", "Quantitative Aptitude", "Percentages & Applications", "Fraction conversions, population growth, profit percentage & successive changes.", "medium", 6, "• Percentage = (Value / Total) × 100%\n• Percentage Increase = [(New - Original) / Original] × 100%\n• Fraction Equivalents: 1/2 = 50%, 1/3 = 33.33%, 1/4 = 25%, 1/5 = 20%", 1, true),
            TopicEntity("ratios", "quant", "Quantitative Aptitude", "Ratios & Proportions", "Direct and inverse proportions, mixture problems, partnership share calculations.", "easy", 6, "• Ratio a:b = a/b\n• Proportion: If a:b = c:d, then a × d = b × c\n• Compounded Ratio of (a:b) and (c:d) = (ac : bd)", 2, true),
            TopicEntity("averages", "quant", "Quantitative Aptitude", "Averages & Weighted Means", "Arithmetic mean, weighted average formulas, speed averages across distance segments.", "easy", 6, "• Average = (Sum of All Observations) / (Total Number of Observations)\n• Average Speed = (Total Distance) / (Total Time Taken)\n• Weighted Average = (w1x1 + w2x2) / (w1 + w2)", 3, true),
            TopicEntity("profit_loss", "quant", "Quantitative Aptitude", "Profit and Loss", "Cost price, selling price, marked price, trade discount and margin percentage calculations.", "medium", 6, "• Profit = SP - CP | Loss = CP - SP\n• Profit % = (Profit / CP) × 100%\n• SP = [(100 + Profit%) / 100] × CP", 4, true),
            TopicEntity("time_work", "quant", "Quantitative Aptitude", "Time and Work", "Individual work efficiency, pipe and cistern rates, man-hours efficiency equivalence.", "hard", 6, "• If A completes a work in 'n' days, A's 1 day work = 1/n\n• Work Done = Rate × Time\n• (M1 × D1 × H1) / W1 = (M2 × D2 × H2) / W2", 5, true),
            TopicEntity("number_series", "logical", "Logical Reasoning", "Number Series & Pattern Analysis", "Missing term identification, Fibonacci sequences, alternate difference and prime patterns.", "medium", 6, "• Common Types: Arithmetic (+d), Geometric (×r), Square/Cube differences (n² ± k).\n• Tip: Calculate differences between consecutive terms twice to detect sub-patterns.", 6, true),
            TopicEntity("directions", "logical", "Logical Reasoning", "Direction Sense Test", "Compass direction navigation, pythagorean displacement, shadow orientation relative to sun.", "easy", 6, "• Shortest Distance = √(x² + y²) [Pythagoras Theorem]\n• Sunrise: Shadow falls towards West.\n• Sunset: Shadow falls towards East.", 7, true),
            TopicEntity("syllogisms", "logical", "Logical Reasoning", "Syllogisms & Deductive Logic", "Venn diagram deductive logic, universal positive/negative statements, conditional inferences.", "hard", 6, "• All A are B (Inclusion)\n• No A is B (Exclusive)\n• Some A are B (Intersection)\n• Rule: A conclusion is valid ONLY if it follows logically in ALL possible Venn configurations.", 8, true),
            TopicEntity("grammar_vocab", "verbal", "Verbal Ability", "Grammar & Vocabulary", "Subject-verb agreement, common idioms, sentence corrections, synonyms and antonyms.", "easy", 6, "• Subject-Verb Agreement: Singular subject requires singular verb.\n• Active vs Passive Voice: Focus on agent vs patient of action.\n• Contextual Clues: Pay attention to contrast words like 'however', 'despite', 'nonetheless'.", 9, true),
            TopicEntity("reading_comp", "verbal", "Verbal Ability", "Reading Comprehension & Analysis", "Paragraph tone analysis, central thesis identification, logical inferences & weaken arguments.", "medium", 6, "• Main Idea: Identify topic sentence in intro/conclusion.\n• Tone Analysis: Pay attention to adjectives (e.g. alarmist, neutral, optimistic).\n• Inferences: Must follow strictly from passage evidence.", 10, true)
        )

        contentDao.insertCategories(defaultCategories)
        contentDao.insertTopics(defaultTopics)

        // Seed or update all 24000 starter questions from starter_questions.json if total count < 24000
        val currentQuestionCount = contentDao.getTotalQuestionCount()
        if (currentQuestionCount < 24000) {
            val jsonString = context.assets.open("starter_questions.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val questionEntities = mutableListOf<QuestionEntity>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                questionEntities.add(
                    QuestionEntity(
                        id = obj.getString("id"),
                        topicId = obj.getString("topicId"),
                        categoryId = obj.getString("categoryId"),
                        difficulty = obj.getString("difficulty"),
                        languageCode = obj.getString("languageCode"),
                        questionText = obj.getString("questionText"),
                        optionsJson = obj.getJSONArray("options").toString(),
                        correctOptionId = obj.getString("correctOptionId"),
                        explanation = obj.getString("explanation"),
                        hint = obj.getString("hint"),
                        extraHint = obj.optString("extraHint", "").trim(),
                        published = obj.getBoolean("published"),
                        version = obj.getInt("version"),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            contentDao.insertQuestions(questionEntities)
        }

        // Seed starter mock tests from starter_tests.json
        val mockTestDao = database.mockTestDao()
        val testString = context.assets.open("starter_tests.json").bufferedReader().use { it.readText() }
        val testArray = JSONArray(testString)
        val mockTestEntities = mutableListOf<MockTestEntity>()

        for (i in 0 until testArray.length()) {
            val obj = testArray.getJSONObject(i)
            mockTestEntities.add(
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
            )
        }
        mockTestDao.insertMockTests(mockTestEntities)
    }
}
