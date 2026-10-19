package com.aptirise.app.di

import android.content.Context
import com.aptirise.app.data.local.ThemePreferencesRepository
import com.aptirise.app.data.local.db.AppDatabase
import com.aptirise.app.data.repository.DeviceLearningOwner
import com.aptirise.app.data.repository.LearningOwner
import com.aptirise.app.data.repository.MockTestRepository
import com.aptirise.app.data.repository.MockTestRepositoryImpl
import com.aptirise.app.data.repository.PracticeRepository
import com.aptirise.app.data.repository.PracticeRepositoryImpl
import com.aptirise.app.data.repository.ProgressRepository
import com.aptirise.app.data.repository.QuestionRepository
import com.aptirise.app.data.repository.RoomProgressRepositoryImpl
import com.aptirise.app.data.repository.RoomQuestionRepositoryImpl
import com.aptirise.app.data.repository.ThemeRepository

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val roomQuestionRepository: QuestionRepository by lazy {
        RoomQuestionRepositoryImpl(database)
    }

    val roomProgressRepository: ProgressRepository by lazy {
        RoomProgressRepositoryImpl(database, learningOwner)
    }

    val questionRepository: QuestionRepository get() = roomQuestionRepository
    val progressRepository: ProgressRepository get() = roomProgressRepository

    val practiceRepository: PracticeRepository by lazy {
        PracticeRepositoryImpl(context, database)
    }

    val mockTestRepository: MockTestRepository by lazy {
        MockTestRepositoryImpl(context, database)
    }

    val learningOwner: LearningOwner = DeviceLearningOwner

    val freeHintRepository: com.aptirise.app.data.repository.FreeHintRepository by lazy {
        com.aptirise.app.data.repository.FreeHintRepositoryImpl(context)
    }

    val themeRepository: ThemeRepository by lazy {
        ThemePreferencesRepository(context)
    }

}
