package com.bntsoft.toastmasters

import android.os.Bundle
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bntsoft.toastmasters.databinding.ActivityMemberMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MemberActivity : BaseActivity() {
    private lateinit var binding: ActivityMemberMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.title_dashboard)

        setupNavigation()
    }

    private fun setupNavigation() {
        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                ?: throw IllegalStateException("NavHostFragment not found")

            val navController = navHostFragment.navController

            val navGraph = navController.navInflater.inflate(R.navigation.member_nav_graph)
            navGraph.setStartDestination(R.id.navigation_dashboard)
            navController.graph = navGraph

            binding.bottomNavView.setupWithNavController(navController)

            navController.addOnDestinationChangedListener { _, destination, _ ->
                supportActionBar?.title = when (destination.id) {
                    R.id.navigation_dashboard -> getString(R.string.title_dashboard)
                    R.id.navigation_meetings -> getString(R.string.title_upcoming_meetings)
                    R.id.navigation_leaderboard -> getString(R.string.title_leaderboard)
                    R.id.navigation_settings -> getString(R.string.title_settings)
                    else -> getString(R.string.app_name)

                }

            }
        } catch (e: Exception) {
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


}
