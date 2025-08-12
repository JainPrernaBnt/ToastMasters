package com.bntsoft.toastmasters

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bntsoft.toastmasters.databinding.ActivityVpMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VpMainActivity : BaseActivity() {
    private lateinit var binding: ActivityVpMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVpMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.title_dashboard)

        setupNavigation()
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
            navGraph.setStartDestination(R.id.dashboardFragment)
            navController.graph = navGraph

            // Set up the bottom navigation with NavController
            binding.bottomNavView.setupWithNavController(navController)

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
                    else -> getString(R.string.app_name)
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

            else -> super.onOptionsItemSelected(item)
        }
    }
} 