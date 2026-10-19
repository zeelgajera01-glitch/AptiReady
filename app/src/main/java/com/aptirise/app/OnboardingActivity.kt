package com.aptirise.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aptirise.app.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {
    private var leaving = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.onboardingBack.setOnClickListener { openHome() }
        binding.onboardingContinue.setOnClickListener { openHome() }
        binding.onboardingSkip.setOnClickListener { openHome() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = openHome()
        })
    }
    private fun openHome() {
        if (leaving) return
        leaving = true
        getSharedPreferences("first_run", MODE_PRIVATE).edit().putBoolean("onboarding_complete", true).apply()
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
