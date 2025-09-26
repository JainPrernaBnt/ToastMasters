package com.bntsoft.toastmasters.presentation.ui.vp.clubmembers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentMemberEditBinding
import com.bntsoft.toastmasters.domain.model.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class MemberEditFragment : Fragment() {
    private var _binding: FragmentMemberEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MemberEditViewModel by viewModels()
    private val args: MemberEditFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadMember(args.memberId)
        setupClickListeners()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveMemberDetails()
        }

        binding.btnCancel.setOnClickListener {
            showDiscardChangesDialog()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.contentLayout.visibility = if (state.isLoading) View.GONE else View.VISIBLE
                
                state.member?.let { member ->
                    populateFields(member)
                }
                
                if (state.isSaving) {
                    binding.btnSave.isEnabled = false
                    binding.btnSave.text = getString(R.string.saving)
                } else {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.save_changes)
                }
                
                if (state.saveSuccess) {
                    showSuccess("Member details updated successfully")
                    requireActivity().findNavController(R.id.nav_host_fragment).navigateUp()
                }
                
                state.error?.let { error ->
                    showError(error)
                    viewModel.clearError()
                }
            }
        }
    }

    private fun populateFields(member: User) {
        binding.apply {
            etName.setText(member.name)
            etEmail.setText(member.email)
            etPhone.setText(member.phoneNumber)
            etAddress.setText(member.address)
            etToastmastersId.setText(member.toastmastersId)
            
            when (member.gender.lowercase()) {
                "male" -> rgGender.check(R.id.rbMale)
                "female" -> rgGender.check(R.id.rbFemale)
                else -> rgGender.clearCheck()
            }
            
            etLevel.setText(member.level ?: "")
            
            val mentorNames = member.mentorNames.joinToString(", ")
            etMentorNames.setText(mentorNames)
        }
    }

    private fun saveMemberDetails() {
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

        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return
        }

        viewModel.updateMember(
            name = name,
            email = email,
            phoneNumber = phone,
            address = address,
            toastmastersId = toastmastersId,
            gender = gender,
            level = level.ifEmpty { null },
            mentorNames = mentorNames
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
