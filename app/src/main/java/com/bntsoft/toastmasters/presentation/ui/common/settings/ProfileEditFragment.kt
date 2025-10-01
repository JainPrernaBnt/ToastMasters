package com.bntsoft.toastmasters.presentation.ui.common.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentProfileEditBinding
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.models.UserRole
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.bntsoft.toastmasters.utils.GlideExtensions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ProfileEditFragment : Fragment() {
    private var _binding: FragmentProfileEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileEditViewModel by viewModels()
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateProfilePicture(it)
            loadProfileImage(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupRoleSpinner()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        binding.btnCancel.setOnClickListener {
            showDiscardChangesDialog()
        }

        binding.profileImageView.setOnClickListener {
            openImagePicker()
        }
    }

    private fun setupRoleSpinner() {
        val roles = arrayOf("Member", "VP Education")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRole.adapter = adapter
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.contentLayout.visibility = if (state.isLoading) View.GONE else View.VISIBLE
                
                state.user?.let { user ->
                    populateFields(user)
                    loadCurrentProfilePicture(user)
                }
                
                if (state.saveSuccess) {
                    showSuccess("Profile updated successfully")
                    requireActivity().findNavController(R.id.nav_host_fragment).navigateUp()
                }
                
                state.error?.let { error ->
                    showError(error)
                    viewModel.clearError()
                }
            }
        }
    }

    private fun populateFields(user: User) {
        binding.apply {
            etName.setText(user.name)
            etEmail.setText(user.email)
            etPhone.setText(user.phoneNumber)
            etAddress.setText(user.address)
            etToastmastersId.setText(user.toastmastersId)
            
            when (user.gender.lowercase()) {
                "male" -> rgGender.check(R.id.rbMale)
                "female" -> rgGender.check(R.id.rbFemale)
                else -> rgGender.clearCheck()
            }
            
            etLevel.setText(user.level ?: "")
            
            val mentorNames = user.mentorNames.joinToString(", ")
            etMentorNames.setText(mentorNames)
            
            // Set role spinner
            val rolePosition = when (user.role) {
                UserRole.VP_EDUCATION -> 1
                UserRole.MEMBER -> 0
            }
            spinnerRole.setSelection(rolePosition)
        }
    }

    private fun setupRoleVisibility(user: User) {
        // Only VP_EDUCATION can edit roles
        val canEditRole = user.role == UserRole.VP_EDUCATION
        binding.roleContainer.visibility = if (canEditRole) View.VISIBLE else View.GONE
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val toastmastersId = binding.etToastmastersId.text.toString().trim()
        val level = binding.etLevel.text.toString().trim()
        val mentorNamesText = binding.etMentorNames.text.toString().trim()
        
        val gender = when (binding.rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Male"
            R.id.rbFemale -> "Female"
            else -> ""
        }
        
        val mentorNames = if (mentorNamesText.isNotEmpty()) {
            mentorNamesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        val selectedRole = when (binding.spinnerRole.selectedItemPosition) {
            1 -> UserRole.VP_EDUCATION
            else -> UserRole.MEMBER
        }

        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return
        }

        viewModel.updateProfile(
            name = name,
            email = email,
            phoneNumber = phone,
            address = address,
            toastmastersId = toastmastersId,
            gender = gender,
            level = level.ifEmpty { null },
            mentorNames = mentorNames,
            role = selectedRole
        )
    }

    private fun showDiscardChangesDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Discard Changes?")
            .setMessage("Are you sure you want to discard your changes?")
            .setPositiveButton("Discard") { _, _ ->
                requireActivity().findNavController(R.id.nav_host_fragment).navigateUp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun loadProfileImage(uri: Uri) {
        GlideExtensions.loadProfilePicture(
            binding.profileImageView,
            uri.toString(),
            R.drawable.ic_person
        )
    }

    private fun loadCurrentProfilePicture(user: User) {
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
