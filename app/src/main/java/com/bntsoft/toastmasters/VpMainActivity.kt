package com.bntsoft.toastmasters

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bntsoft.toastmasters.databinding.ActivityVpMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VpMainActivity : BaseActivity() {
    private lateinit var binding: ActivityVpMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVpMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Setup bottom navigation with navigation controller
        binding.bottomNavView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.status_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle status bar item clicks
        return when (item.itemId) {
            R.id.action_reports -> {
                // Navigate to ReportsFragment
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                navController.navigate(R.id.reportsFragment)
                true
            }

            R.id.action_settings -> {
                // Navigate to SettingsFragment
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                navController.navigate(R.id.settingsFragment)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
} 