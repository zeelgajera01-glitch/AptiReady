package com.aptirise.app

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aptirise.app.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Displays real content initialization, with a retry if importing questions fails. */
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private var leaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // A repeated launcher tap should simply reveal the existing app task.
        if (!isTaskRoot && intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            finish()
            return
        }
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val app = application as AptiRiseApplication
        val startedAt = SystemClock.elapsedRealtime()
        binding.splashLogo.alpha = 0f
        binding.splashLogo.scaleX = 0.9f
        binding.splashLogo.scaleY = 0.9f
        binding.splashLogo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).start()
        binding.splashRetry.setOnClickListener { app.prepareContent() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.contentState.collectLatest { content ->
                        binding.splashProgress.visibility = if (content == "ERROR") View.GONE else View.VISIBLE
                        binding.splashRetry.visibility = if (content == "ERROR") View.VISIBLE else View.GONE
                        binding.splashMessage.setText(if (content == "ERROR") R.string.content_prepare_error else R.string.journey_preparing)
                        if (content == "READY" && !leaving) {
                            delay((800L - (SystemClock.elapsedRealtime() - startedAt)).coerceAtLeast(0))
                            leaving = true
                            val complete = getSharedPreferences("first_run", MODE_PRIVATE).getBoolean("onboarding_complete", false)
                            startActivity(Intent(this@SplashActivity, if (complete) MainActivity::class.java else OnboardingActivity::class.java))
                            finish()
                        }
                    }
            }
        }
    }
}
