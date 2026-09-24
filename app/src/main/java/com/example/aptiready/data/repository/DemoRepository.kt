package com.example.aptiready.data.repository

import com.example.aptiready.R
import com.example.aptiready.data.model.Category
import com.example.aptiready.data.model.Difficulty
import com.example.aptiready.data.model.TestSummary
import com.example.aptiready.data.model.Topic
import com.example.aptiready.data.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DemoRepository : QuestionRepository, ProgressRepository {

    private val categories = listOf(
        Category(
            id = "quant",
            title = "Quantitative Aptitude",
            description = "Numerical ability, arithmetic, percentages & algebra",
            iconRes = R.drawable.ic_category_quant,
            topicCount = 5
        ),
        Category(
            id = "logical",
            title = "Logical Reasoning",
            description = "Patterns, series, blood relations & syllogisms",
            iconRes = R.drawable.ic_category_logical,
            topicCount = 3
        ),
        Category(
            id = "verbal",
            title = "Verbal Ability",
            description = "Vocabulary, grammar, reading comprehension & synonyms",
            iconRes = R.drawable.ic_category_verbal,
            topicCount = 1
        )
    )

    private val topics = listOf(
        Topic(
            id = "percentages",
            categoryId = "quant",
            categoryName = "Quantitative Aptitude",
            title = "Percentages & Applications",
            description = "Fraction to percentage conversions, population growth, profit percentage & successive changes.",
            difficulty = Difficulty.MEDIUM,
            sampleQuestionCount = 15,
            formulaPreview = "• Percentage = (Value / Total) × 100%\n• Percentage Increase = [(New - Original) / Original] × 100%\n• Fraction Equivalents: 1/2 = 50%, 1/3 = 33.33%, 1/4 = 25%, 1/5 = 20%"
        ),
        Topic(
            id = "ratios",
            categoryId = "quant",
            categoryName = "Quantitative Aptitude",
            title = "Ratios & Proportions",
            description = "Direct and inverse proportions, mixture problems, partnership share calculations.",
            difficulty = Difficulty.EASY,
            sampleQuestionCount = 12,
            formulaPreview = "• Ratio a:b = a/b\n• Proportion: If a:b = c:d, then a × d = b × c\n• Compounded Ratio of (a:b) and (c:d) = (ac : bd)"
        ),
        Topic(
            id = "averages",
            categoryId = "quant",
            categoryName = "Quantitative Aptitude",
            title = "Averages & Weighted Means",
            description = "Arithmetic mean, weighted average formulas, speed averages across distance segments.",
            difficulty = Difficulty.EASY,
            sampleQuestionCount = 10,
            formulaPreview = "• Average = (Sum of All Observations) / (Total Number of Observations)\n• Average Speed = (Total Distance) / (Total Time Taken)\n• Weighted Average = (w1x1 + w2x2) / (w1 + w2)"
        ),
        Topic(
            id = "profit_loss",
            categoryId = "quant",
            categoryName = "Quantitative Aptitude",
            title = "Profit and Loss",
            description = "Cost price, selling price, marked price, trade discount and margin percentage calculations.",
            difficulty = Difficulty.MEDIUM,
            sampleQuestionCount = 15,
            formulaPreview = "• Profit = SP - CP | Loss = CP - SP\n• Profit % = (Profit / CP) × 100%\n• SP = [(100 + Profit%) / 100] × CP"
        ),
        Topic(
            id = "time_work",
            categoryId = "quant",
            categoryName = "Quantitative Aptitude",
            title = "Time and Work",
            description = "Individual work efficiency, pipe and cistern rates, man-hours efficiency equivalence.",
            difficulty = Difficulty.HARD,
            sampleQuestionCount = 14,
            formulaPreview = "• If A completes a work in 'n' days, A's 1 day work = 1/n\n• Work Done = Rate × Time\n• (M1 × D1 × H1) / W1 = (M2 × D2 × H2) / W2"
        ),
        Topic(
            id = "number_series",
            categoryId = "logical",
            categoryName = "Logical Reasoning",
            title = "Number Series & Pattern Analysis",
            description = "Missing term identification, Fibonacci sequences, alternate difference and prime patterns.",
            difficulty = Difficulty.MEDIUM,
            sampleQuestionCount = 15,
            formulaPreview = "• Common Types: Arithmetic (+d), Geometric (×r), Square/Cube differences (n² ± k).\n• Tip: Calculate differences between consecutive terms twice to detect sub-patterns."
        ),
        Topic(
            id = "directions",
            categoryId = "logical",
            categoryName = "Logical Reasoning",
            title = "Direction Sense Test",
            description = "Compass direction navigation, pythagorean displacement, shadow orientation relative to sun.",
            difficulty = Difficulty.EASY,
            sampleQuestionCount = 10,
            formulaPreview = "• Shortest Distance = √(x² + y²) [Pythagoras Theorem]\n• Sunrise: Shadow falls towards West.\n• Sunset: Shadow falls towards East."
        ),
        Topic(
            id = "syllogisms",
            categoryId = "logical",
            categoryName = "Logical Reasoning",
            title = "Syllogisms & Deductive Logic",
            description = "Venn diagram deductive logic, universal positive/negative statements, conditional inferences.",
            difficulty = Difficulty.HARD,
            sampleQuestionCount = 12,
            formulaPreview = "• All A are B (Inclusion)\n• No A is B (Exclusive)\n• Some A are B (Intersection)\n• Rule: A conclusion is valid ONLY if it follows logically in ALL possible Venn configurations."
        ),
        Topic(
            id = "grammar_vocab",
            categoryId = "verbal",
            categoryName = "Verbal Ability",
            title = "Grammar & Vocabulary",
            description = "Subject-verb agreement, common idioms, sentence corrections, synonyms and antonyms.",
            difficulty = Difficulty.EASY,
            sampleQuestionCount = 20,
            formulaPreview = "• Subject-Verb Agreement: Singular subject requires singular verb.\n• Active vs Passive Voice: Focus on agent vs patient of action.\n• Contextual Clues: Pay attention to contrast words like 'however', 'despite', 'nonetheless'."
        )
    )

    private val testCatalog = listOf(
        TestSummary(
            id = "quant_sprint_01",
            title = "Quant Speed & Accuracy Sprint 01",
            categoryName = "Quantitative Aptitude",
            description = "20 timed questions on Percentages, Ratios, Averages, and Profit & Loss.",
            durationMinutes = 15,
            questionCount = 20,
            difficulty = Difficulty.MEDIUM
        ),
        TestSummary(
            id = "reasoning_sprint_01",
            title = "Logical Reasoning Sprint 01",
            categoryName = "Logical Reasoning",
            description = "25 questions evaluating Number Series, Directions, and Syllogism deductive logic.",
            durationMinutes = 20,
            questionCount = 25,
            difficulty = Difficulty.MEDIUM
        ),
        TestSummary(
            id = "verbal_challenge_01",
            title = "Verbal Ability Practice Challenge",
            categoryName = "Verbal Ability",
            description = "20 vocabulary and grammar correction questions.",
            durationMinutes = 15,
            questionCount = 20,
            difficulty = Difficulty.EASY
        ),
        TestSummary(
            id = "full_length_mock_01",
            title = "Full-Length Comprehensive Mock Test 01",
            categoryName = "All Categories",
            description = "Simulates complete placement exam conditions with 50 questions spanning all three sections.",
            durationMinutes = 60,
            questionCount = 50,
            difficulty = Difficulty.HARD
        )
    )



    override fun getCategories(): Flow<List<Category>> = flowOf(categories)

    override fun getTopics(categoryId: String?, searchQuery: String?): Flow<List<Topic>> {
        val query = searchQuery?.trim()
        val filtered = topics.filter { topic ->
            val matchesCategory = categoryId == null || categoryId == "all" || topic.categoryId.equals(categoryId, ignoreCase = true)
            val matchesSearch = query.isNullOrEmpty() ||
                    topic.title.contains(query, ignoreCase = true) ||
                    topic.description.contains(query, ignoreCase = true) ||
                    topic.categoryName.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
        return flowOf(filtered)
    }

    override fun getTopicById(topicId: String): Flow<Topic?> {
        return flowOf(topics.find { it.id == topicId } ?: topics.firstOrNull())
    }

    override fun getTestCatalog(): Flow<List<TestSummary>> = flowOf(testCatalog)

    override fun getTestById(testId: String): Flow<TestSummary?> {
        return flowOf(testCatalog.find { it.id == testId } ?: testCatalog.firstOrNull())
    }

    override fun getUserProgress(includeSampleData: Boolean): Flow<UserProgress> {
        return if (includeSampleData) {
            flowOf(
                UserProgress(
                    isSampleData = true,
                    accuracyPercentage = 82,
                    streakDays = 5,
                    topicsCompleted = 12,
                    totalQuestionsAnswered = 140,
                    quantMastery = 80,
                    logicalMastery = 75,
                    verbalMastery = 90
                )
            )
        } else {
            flowOf(
                UserProgress(
                    isSampleData = false,
                    accuracyPercentage = 0,
                    streakDays = 0,
                    topicsCompleted = 0,
                    totalQuestionsAnswered = 0,
                    quantMastery = 0,
                    logicalMastery = 0,
                    verbalMastery = 0
                )
            )
        }
    }

    override fun getRecentTopic(): Flow<Topic?> {
        return flowOf(topics.firstOrNull())
    }

}