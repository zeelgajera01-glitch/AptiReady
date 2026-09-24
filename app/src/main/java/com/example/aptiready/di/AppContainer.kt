package com.example.aptiready.di

import android.content.Context
import com.example.aptiready.data.local.AdPreferencesRepository
import com.example.aptiready.data.local.AdPreferencesRepositoryImpl
import com.example.aptiready.data.local.SyncPreferencesRepository
import com.example.aptiready.data.local.ThemePreferencesRepository
import com.example.aptiready.data.local.db.AppDatabase
import com.example.aptiready.data.repository.AuthRepository
import com.example.aptiready.data.repository.CloudSyncRepository
import com.example.aptiready.data.repository.CloudSyncRepositoryImpl
import com.example.aptiready.data.repository.DemoRepository
import com.example.aptiready.data.repository.FirebaseAuthRepository
import com.example.aptiready.data.repository.FirestoreProfileRepository
import com.example.aptiready.data.repository.MockTestRepository
import com.example.aptiready.data.repository.MockTestRepositoryImpl
import com.example.aptiready.data.repository.PracticeRepository
import com.example.aptiready.data.repository.PracticeRepositoryImpl
import com.example.aptiready.data.repository.ProfileRepository
import com.example.aptiready.data.repository.ProgressRepository
import com.example.aptiready.data.repository.QuestionRepository
import com.example.aptiready.data.repository.RoomProgressRepositoryImpl
import com.example.aptiready.data.repository.RoomQuestionRepositoryImpl
import com.example.aptiready.data.repository.ThemeRepository
import com.example.aptiready.ui.ads.AdProvider
import com.example.aptiready.ui.ads.AdSessionManager
import com.example.aptiready.ui.ads.ConsentManager
import com.example.aptiready.ui.ads.MobileAdsProvider

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val syncPreferencesRepository: SyncPreferencesRepository by lazy {
        SyncPreferencesRepository(context)
    }

    val demoRepository: DemoRepository by lazy {
        DemoRepository()
    }

    val roomQuestionRepository: QuestionRepository by lazy {
        RoomQuestionRepositoryImpl(database)
    }

    val roomProgressRepository: ProgressRepository by lazy {
        RoomProgressRepositoryImpl(database, authRepository)
    }

    val questionRepository: QuestionRepository get() = roomQuestionRepository
    val progressRepository: ProgressRepository get() = roomProgressRepository

    val practiceRepository: PracticeRepository by lazy {
        PracticeRepositoryImpl(context, database, cloudSyncRepositoryProvider = { cloudSyncRepository })
    }

    val mockTestRepository: MockTestRepository by lazy {
        MockTestRepositoryImpl(context, database, cloudSyncRepositoryProvider = { cloudSyncRepository })
    }

    val profileRepository: ProfileRepository by lazy {
        FirestoreProfileRepository(context)
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(context, profileRepository)
    }

    val cloudSyncRepository: CloudSyncRepository by lazy {
        CloudSyncRepositoryImpl(context, database, syncPreferencesRepository, authRepository)
    }

    val freeHintRepository: com.example.aptiready.data.repository.FreeHintRepository by lazy {
        com.example.aptiready.data.repository.FreeHintRepositoryImpl(context, authRepository)
    }

    val themeRepository: ThemeRepository by lazy {
        ThemePreferencesRepository(context)
    }

    val consentManager: ConsentManager by lazy {
        ConsentManager(context)
    }

    val adPreferencesRepository: AdPreferencesRepository by lazy {
        AdPreferencesRepositoryImpl(context)
    }

    val adSessionManager: AdSessionManager by lazy {
        AdSessionManager(adPreferencesRepository, com.example.aptiready.ui.ads.SystemTimeProvider(context))
    }

    val rewardDelivery by lazy {
        com.example.aptiready.ui.ads.RewardDelivery(context.applicationContext, practiceRepository)
    }

    val adProvider: MobileAdsProvider by lazy {
        MobileAdsProvider(context.applicationContext, consentManager, adSessionManager)
    }
}
