package com.bntsoft.toastmasters

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bntsoft.toastmasters.data.model.UserRole
import com.bntsoft.toastmasters.databinding.ActivityMainBinding
import com.bntsoft.toastmasters.presentation.viewmodel.MainViewModel
import com.bntsoft.toastmasters.utils.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val viewModel: MainViewModel by viewModels()

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private val appBarConfiguration = AppBarConfiguration(
        setOf(
            R.id.dashboardFragment,
            R.id.memberDashboardFragment,
            R.id.upcomingMeetingsFragment,
            R.id.settingsFragment2,
            R.id.leaderboardFragment2,
            R.id.createMeetingFragment,
            R.id.assignRolesFragment,
            R.id.leaderboardFragment
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the toolbar as the action bar
        setSupportActionBar(binding.toolbar)

        // Setup navigation controller
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup bottom navigation
        bottomNav = binding.bottomNavView

        // Setup action bar with nav controller
        setupActionBarWithNavController(navController, appBarConfiguration)

        if (preferenceManager.isLoggedIn) {
            val role = preferenceManager.getUserRole() ?: UserRole.MEMBER
            setupBottomNavForUser(role)
        } else {
            setupAuthNavigation()
        }

        // Setup navigation listener
        setupNavigationListener()
    }

    // Always show status bar menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.status_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reports -> {
                // Navigate to reports
                if (navController.currentDestination?.id != R.id.reportsFragment) {
                    navController.navigate(R.id.reportsFragment)
                }
                true
            }

            R.id.action_settings -> {
                // Navigate to settings
                if (navController.currentDestination?.id != R.id.settingsFragment) {
                    navController.navigate(R.id.settingsFragment)
                }
                true
            }

            R.id.action_notifications -> {
                // Navigate to member approval
                if (navController.currentDestination?.id != R.id.memberApprovalFragment) {
                    navController.navigate(R.id.memberApprovalFragment)
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupAuthNavigation() {
        bottomNav.visibility = View.GONE
        val navGraph = navController.navInflater.inflate(R.navigation.auth_nav_graph)
        navGraph.setStartDestination(R.id.loginFragment)
        navController.graph = navGraph

        // Clear back stack to prevent going back to login after successful login
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment) {
                // Clear the back stack by popping everything except the current destination
                while (navController.currentDestination?.id != navController.graph.startDestinationId) {
                    navController.popBackStack()
                }
            }
        }
    }

    private fun setupBottomNavForUser(role: UserRole) {
        // Setup bottom navigation based on user role
        val (navGraph, menuRes) = when (role) {
            UserRole.VP_EDUCATION -> R.navigation.vp_nav_graph to R.menu.vp_bottom_nav_menu
            UserRole.MEMBER -> R.navigation.member_nav_graph to R.menu.member_bottom_nav_menu
            else -> R.navigation.vp_nav_graph to R.menu.vp_bottom_nav_menu // Default to VP navigation
        }

        // Inflate the navigation graph
        val navGraphObj = navController.navInflater.inflate(navGraph)

        // Set the navigation graph
        navController.graph = navGraphObj

        // Set the appropriate menu for bottom navigation
        bottomNav.menu.clear()
        bottomNav.inflateMenu(menuRes)

        // Setup bottom navigation with nav controller
        bottomNav.setupWithNavController(navController)

        // Show bottom navigation
        bottomNav.visibility = View.VISIBLE

        // Handle destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Hide bottom nav for auth screens
            when (destination.id) {
                R.id.loginFragment, R.id.signUpFragment -> bottomNav.visibility = View.GONE
                else -> bottomNav.visibility = View.VISIBLE
            }
        }
    }

    private fun setupNavigationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Handle back button visibility
            val isTopLevelDestination =
                appBarConfiguration.topLevelDestinations.contains(destination.id)
            supportActionBar?.let { actionBar ->
                actionBar.setDisplayHomeAsUpEnabled(!isTopLevelDestination)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    fun navigateToRoleBasedScreen(role: UserRole) {
        setupBottomNavForUser(role)
    }
}