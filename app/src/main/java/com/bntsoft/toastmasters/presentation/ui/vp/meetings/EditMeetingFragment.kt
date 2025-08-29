package com.bntsoft.toastmasters.presentation.ui.vp.meetings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentEditMeetingBinding
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.viewmodel.EditMeetingViewModel
import com.bntsoft.toastmasters.utils.UiUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EditMeetingFragment : Fragment() {

    private var _binding: FragmentEditMeetingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditMeetingViewModel by viewModels()
    private var meetingId: String = 0.toString()
    private var meeting: Meeting? = null

    // Editable preferred roles for this screen
    private val preferredRoles: MutableList<String> = mutableListOf()

    private val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditMeetingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get meeting ID from nav arguments (string id from Firestore)
        meetingId = arguments?.getString("meeting_id") ?: (arguments?.getInt(ARG_MEETING_ID)
            ?: 0).toString()

        setupClickListeners()
        observeViewModel()

        // Load meeting details
        viewModel.loadMeeting(meetingId)
    }

    // Toolbar removed; Save handled by bottom button

    private fun setupClickListeners() {
        // Date Picker
        binding.dateInputLayout.setEndIconOnClickListener {
            showDatePicker()
        }

        binding.dateEditText.setOnClickListener {
            showDatePicker()
        }

        // Time pickers
        binding.startTimeInputLayout.setEndIconOnClickListener {
            showTimePicker(isStartTime = true)
        }
        binding.endTimeInputLayout.setEndIconOnClickListener {
            showTimePicker(isStartTime = false)
        }
        binding.startTimeEditText.setOnClickListener {
            showTimePicker(isStartTime = true)
        }
        binding.endTimeEditText.setOnClickListener {
            showTimePicker(isStartTime = false)
        }

        // Save button
        binding.btnSave.setOnClickListener {
            if (validateForm()) {
                updateMeeting()
            }
        }

        // Add role button
        binding.btnAddRole.setOnClickListener {
            val role = binding.roleEditText.text?.toString()?.trim().orEmpty()
            if (role.isEmpty()) {
                binding.roleInputLayout.error = getString(R.string.error_required)
                return@setOnClickListener
            }
            binding.roleInputLayout.error = null

            // Avoid duplicates (case-insensitive)
            val exists = preferredRoles.any { it.equals(role, ignoreCase = true) }
            if (!exists) {
                preferredRoles.add(role)
                addRoleChip(role)
            } else {
                binding.roleInputLayout.error = getString(R.string.error_duplicate)
            }
            binding.roleEditText.setText("")
        }
    }

    private fun renderPreferredRoleChips() {
        binding.roleChipGroup.removeAllViews()
        preferredRoles.forEach { addRoleChip(it) }
    }

    private fun addRoleChip(role: String) {
        val chip = com.google.android.material.chip.Chip(requireContext()).apply {
            text = role
            isCheckable = false
            isClickable = true
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                // Remove role and chip
                val index = preferredRoles.indexOfFirst { it.equals(role, ignoreCase = true) }
                if (index >= 0) preferredRoles.removeAt(index)
                binding.roleChipGroup.removeView(this)
            }
        }
        binding.roleChipGroup.addView(chip)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is EditMeetingViewModel.EditMeetingState.Loading -> {
                        showLoading(true)
                    }

                    is EditMeetingViewModel.EditMeetingState.Success -> {
                        showLoading(false)
                        meeting = state.meeting
                        populateForm(state.meeting)
                    }

                    is EditMeetingViewModel.EditMeetingState.Updated -> {
                        showLoading(false)
                        meeting = state.meeting
                        showSuccess("Meeting updated successfully")
                    }

                    is EditMeetingViewModel.EditMeetingState.Deleted -> {
                        showLoading(false)
                        showSuccess("Meeting deleted successfully")
                        findNavController().navigateUp()
                    }

                    is EditMeetingViewModel.EditMeetingState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun populateForm(meeting: Meeting) {
        with(binding) {
            // Set location
            venueEditText.setText(meeting.location)

            // Set meeting date (only date)
            val dateFormatter =
                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
            dateEditText.setText(dateFormatter.format(meeting.dateTime.toLocalDate()))

            // Set theme
            themeEditText.setText(meeting.theme)

            // Set times (no timezone conversion)
            startTimeEditText.setText(timeFormatter.format(meeting.dateTime.toLocalTime()))
            val endDt = meeting.endDateTime
            endTimeEditText.setText(endDt?.let { timeFormatter.format(it.toLocalTime()) } ?: "")

            // Populate preferred roles chips (editable)
            preferredRoles.clear()
            preferredRoles.addAll(meeting.preferredRoles)
            renderPreferredRoleChips()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validate date
        if (binding.dateEditText.text.isNullOrBlank()) {
            binding.dateInputLayout.error = "Date is required"
            isValid = false
        } else {
            binding.dateInputLayout.error = null
        }

        // Validate times
        if (binding.startTimeEditText.text.isNullOrBlank()) {
            binding.startTimeInputLayout.error = getString(R.string.error_required)
            isValid = false
        } else {
            binding.startTimeInputLayout.error = null
        }
        if (binding.endTimeEditText.text.isNullOrBlank()) {
            binding.endTimeInputLayout.error = getString(R.string.error_required)
            isValid = false
        } else {
            binding.endTimeInputLayout.error = null
        }

        return isValid
    }

    private fun isFormEdited(): Boolean {
        val currentMeeting = meeting ?: return false

        with(binding) {
            return venueEditText.text?.toString() != currentMeeting.location ||
                    themeEditText.text?.toString() != currentMeeting.theme ||
                    // Compare date text
                    run {
                        val currentDate =
                            Date(currentMeeting.dateTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000)
                        dateEditText.text?.toString() != dateFormat.format(currentDate)
                    }
        }
    }

    private fun updateMeeting() {
        if (!validateForm()) return
        val dateOnly = binding.dateEditText.text.toString().trim()
        val venue = binding.venueEditText.text.toString().trim()
        val theme = binding.themeEditText.text.toString().trim()
        val isRecurring = false

        try {
            val current = meeting ?: throw IllegalStateException("Meeting not loaded")

            // Parse using java.time to avoid timezone shifts
            val dateFormatter =
                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

            val pickedLocalDate: LocalDate = LocalDate.parse(dateOnly, dateFormatter)
            val startLocalTime: LocalTime = LocalTime.parse(
                binding.startTimeEditText.text?.toString()?.trim().orEmpty(),
                timeFormatter
            )
            val endLocalTime: LocalTime = LocalTime.parse(
                binding.endTimeEditText.text?.toString()?.trim().orEmpty(),
                timeFormatter
            )

            val startLdt: LocalDateTime = LocalDateTime.of(pickedLocalDate, startLocalTime)
            val endLdt: LocalDateTime = LocalDateTime.of(pickedLocalDate, endLocalTime)

            // Validate end after start
            if (!endLdt.isAfter(startLdt)) {
                binding.endTimeInputLayout.error = getString(R.string.error_generic)
                showError("End time must be after start time")
                return
            } else {
                binding.endTimeInputLayout.error = null
            }

            val updated = Meeting(
                id = (current.id.ifEmpty { meetingId }),
                dateTime = startLdt,
                endDateTime = endLdt,
                location = venue,
                theme = theme,
                isRecurring = current.isRecurring,
                createdBy = current.createdBy,
                createdAt = current.createdAt,
                updatedAt = System.currentTimeMillis()
            )

            viewModel.updateMeeting(updated)

        } catch (e: Exception) {
            showError("Error saving meeting: ${e.message}")
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)

                binding.dateEditText.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)

                if (isStartTime) {
                    binding.startTimeEditText.setText(timeFormat.format(calendar.time))
                } else {
                    binding.endTimeEditText.setText(timeFormat.format(calendar.time))
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Meeting")
            .setMessage("Are you sure you want to delete this meeting?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMeeting(meetingId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDiscardChangesDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Discard Changes?")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("OK") { _, _ ->
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.scrollView.isEnabled = !isLoading
    }

    private fun showSuccess(message: String) {
        view?.let { view ->
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        UiUtils.showSnackbar(
            view = binding.root,
            message = message,
            actionText = R.string.retry,
            action = { viewModel.loadMeeting(meetingId) },
            duration = Snackbar.LENGTH_LONG
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    companion object {
        private const val ARG_MEETING_ID = "meeting_id"

        fun newInstance(meetingId: Int) = EditMeetingFragment().apply {
            arguments = bundleOf(ARG_MEETING_ID to meetingId)
        }
    }
}

