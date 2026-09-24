package com.example.aptiready

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.aptiready.data.local.db.LocalContentSeeder
import com.example.aptiready.data.model.ThemeMode
import com.example.aptiready.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AptiRiseApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        com.example.aptiready.ui.ads.AdConfig.validateAdMobConfiguration()
        appContainer = AppContainer(this)

        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                    runCatching { appContainer.adSessionManager.onAppForeground() }
                        .onFailure { com.example.aptiready.ui.ads.AdConfig.isAdsEnabled = false }
                    appContainer.rewardDelivery.resumePending()
                }
                override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                    runCatching { appContainer.adSessionManager.onAppBackground() }
                        .onFailure { com.example.aptiready.ui.ads.AdConfig.isAdsEnabled = false }
                }
            })

        applicationScope.launch {
            LocalContentSeeder.seedIfNeeded(this@AptiRiseApplication, appContainer.database)

            appContainer.themeRepository.themeMode.collectLatest { mode ->
                val nightMode = when (mode) {
                    ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }
        }
    }
}
