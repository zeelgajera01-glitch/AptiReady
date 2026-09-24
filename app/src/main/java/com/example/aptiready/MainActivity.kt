package com.example.aptiready

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.aptiready.databinding.ActivityMainBinding
import com.example.aptiready.ui.ads.AdProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adProvider = (application as AptiRiseApplication).appContainer.adProvider
        adProvider.initialize(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            binding.mainRoot.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Hide bottom navigation on detail/auth/practice session screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.topicDetailFragment,
                R.id.testDetailFragment,
                R.id.upcomingFeatureFragment,
                R.id.welcomeFragment,
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.verifyEmailFragment,
                R.id.forgotPasswordFragment,
                R.id.editProfileFragment,
                R.id.practiceSessionFragment,
                R.id.practiceResultFragment,
                R.id.practiceReviewFragment,
                R.id.bookmarksFragment,
                R.id.adDiagnosticsFragment,
                R.id.accountDeletionFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        val container = (application as AptiRiseApplication).appContainer
        container.adProvider.initialize(this)
        container.rewardDelivery.resumePending()
    }
}
