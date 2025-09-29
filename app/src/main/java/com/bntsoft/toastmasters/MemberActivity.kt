package com.bntsoft.toastmasters

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bntsoft.toastmasters.databinding.ActivityMemberMainBinding
import com.bntsoft.toastmasters.utils.NotificationPermissionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MemberActivity : BaseActivity() {
    private lateinit var binding: ActivityMemberMainBinding
    
    @Inject
    lateinit var notificationPermissionManager: NotificationPermissionManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handleNotificationPermissionResult(isGranted)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.title_dashboard)

        setupNavigation()
        
        // Request notification permission for fresh installs
        requestNotificationPermissionForFreshInstall()
    }

    private fun setupNavigation() {
        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                ?: throw IllegalStateException("NavHostFragment not found")

            val navController = navHostFragment.navController

            // Set up bottom navigation
            binding.bottomNavView.setupWithNavController(navController)

            // Update action bar title and handle bottom nav visibility based on destination
            navController.addOnDestinationChangedListener { _, destination, _ ->
                // Update the action bar title
                supportActionBar?.title = when (destination.id) {
                    R.id.navigation_dashboard -> getString(R.string.title_dashboard)
                    R.id.navigation_meetings -> getString(R.string.title_upcoming_meetings)
                    R.id.navigation_leaderboard -> getString(R.string.title_leaderboard)
                    R.id.navigation_settings -> getString(R.string.title_settings)
                    R.id.agendaTableFragment -> getString(R.string.agenda)
                    else -> getString(R.string.app_name)
                }

                // Hide bottom navigation for specific fragments
                val shouldHideBottomNav = when (destination.id) {
                    R.id.agendaTableFragment -> true
                    else -> false
                } || destination.label.toString().contains("Agenda", ignoreCase = true)

                // Debug logging
                Log.d("MemberActivity", "Destination ID: ${destination.id}, Label: ${destination.label}, Should hide bottom nav: $shouldHideBottomNav")

                binding.bottomNavView.visibility = if (shouldHideBottomNav) View.GONE else View.VISIBLE
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("MemberActivity", "Navigation setup error", e)
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
            Log.d("MemberActivity", "Notification permission granted for Member")
        } else {
            Log.d("MemberActivity", "Notification permission denied for Member")
            
            if (notificationPermissionManager.hasReachedMaxDenials()) {
                notificationPermissionManager.showPermissionExplanationDialog(
                    context = this,
                    userRole = "member"
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
