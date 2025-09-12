package com.bntsoft.toastmasters.presentation.ui.common.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.bntsoft.toastmasters.MainActivity
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentSettingsBinding
import com.bntsoft.toastmasters.utils.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeUserData()
    }

    private fun observeUserData() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                when {
                    state.isLoading -> {
                        // Show loading state if needed
                    }

                    state.navigateToLogin -> {
                        // Navigate to login screen
                        navigateToLogin()
                        viewModel.onNavigationComplete()
                    }

                    state.user != null -> {
                        binding.user = state.user
                        updateUserDetails(state.user)
                    }

                    !state.error.isNullOrEmpty() -> {
                        showError(state.error)
                    }
                }
            }
        }
    }

    private fun updateUserDetails(user: com.bntsoft.toastmasters.domain.model.User) {
        with(binding) {
            // Format the joined date
            val formattedDate = try {
                dateFormat.format(user.joinedDate)
            } catch (e: Exception) {
                user.joinedDate.toString()
            }

            val formattedRole = when (user.role.name) {
                "VP_EDUCATION" -> "VP Education"
                "MEMBER" -> "Member"
                else -> user.role.name.replace(
                    "_",
                    " "
                ) // fallback: convert ENUM_NAME to readable text
            }

            // Update user details section with proper formatting
            tvPhoneNumber.text = user.phoneNumber.ifEmpty { getString(R.string.not_provided) }
            tvAddress.text = user.address.ifEmpty { getString(R.string.not_provided) }

            tvToastmastersId.text = getString(
                R.string.tm_id_with_value,
                user.toastmastersId.ifEmpty { getString(R.string.not_provided) }
            )
            tvClubId.text = getString(
                R.string.club_id_with_value,
                user.clubId.ifEmpty { getString(R.string.not_provided) }
            )

            tvMemberSince.text = formattedDate
            tvUserRole.text = formattedRole
            tvLevel.text = user.level?.ifEmpty { getString(R.string.not_provided) }
        }
    }


    private fun showError(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnEditProfile.setOnClickListener {
            // TODO: Implement edit profile
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirmation)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.onLogout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun navigateToLogin() {
        try {
            // Get the activity's nav controller
            val navController = requireActivity().findNavController(R.id.nav_host_fragment)
            
            // Create a new NavOptions to clear the back stack
            val navOptions = NavOptions.Builder()
                .setPopUpTo(navController.graph.startDestinationId, true)
                .build()
                
            // Navigate to login fragment with cleared back stack
            navController.navigate(
                R.id.loginFragment,
                null,
                navOptions
            )
            
            // Reset the navigation graph to auth flow
            (requireActivity() as? MainActivity)?.setupAuthNavigation()
            
        } catch (e: Exception) {
            // If navigation fails, restart the app to the login screen
            val intent = requireContext().packageManager.getLaunchIntentForPackage(requireContext().packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (intent != null) {
                startActivity(intent)
            }
            activity?.finish()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}