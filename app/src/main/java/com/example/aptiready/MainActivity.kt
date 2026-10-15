package com.example.aptiready

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.aptiready.data.model.AuthState
import com.example.aptiready.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isKeepOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { isKeepOnScreen }

        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appContainer = (application as AptiRiseApplication).appContainer
        val app = application as AptiRiseApplication
        binding.btnRetryContent.setOnClickListener { app.prepareContent() }
        lifecycleScope.launch {
            app.contentState.collectLatest { state ->
                binding.contentLoadingOverlay.visibility = if (state == "READY") View.GONE else View.VISIBLE
                binding.contentLoadingProgress.visibility = if (state == "LOADING") View.VISIBLE else View.GONE
                binding.btnRetryContent.visibility = if (state == "ERROR") View.VISIBLE else View.GONE
                binding.contentLoadingMessage.text = if (state == "ERROR")
                    "Could not prepare your questions. Check available storage and try again."
                else "Preparing your questions… This may take a little longer after an update."
            }
        }
        val adProvider = appContainer.adProvider
        adProvider.initialize(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            val bottom = if (binding.bottomNavigation.visibility == View.GONE) systemBars.bottom else 0
            binding.mainRoot.setPadding(systemBars.left, systemBars.top, systemBars.right, bottom)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Determine initial start destination synchronously to avoid any flash of Welcome/Login screen
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        val initialUser = if (appContainer.authRepository.isFirebaseConfigured) {
            try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
        } else null

        val initialStartDestination = when {
            initialUser != null -> R.id.homeFragment
            else -> R.id.welcomeFragment
        }

        navGraph.setStartDestination(initialStartDestination)
        if (savedInstanceState == null || runCatching { navController.graph }.isFailure) {
            navController.graph = navGraph
        }
        isKeepOnScreen = false

        binding.bottomNavigation.setupWithNavController(navController)

        // Check for mandatory in-app force updates via Firebase Remote Config
        com.example.aptiready.data.remote.ForceUpdateManager(this).checkForUpdate()

        // Observe authState for live logout state changes
        lifecycleScope.launch {
            appContainer.authRepository.authState.collectLatest { state ->
                if (state is AuthState.SignedOut) {
                    val currentDest = navController.currentDestination?.id
                    if (currentDest != null && currentDest != R.id.welcomeFragment) {
                        try {
                            navController.navigate(R.id.action_global_to_welcome)
                        } catch (_: Exception) {
                            // Already navigated or destination changed
                        }
                    }
                }
            }
        }

        // Hide bottom navigation on detail/auth/practice session screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val mainDestinations = setOf(
                R.id.homeFragment, R.id.practiceFragment, R.id.testCatalogFragment,
                R.id.progressFragment, R.id.profileFragment
            )
            binding.bottomNavigation.visibility = if (destination.id in mainDestinations) View.VISIBLE else View.GONE
            ViewCompat.requestApplyInsets(binding.mainRoot)
        }
    }

    override fun onResume() {
        super.onResume()
        val container = (application as AptiRiseApplication).appContainer
        container.adProvider.initialize(this)
        container.rewardDelivery.resumePending()
    }
}
