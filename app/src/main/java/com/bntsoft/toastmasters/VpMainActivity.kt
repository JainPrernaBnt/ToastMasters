package com.bntsoft.toastmasters

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bntsoft.toastmasters.databinding.ActivityVpMainBinding
import com.bntsoft.toastmasters.utils.NotificationPermissionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint
class VpMainActivity : BaseActivity() {
    private lateinit var binding: ActivityVpMainBinding
    
    @Inject
    lateinit var notificationPermissionManager: NotificationPermissionManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handleNotificationPermissionResult(isGranted)
    }
    
    fun hideBottomNavigation() {
        binding.bottomNavView.visibility = View.GONE
    }
    
    fun showBottomNavigation() {
        binding.bottomNavView.visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVpMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.title_dashboard)

        setupNavigation()
        
        // Request notification permission for fresh installs
        requestNotificationPermissionForFreshInstall()
    }

    private fun setupNavigation() {
        try {
            // Find the NavHostFragment
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                ?: throw IllegalStateException("NavHostFragment not found")

            // Get the NavController
            val navController = navHostFragment.navController

            // Set up the navigation graph programmatically to ensure it's properly initialized
            val navGraph = navController.navInflater.inflate(R.navigation.vp_nav_graph)
            
            // Set up bottom navigation
            binding.bottomNavView.setupWithNavController(navController)
            
            // Update action bar title based on destination
            // Add navigation listener for debugging and title updates
            navController.addOnDestinationChangedListener { _, destination, _ ->

                // Update the action bar title
                supportActionBar?.title = when (destination.id) {
                    R.id.dashboardFragment -> getString(R.string.title_dashboard)
                    R.id.createMeetingFragment -> getString(R.string.title_meetings)
                    R.id.assignRolesFragment -> getString(R.string.title_assign_roles)
                    R.id.leaderboardFragment -> getString(R.string.title_leaderboard)
                    R.id.reportsFragment -> getString(R.string.title_reports)
                    R.id.settingsFragment -> getString(R.string.title_settings)
                    R.id.agendaTableFragment -> getString(R.string.agenda)
                    R.id.editMeetingFragment -> getString(R.string.title_edit_meeting)
                    else -> getString(R.string.app_name)
                }

                // Hide bottom navigation for specific fragments
                val shouldHideBottomNav = when (destination.id) {
                    R.id.createAgendaFragment -> true
                    R.id.agendaTableFragment -> true
                    R.id.editMeetingFragment -> true
                    R.id.memberRoleAssignFragment -> true
                    else -> false
                } || destination.label.toString().contains("Agenda", ignoreCase = true)

                // Debug logging
                Log.d("VpMainActivity", "Destination ID: ${destination.id}, Label: ${destination.label}, Should hide bottom nav: $shouldHideBottomNav")

                if (shouldHideBottomNav) {
                    hideBottomNavigation()
                } else {
                    showBottomNavigation()
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.status_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reports -> {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                navController.navigate(R.id.reportsFragment)
                val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
                bottomNav.visibility = View.GONE
                true
            }

            R.id.action_settings -> {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                navController.navigate(R.id.settingsFragment)
                val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
                bottomNav.visibility = View.GONE
                true
            }

            R.id.action_notifications -> {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                navController.navigate(R.id.memberApprovalFragment)
                val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
                bottomNav.visibility = View.GONE
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestNotificationPermissionForFreshInstall() {
        if (notificationPermissionManager.shouldRequestPermission(this)) {
            notificationPermissionManager.requestNotificationPermission(this) { isGranted ->
                handleNotificationPermissionResult(isGranted)
            }
        }
    }

    private fun handleNotificationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            Log.d("VpMainActivity", "Notification permission granted for VP Education")
        } else {
            Log.d("VpMainActivity", "Notification permission denied for VP Education")
            
            if (notificationPermissionManager.hasReachedMaxDenials()) {
                notificationPermissionManager.showPermissionExplanationDialog(
                    context = this,
                    userRole = "vp_education"
                ) {
                    openAppSettings()
                }
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
} 