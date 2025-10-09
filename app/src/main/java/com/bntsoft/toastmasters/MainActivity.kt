package com.bntsoft.toastmasters

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bntsoft.toastmasters.databinding.ActivityMainBinding
import com.bntsoft.toastmasters.domain.models.UserRole
import com.bntsoft.toastmasters.utils.NotificationPermissionManager
import com.bntsoft.toastmasters.utils.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var notificationPermissionManager: NotificationPermissionManager

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handleNotificationPermissionResult(isGranted)
    }

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

        bottomNav = binding.bottomNavView

        // Setup action bar with nav controller
        setupActionBarWithNavController(navController, appBarConfiguration)

        checkAuthenticationState()

        // Setup navigation listener
        setupNavigationListener()
    }

    private fun checkAuthenticationState() {
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val isLoggedInPrefs = preferenceManager.isLoggedIn
        val userRole = preferenceManager.getUserRole()
        val userId = preferenceManager.userId
        val userEmail = preferenceManager.userEmail

        Log.d("MainActivity", "Auth check - Firebase user: ${firebaseUser != null} (${firebaseUser?.uid}), Prefs logged in: $isLoggedInPrefs, Role: $userRole, UserId: $userId, Email: $userEmail")

        when {
            // Both Firebase and preferences indicate user is logged in
            firebaseUser != null && isLoggedInPrefs && userRole != null -> {
                Log.d("MainActivity", "User is authenticated, navigating to role-based activity")
                navigateToRoleBasedActivity(userRole)
            }
            
            // Firebase user exists but preferences are incomplete - try to restore from Firebase
            firebaseUser != null && (!isLoggedInPrefs || userRole == null) -> {
                Log.d("MainActivity", "Firebase user exists but preferences incomplete - attempting to restore user data")
                restoreUserDataFromFirebase(firebaseUser)
            }
            
            // Preferences indicate logged in but no Firebase user (shouldn't happen normally)
            firebaseUser == null && isLoggedInPrefs -> {
                Log.d("MainActivity", "Preferences indicate logged in but no Firebase user - clearing preferences")
                preferenceManager.clearUserData()
                setupAuthNavigation()
            }
            
            // Neither Firebase nor preferences indicate logged in
            else -> {
                Log.d("MainActivity", "User not authenticated, showing login")
                setupAuthNavigation()
            }
        }
    }
    
    private fun restoreUserDataFromFirebase(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        // Try to restore user data from Firebase
        lifecycleScope.launch {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                
                if (userDoc.exists()) {
                    val isVpEducation = userDoc.getBoolean("isVpEducation") ?: false
                    val isApproved = userDoc.getBoolean("isApproved") ?: false
                    val userName = userDoc.getString("name") ?: ""
                    val userEmail = userDoc.getString("email") ?: firebaseUser.email ?: ""
                    
                    // Determine user role
                    val role = if (isVpEducation) {
                        com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION
                    } else {
                        com.bntsoft.toastmasters.domain.models.UserRole.MEMBER
                    }
                    
                    // Check if user is approved (or is VP Education)
                    if (isVpEducation || isApproved) {
                        Log.d("MainActivity", "Restored user data from Firebase - Role: $role, Approved: $isApproved")
                        
                        // Restore preferences
                        preferenceManager.userId = firebaseUser.uid
                        preferenceManager.userEmail = userEmail
                        preferenceManager.userName = userName
                        preferenceManager.saveUserRole(role)
                        preferenceManager.isLoggedIn = true
                        
                        // Navigate to appropriate activity
                        navigateToRoleBasedActivity(role)
                    } else {
                        Log.d("MainActivity", "User not approved, signing out")
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        setupAuthNavigation()
                    }
                } else {
                    Log.d("MainActivity", "User document not found in Firestore, signing out")
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    setupAuthNavigation()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error restoring user data from Firebase", e)
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                setupAuthNavigation()
            }
        }
    }

    private fun navigateToRoleBasedActivity(role: UserRole) {
        val intent = if (role == UserRole.VP_EDUCATION) {
            android.content.Intent(this, VpMainActivity::class.java)
        } else {
            android.content.Intent(this, MemberActivity::class.java)
        }
        startActivity(intent)
        finish()
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
//            R.id.action_reports -> {
//                // Navigate to reports
//                if (navController.currentDestination?.id != R.id.reportsFragment) {
//                    navController.navigate(R.id.reportsFragment)
//                }
//                true
//            }

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

    fun requestNotificationPermissionForFreshInstall() {
        if (notificationPermissionManager.shouldRequestPermission(this)) {
            notificationPermissionManager.requestNotificationPermission(this) { isGranted ->
                handleNotificationPermissionResult(isGranted)
            }
        }
    }

    private fun handleNotificationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")

            val userRole = preferenceManager.getUserRole()?.name ?: "member"

            if (notificationPermissionManager.hasReachedMaxDenials()) {
                notificationPermissionManager.showPermissionExplanationDialog(
                    context = this,
                    userRole = userRole
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