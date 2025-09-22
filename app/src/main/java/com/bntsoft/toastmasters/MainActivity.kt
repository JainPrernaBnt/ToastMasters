package com.bntsoft.toastmasters

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bntsoft.toastmasters.databinding.ActivityMainBinding
import com.bntsoft.toastmasters.domain.models.UserRole
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
            R.id.createMeetingFragment,
            R.id.settingsFragment,
            R.id.leaderboardFragment,
            R.id.reportsFragment,
            R.id.memberApprovalFragment,
            R.id.loginFragment,
            R.id.signUpFragment,
            R.id.assignRolesFragment,
            R.id.memberRoleAssignFragment
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)

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
            // Navigate to the appropriate activity based on role
            val intent = if (role == UserRole.VP_EDUCATION) {
                android.content.Intent(this, VpMainActivity::class.java)
            } else {
                android.content.Intent(this, MemberActivity::class.java)
            }
            startActivity(intent)
            finish()
        } else {
            setupAuthNavigation()
        }

        // Setup navigation listener
        setupNavigationListener()
    }

    // Only show status bar menu for VP Education users
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val currentRole = preferenceManager.getUserRole()
        if (currentRole == UserRole.VP_EDUCATION) {
            menuInflater.inflate(R.menu.status_bar_menu, menu)
            return true
        }
        return false
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

    fun setupAuthNavigation() {
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

    private fun setupNavigationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // List of auth fragments where we want to hide the toolbar and bottom nav
            val isAuth = listOf(R.id.loginFragment, R.id.signUpFragment).contains(destination.id)

            if (isAuth) {
                supportActionBar?.hide()
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
                bottomNav.visibility = View.GONE
            } else {
                supportActionBar?.show()
                window.decorView.systemUiVisibility = 0
                // Show bottom nav for all non-auth destinations except when in CreateAgendaFragment or AgendaTableFragment
                val shouldHideBottomNav = when (destination.id) {
                    R.id.createAgendaFragment -> true
                    R.id.agendaTableFragment -> true
                    else -> false
                } || destination.label.toString().contains("Agenda", ignoreCase = true)

                // Debug logging
                Log.d(
                    "MainActivity",
                    "Destination ID: ${destination.id}, Label: ${destination.label}, Should hide bottom nav: $shouldHideBottomNav"
                )

                bottomNav.visibility = if (shouldHideBottomNav) View.GONE else View.VISIBLE

            }

            // Handle back button visibility - show back button for all non-top-level destinations
            // except for auth screens where we hide the action bar completely
            val isTopLevelDestination =
                appBarConfiguration.topLevelDestinations.contains(destination.id)
            supportActionBar?.let { actionBar ->
                // Show back button for all fragments except auth fragments and top-level destinations
                val shouldShowBackButton = !isAuth && !isTopLevelDestination
                actionBar.setDisplayHomeAsUpEnabled(shouldShowBackButton)

                // Set the title based on the current destination
                actionBar.title = when (destination.id) {
                    R.id.dashboardFragment -> getString(R.string.title_dashboard)
                    R.id.createAgendaFragment -> getString(R.string.create_agenda)
                    R.id.createMeetingFragment -> getString(R.string.meetings)
                    R.id.settingsFragment -> getString(R.string.settings)
                    R.id.leaderboardFragment -> getString(R.string.leaderboard)
                    R.id.reportsFragment -> getString(R.string.title_reports)
                    R.id.memberApprovalFragment -> getString(R.string.approve_member)
                    R.id.assignRolesFragment -> getString(R.string.title_assign_roles)
                    R.id.memberRoleAssignFragment -> getString(R.string.title_assign_roles)
                    // Add more titles later
                    else -> getString(R.string.app_name)
                }
            }

            // Invalidate options menu to update status bar menu visibility
            invalidateOptionsMenu()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    fun navigateToRoleBasedScreen(role: UserRole) {
        // This method is no longer needed as we're using separate activities for each role
        // The navigation is now handled in the LoginFragment
    }

    private fun updateBottomNavVisibility() {
        val currentDestination = navController.currentDestination?.id ?: return
        val isAuthScreen = currentDestination == R.id.loginFragment ||
                currentDestination == R.id.signUpFragment

        val bottomNavHide = currentDestination == R.id.editMeetingFragment
        // Hide bottom nav for auth screens
        bottomNav.visibility = if (isAuthScreen && bottomNavHide) View.GONE else View.VISIBLE

        // Update system UI flags
        if (isAuthScreen) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        } else {
            window.decorView.systemUiVisibility = 0
        }
    }
}