package com.aptirise.app.ui.navigation

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import com.aptirise.app.AptiRiseApplication
import com.aptirise.app.R
import com.aptirise.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/** Shared window and services. Each subclass owns one screen and its ViewModels. */
abstract class ScreenActivity(val screenId: Int) : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var navigationPending = false
    private val app get() = application as AptiRiseApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = NamespaceFragmentFactory()
        // Retired diagnostic tasks redirect to Home with a fresh navigation graph.
        val retiredTask = intent.component?.className?.let { it != javaClass.name } == true
        super.onCreate(if (retiredTask) null else savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val host = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        // Supply the graph on recreation too: NavController restores its saved entries
        // only after a graph is attached (this host deliberately has no XML navGraph).
        val graph = host.navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(screenId)
        host.navController.setGraph(graph, intent.extras)
        val drawerEnabled = screenId in ActivityRoutes.drawerScreens
        binding.toolbar.visibility = View.VISIBLE
        binding.toolbar.setNavigationIcon(if (drawerEnabled) R.drawable.ic_menu else R.drawable.ic_back)
        binding.toolbar.setNavigationContentDescription(if (drawerEnabled) R.string.drawer_open else R.string.cd_back_button)
        if (drawerEnabled) binding.toolbar.setLogo(R.drawable.toolbar_logo)
        binding.drawerLayout.setDrawerLockMode(if (drawerEnabled)
            DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        binding.toolbar.title = if (screenId == R.id.homeFragment) getString(R.string.app_name) else host.navController.currentDestination?.label
        binding.toolbar.setNavigationOnClickListener {
            if (drawerEnabled) binding.drawerLayout.openDrawer(GravityCompat.START)
            else onBackPressedDispatcher.onBackPressed()
        }
        if (drawerEnabled) binding.drawerNavigation.setCheckedItem(screenId)
        binding.drawerNavigation.setNavigationItemSelectedListener { item ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            if (item.itemId == R.id.drawer_privacy_policy) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(getString(R.string.privacy_policy_url)))
                        .addCategory(Intent.CATEGORY_BROWSABLE))
                } catch (_: android.content.ActivityNotFoundException) {
                    android.widget.Toast.makeText(this, R.string.privacy_open_error, android.widget.Toast.LENGTH_LONG).show()
                }
            } else if (item.itemId == R.id.drawer_appearance) {
                val modes = listOf(com.aptirise.app.data.model.ThemeMode.LIGHT, com.aptirise.app.data.model.ThemeMode.DARK, com.aptirise.app.data.model.ThemeMode.SYSTEM)
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.appearance)
                    .setSingleChoiceItems(arrayOf(getString(R.string.theme_light), getString(R.string.theme_dark), getString(R.string.theme_system)), -1) { dialog, index ->
                        lifecycleScope.launch { app.appContainer.themeRepository.setThemeMode(modes[index]) }
                        dialog.dismiss()
                    }.setNegativeButton(android.R.string.cancel, null).show()
            } else if (item.itemId != screenId) openScreen(item.itemId, reuse = true)
            true
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBack()
        })
        val drawerBack = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { binding.drawerLayout.closeDrawer(GravityCompat.START) }
        }
        onBackPressedDispatcher.addCallback(this, drawerBack)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) { drawerBack.isEnabled = true }
            override fun onDrawerClosed(drawerView: View) { drawerBack.isEnabled = false }
        })
        // Protect restored activities when Android recreated the application process.
        binding.btnRetryContent.setOnClickListener { app.prepareContent() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.contentState.collect { state ->
                    binding.contentLoadingOverlay.visibility = if (state == "READY") View.GONE else View.VISIBLE
                    binding.contentLoadingProgress.visibility = if (state == "LOADING") View.VISIBLE else View.GONE
                    binding.btnRetryContent.visibility = if (state == "ERROR") View.VISIBLE else View.GONE
                    binding.contentLoadingMessage.setText(if (state == "ERROR") R.string.content_prepare_error else R.string.content_preparing)
                }
            }
        }
        if (screenId == R.id.homeFragment) {
            com.aptirise.app.data.remote.ForceUpdateManager(this).checkForUpdate()
        }
    }

    override fun onResume() {
        super.onResume()
        navigationPending = false
    }

    fun openScreen(destinationId: Int, args: Bundle? = null, replace: Boolean = false,
                   clearTask: Boolean = false, reuse: Boolean = false) {
        if (navigationPending || isFinishing) return
        val target = Intent(this, ActivityRoutes.activityFor(destinationId)).apply {
            args?.let { putExtras(it) }
            if (clearTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            else if (reuse) addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        navigationPending = true
        startActivity(target)
        if (replace || clearTask) finish()
    }

    fun returnToScreen(destinationId: Int) {
        openScreen(destinationId, reuse = true)
        if (destinationId != screenId) finish()
    }

    fun goBack() {
        if (isTaskRoot && screenId != R.id.homeFragment ) {
            openScreen(R.id.homeFragment, replace = true)
        } else finish()
    }

}
