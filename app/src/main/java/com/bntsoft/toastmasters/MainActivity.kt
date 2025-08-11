package com.bntsoft.toastmasters

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.bntsoft.toastmasters.data.model.UserRole
import com.bntsoft.toastmasters.databinding.ActivityMainBinding
import com.bntsoft.toastmasters.presentation.viewmodel.MainViewModel
import com.bntsoft.toastmasters.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    
    @Inject
    lateinit var preferenceManager: PreferenceManager
    
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if user is already logged in
        checkUserRoleAndNavigate()
    }
    
    private fun checkUserRoleAndNavigate() {
        navigateToRoleBasedScreen(UserRole.VP_EDUCATION)
    }
    
    private fun navigateToRoleBasedScreen(role: UserRole) {

        val intent = when (role) {
            UserRole.VP_EDUCATION -> {
                Intent(this, VpMainActivity::class.java)
            }
            else -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra("SHOW_ERROR", "Unsupported role: $role")
                }
            }
        }
        
        startActivity(intent)
        finish()
    }
}