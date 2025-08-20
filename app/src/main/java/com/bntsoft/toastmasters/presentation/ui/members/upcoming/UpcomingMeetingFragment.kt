package com.bntsoft.toastmasters.presentation.ui.members.upcoming

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.DialogMeetingResponseBinding
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.utils.DateTimeUtils
import com.bntsoft.toastmasters.utils.UiUtils
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UpcomingMeetingFragment : DialogFragment() {
    
    private var _binding: DialogMeetingResponseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UpcomingMeetingViewModel by viewModels()

    private var meetingId: String = ""
    private var onResponseSubmitted: (() -> Unit)? = null
    private var loadingDialog: androidx.appcompat.app.AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
        meetingId = arguments?.getString("meetingId") ?: run {
            dismiss()
            return
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMeetingResponseBinding.inflate(inflater, container, false)
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return binding.root
    }

    private var isDialogShown = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState?.getBoolean(KEY_DIALOG_SHOWN, false) == true) {
            // If we're recreating after a configuration change and dialog was shown, dismiss
            dismiss()
            return
        }

        setupUI()
        setupClickListeners()
        observeViewModel()
        
        // Load the meeting data
        if (meetingId.isNotBlank()) {
            viewModel.loadMeetingAndResponse()
        } else {
            showError("No meeting ID provided")
            safeDismiss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_DIALOG_SHOWN, isDialogShown)
    }

    private fun safeDismiss() {
        if (isAdded && !isRemoving) {
            dismiss()
        }
    }

    private fun setupUI() {
        // Set dialog properties
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.setCanceledOnTouchOutside(false)
    }

    private fun setupClickListeners() {
        // Availability radio buttons
        binding.rgAvailability.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbAvailable -> viewModel.updateAvailability(MemberResponse.AvailabilityStatus.AVAILABLE)
                R.id.rbNotAvailable -> viewModel.updateAvailability(MemberResponse.AvailabilityStatus.NOT_AVAILABLE)
                R.id.rbNotConfirmed -> viewModel.updateAvailability(MemberResponse.AvailabilityStatus.NOT_CONFIRMED)
            }
        }

        // Submit button
        binding.btnSubmit.setOnClickListener {
            showSubmitConfirmation()
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            if (hasUnsavedChanges()) {
                showDiscardChangesDialog()
            } else {
                dismiss()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe UI state first
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is UpcomingMeetingViewModel.MemberResponseUiState.Loading -> {
                        showLoading(true)
                    }
                    is UpcomingMeetingViewModel.MemberResponseUiState.Saving -> {
                        showLoading(true, getString(R.string.saving_changes))
                    }
                    is UpcomingMeetingViewModel.MemberResponseUiState.Success -> {
                        showLoading(false)
                        onResponseSubmitted?.invoke()
                    }
                    is UpcomingMeetingViewModel.MemberResponseUiState.Error -> {
                        showLoading(false)
                        showError(state.message)
                        if (state.message.contains("Failed to load meeting") || 
                            state.message.contains("Meeting not found")) {
                            dismiss()
                        }
                    }
                }
            }
        }

        // Observe meeting details
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.meeting.collect { meeting ->
                meeting?.let { 
                    updateMeetingDetails(it)
                    // Load available roles from the meeting
                    updateAvailableRoles(it.availableRoles)
                }
            }
        }

        // Observe current response
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentResponse.collect { response ->
                response?.let { updateResponseUI(it) }
            }
        }

        // Observe available roles
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.availableRoles.collect { roles ->
                updateAvailableRoles(roles)
            }
        }
    }

    private fun updateMeetingDetails(meeting: com.bntsoft.toastmasters.domain.model.Meeting) {
        // Format date and time
        val dateTime = meeting.dateTime
        val endDateTime = meeting.endDateTime ?: dateTime.plusHours(2) // Default 2-hour duration if end time not specified
        
        // Convert LocalDateTime to Date for the formatter
        val startDate = java.util.Date(
            dateTime.year - 1900, 
            dateTime.monthValue - 1, 
            dateTime.dayOfMonth,
            dateTime.hour,
            dateTime.minute
        )
        
        val endDate = java.util.Date(
            endDateTime.year - 1900,
            endDateTime.monthValue - 1,
            endDateTime.dayOfMonth,
            endDateTime.hour,
            endDateTime.minute
        )
        
        binding.tvMeetingDateTime.text = "${DateTimeUtils.formatDate(startDate.time)} • ${DateTimeUtils.formatTime(startDate.time)} - ${DateTimeUtils.formatTime(endDate.time)}"
        binding.tvMeetingLocation.text = meeting.location
        
        // Update available roles - using an empty list for now since roles aren't part of the Meeting model
        updateAvailableRoles(emptyList())
    }

    private fun updateResponseUI(response: MemberResponse) {
        // Update availability radio buttons
        when (response.availability) {
            MemberResponse.AvailabilityStatus.AVAILABLE -> binding.rbAvailable.isChecked = true
            MemberResponse.AvailabilityStatus.NOT_AVAILABLE -> binding.rbNotAvailable.isChecked = true
            MemberResponse.AvailabilityStatus.NOT_CONFIRMED -> binding.rbNotConfirmed.isChecked = true
        }

        // Update notes
        binding.etNotes.setText(response.notes)
        
        // Update selected roles
        response.preferredRoles.forEach { roleId ->
            binding.cgRoles.findViewWithTag<Chip>(roleId)?.isChecked = true
        }
    }

    private fun updateAvailableRoles(roles: List<String>) {
        if (roles.isEmpty()) {
            binding.tvNoRoles.isVisible = true
            binding.cgRoles.isVisible = false
            return
        }
        
        binding.tvNoRoles.isVisible = false
        binding.cgRoles.isVisible = true
        
        // Only update if the roles have changed to avoid UI flicker
        val currentRoles = (0 until binding.cgRoles.childCount)
            .map { binding.cgRoles.getChildAt(it) as? Chip }
            .filterNotNull()
            .map { it.text.toString() }
            
        if (currentRoles == roles) return
        
        binding.cgRoles.removeAllViews()
        
        roles.forEach { role ->
            val chip = Chip(requireContext()).apply {
                text = role
                tag = role
                isCheckable = true
                isClickable = true
                isChecked = viewModel.currentResponse.value?.preferredRoles?.contains(role) == true
                setOnCheckedChangeListener { _, isChecked ->
                    viewModel.toggleRole(role)
                }
                setChipBackgroundColorResource(R.drawable.chip_background_selector)
                setTextColor(androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector))
                chipStrokeColor = androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.chip_stroke_selector)
                chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width)
                chipCornerRadius = resources.getDimension(R.dimen.chip_corner_radius)
            }
            binding.cgRoles.addView(chip)
        }
    }

    private fun showSubmitConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_submission)
            .setMessage(R.string.are_you_sure_you_want_to_submit)
            .setPositiveButton(R.string.submit) { _, _ ->
                viewModel.submitResponse()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDiscardChangesDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.discard_changes)
            .setMessage(R.string.are_you_sure_you_want_to_discard_changes)
            .setPositiveButton(R.string.discard) { _, _ ->
                dismiss()
            }
            .setNegativeButton(R.string.keep_editing, null)
            .show()
    }

    private fun hasUnsavedChanges(): Boolean {
        val currentResponse = viewModel.currentResponse.value ?: return false
        return currentResponse.availability != MemberResponse.AvailabilityStatus.NOT_CONFIRMED ||
                currentResponse.preferredRoles.isNotEmpty() ||
                currentResponse.notes.isNotBlank()
    }

    private fun showLoading(show: Boolean, message: String? = null) {
        if (show) {
            showLoading(message ?: getString(R.string.loading))
        } else {
            hideLoading()
        }
    }

    private fun showLoading(message: String) {
        if (loadingDialog == null) {
            loadingDialog = MaterialAlertDialogBuilder(requireContext())
                .setMessage(message)
                .setCancelable(false)
                .create()
        }
        loadingDialog?.show()
        
        // Disable UI
        binding.btnSubmit.isEnabled = false
        binding.btnCancel.isEnabled = false
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
        
        // Enable UI
        binding.btnSubmit.isEnabled = true
        binding.btnCancel.isEnabled = true
    }

    private fun showError(message: String) {
        if (isAdded) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            safeDismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_DIALOG_SHOWN = "dialog_shown"
    }
}
