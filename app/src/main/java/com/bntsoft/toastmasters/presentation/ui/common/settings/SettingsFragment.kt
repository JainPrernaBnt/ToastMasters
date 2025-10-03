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
import androidx.navigation.fragment.findNavController
import com.bntsoft.toastmasters.MainActivity
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentSettingsBinding
import com.bntsoft.toastmasters.utils.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.bumptech.glide.Glide
import com.bntsoft.toastmasters.utils.GlideExtensions
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
        // Listen for updates from ProfileEditFragment
        val navController = requireActivity().findNavController(R.id.nav_host_fragment)
        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>("profileUpdated")
            ?.observe(viewLifecycleOwner) { updated ->
                if (updated == true) {
                    android.util.Log.d("SettingsFragment", "Profile updated flag received, reloading user data")
                    viewModel.loadUserData()
                    navController.currentBackStackEntry?.savedStateHandle?.set("profileUpdated", false)
                }
            }

    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("SettingsFragment", "onResume called, refreshing user data")
        viewModel.loadUserData()
    }

    private fun observeUserData() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is SettingsUiState -> {
                        if (state.navigateToLogin) {
                            // Stop notification listener service
                            try {
                                val notificationServiceIntent = android.content.Intent(requireContext(), com.bntsoft.toastmasters.service.NotificationListenerService::class.java)
                                requireContext().stopService(notificationServiceIntent)
                            } catch (e: Exception) {
                                // Ignore errors when stopping service
                            }
                            
                            navigateToLogin()
                            viewModel.onNavigationComplete()
                        }
                        
                        state.user?.let { user ->
                            binding.user = user
                            updateUserDetails(user)
                            loadProfilePicture(user)
                        }
                        
                        state.error?.let { error ->
                            showError(error)
                        }
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
            
            // Update mentors section for MEMBER role
            updateMentorsSection(user)
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

        binding.btnClubMembers.setOnClickListener {
            navigateToClubMembers()
        }

        binding.btnEditProfile.setOnClickListener {
            navigateToEditProfile()
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

    private fun navigateToClubMembers() {
        try {
            val navController = requireActivity().findNavController(R.id.nav_host_fragment)
            navController.navigate(R.id.action_settingsFragment_to_clubMembersFragment)
        } catch (e: Exception) {
            showError("Navigation failed: ${e.message}")
        }
    }

    private fun navigateToEditProfile() {
        try {
            val navController = requireActivity().findNavController(R.id.nav_host_fragment)
            navController.navigate(R.id.action_settingsFragment_to_profileEditFragment)
        } catch (e: Exception) {
            showError("Navigation failed: ${e.message}")
        }
    }

    private fun updateMentorsSection(user: com.bntsoft.toastmasters.domain.model.User) {
        val mentorsContainer = binding.mentorsContainer
        val noMentorsText = binding.tvNoMentors
        
        // Clear existing mentor views
        mentorsContainer.removeAllViews()
        
        if (user.mentorNames.isEmpty()) {
            noMentorsText.visibility = View.VISIBLE
        } else {
            noMentorsText.visibility = View.GONE
            
            // Add mentor items
            user.mentorNames.forEach { mentorName ->
                val mentorView = createMentorItemView(mentorName)
                mentorsContainer.addView(mentorView)
            }
        }
    }

    private fun createMentorItemView(mentorName: String): View {
        val mentorView = LayoutInflater.from(requireContext()).inflate(
            android.R.layout.simple_list_item_1, 
            binding.mentorsContainer, 
            false
        )
        
        val textView = mentorView.findViewById<android.widget.TextView>(android.R.id.text1)
        textView.text = mentorName
        textView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
        textView.setTextColor(resources.getColor(android.R.color.primary_text_light, null))
        textView.setPadding(0, 8, 0, 8)
        
        // Add mentor icon
        textView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_mentor, 0, 0, 0
        )
        textView.compoundDrawablePadding = 16
        
        return mentorView
    }


    private fun loadProfilePicture(user: com.bntsoft.toastmasters.domain.model.User) {
        android.util.Log.d("SettingsFragment", "Loading profile picture for user: ${user.name}, profilePictureUrl: ${if (user.profilePictureUrl.isNullOrEmpty()) "null/empty" else "has data"}")
        GlideExtensions.loadProfilePicture(
            binding.profileImageView,
            user.profilePictureUrl,
            R.drawable.ic_person
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}