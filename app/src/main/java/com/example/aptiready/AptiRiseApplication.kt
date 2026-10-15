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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AptiRiseApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _contentState = kotlinx.coroutines.flow.MutableStateFlow("LOADING")
    val contentState = _contentState.asStateFlow()
    private var contentJob: kotlinx.coroutines.Job? = null

    fun prepareContent() {
        if (contentJob?.isActive == true || _contentState.value == "READY") return
        _contentState.value = "LOADING"
        contentJob = applicationScope.launch {
            try {
                LocalContentSeeder.seedIfNeeded(this@AptiRiseApplication, appContainer.database)
                _contentState.value = "READY"
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                android.util.Log.e("ContentImport", "Question bank initialization failed", failure)
                _contentState.value = "ERROR"
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        com.example.aptiready.ui.ads.AdConfig.validateAdMobConfiguration()
        appContainer = AppContainer(this)

        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                    runCatching { appContainer.adSessionManager.onAppForeground() }
                        .onFailure { appContainer.adSessionManager.onStorageFailure(it) }
                    appContainer.rewardDelivery.resumePending()
                }
                override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                    runCatching { appContainer.adSessionManager.onAppBackground() }
                        .onFailure { appContainer.adSessionManager.onStorageFailure(it) }
                }
            })

        prepareContent()
        applicationScope.launch {
            appContainer.themeRepository.themeMode.collectLatest { mode ->
                val nightMode = when (mode) {
                    ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                kotlinx.coroutines.withContext(Dispatchers.Main.immediate) {
                    AppCompatDelegate.setDefaultNightMode(nightMode)
                }
            }
        }
    }
}
