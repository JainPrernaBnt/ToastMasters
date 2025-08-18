package com.bntsoft.toastmasters

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavDestination
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
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup navigation controller
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup bottom navigation
        bottomNav = binding.bottomNavView
        
        // Setup app bar with navigation controller
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.meetingsFragment,
                R.id.membersFragment,
                R.id.profileFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Check if user is already logged in
        if (preferenceManager.isLoggedIn) {
            val userRole = preferenceManager.userRole?.let { UserRole.valueOf(it) } ?: UserRole.VP_EDUCATION
            setupBottomNavForUser(userRole)
        } else {
            setupAuthNavigation()
        }
        
        // Setup navigation listener
        setupNavigationListener()
    }
    
    private fun setupAuthNavigation() {
        bottomNav.visibility = View.GONE
        val navGraph = navController.navInflater.inflate(R.navigation.auth_nav_graph)
        navGraph.setStartDestination(R.id.loginFragment)
        navController.graph = navGraph
        
        // Clear back stack to prevent going back to login after successful login
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment) {
                navController.backQueue.clear()
            }
        }
    }
    
    private fun setupBottomNavForUser(role: UserRole) {
        // Setup bottom navigation based on user role
        val navGraph = when (role) {
            UserRole.VP_EDUCATION -> R.navigation.vp_nav_graph
            else -> R.navigation.vp_nav_graph // Default to VP navigation
        }
        
        // Inflate the navigation graph
        val navGraphObj = navController.navInflater.inflate(navGraph)
        
        // Set the navigation graph
        navController.graph = navGraphObj
        
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
            
            // Update action bar title
            supportActionBar?.title = when (destination.id) {
                R.id.homeFragment -> getString(R.string.title_home)
                R.id.meetingsFragment -> getString(R.string.title_meetings)
                R.id.membersFragment -> getString(R.string.title_members)
                R.id.profileFragment -> getString(R.string.title_profile)
                else -> getString(R.string.app_name)
            }
        }
    }
    
    private fun setupNavigationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Handle back button visibility
            when (destination.id) {
                R.id.loginFragment, R.id.signUpFragment -> {
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
                else -> {
                    supportActionBar?.setDisplayHomeAsUpEnabled(
                        !appBarConfiguration.topLevelDestinations.contains(destination.id)
                    )
                }
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    
    private fun navigateToRoleBasedScreen(role: UserRole) {
        setupBottomNavForUser(role)
    }
}