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
import com.bntsoft.toastmasters.utils.UiUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class EditMeetingFragment : Fragment() {

    private var _binding: FragmentEditMeetingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditMeetingViewModel by viewModels()
    private var meetingId: Int = 0
    private var meeting: Meeting? = null

    private val dateFormat = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private val timeFormat = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())

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

        // Get meeting ID from arguments
        meetingId = arguments?.getInt(ARG_MEETING_ID) ?: 0

        setupToolbar()
        setupClickListeners()
        observeViewModel()

        // Load meeting details
        viewModel.loadMeeting(meetingId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            if (isFormEdited()) {
                showDiscardChangesDialog()
            } else {
                findNavController().navigateUp()
            }
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    if (validateForm()) {
                        updateMeeting()
                    }
                    true
                }

                R.id.action_delete -> {
                    showDeleteConfirmationDialog()
                    true
                }

                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        // Date Picker
        binding.dateInputLayout.setEndIconOnClickListener {
            showDatePicker()
        }

        binding.dateEditText.setOnClickListener {
            showDatePicker()
        }

        // Time Pickers
        binding.startTimeEditText.setOnClickListener {
            showTimePicker(isStartTime = true)
        }

        binding.endTimeEditText.setOnClickListener {
            showTimePicker(isStartTime = false)
        }

        // Start Time Picker
        binding.startTimeInputLayout.setEndIconOnClickListener {
            showTimePicker(true)
        }

        binding.startTimeEditText.setOnClickListener {
            showTimePicker(true)
        }

        // End Time Picker
        binding.endTimeInputLayout.setEndIconOnClickListener {
            showTimePicker(false)
        }

        binding.endTimeEditText.setOnClickListener {
            showTimePicker(false)
        }
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
            // Set meeting title
            titleEditText.setText(meeting.title)

            // Set meeting date and time
            val date = Date(meeting.dateTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000)
            dateEditText.setText(dateFormat.format(date))
            startTimeEditText.setText(timeFormat.format(date))

            // Set end time if available
            meeting.endDateTime?.let { endDateTime ->
                val endDate = Date(endDateTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000)
                endTimeEditText.setText(timeFormat.format(endDate))
            }

            // Set location
            venueEditText.setText(meeting.location)

            // Set agenda if available
            if (meeting.agenda.isNotBlank()) {
                agendaEditText.setText(meeting.agenda)
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validate title
        if (binding.titleEditText.text.isNullOrBlank()) {
            binding.titleInputLayout.error = "Title is required"
            isValid = false
        } else {
            binding.titleInputLayout.error = null
        }

        // Validate date
        if (binding.dateEditText.text.isNullOrBlank()) {
            binding.dateInputLayout.error = "Date is required"
            isValid = false
        } else {
            binding.dateInputLayout.error = null
        }

        // Validate time
        if (binding.startTimeEditText.text.isNullOrBlank()) {
            binding.startTimeInputLayout.error = "Start time is required"
            isValid = false
        } else {
            binding.startTimeInputLayout.error = null
        }

        // Validate end time
        if (binding.endTimeEditText.text.isNullOrBlank()) {
            binding.endTimeInputLayout.error = "End time is required"
            isValid = false
        } else {
            binding.endTimeInputLayout.error = null
        }

        return isValid
    }

    private fun isFormEdited(): Boolean {
        val currentMeeting = meeting ?: return false

        with(binding) {
            return titleEditText.text?.toString() != currentMeeting.title ||
                    venueEditText.text?.toString() != currentMeeting.location ||
                    agendaEditText.text?.toString() != currentMeeting.agenda
        }
    }

    private fun updateMeeting() {
        if (!validateForm()) return

        val title = binding.titleEditText.text.toString().trim()
        val date = binding.dateEditText.text.toString().trim()
        val startTime = binding.startTimeEditText.text.toString().trim()
        val endTime = binding.endTimeEditText.text.toString().trim()
        val venue = binding.venueEditText.text.toString().trim()
        val agenda = binding.agendaEditText.text.toString().trim()
        val isRecurring = false // Removed recurring switch from UI

        try {
            val dateTime = "$date $startTime"
            val endDateTime = "$date $endTime"

            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy h:mm a", Locale.getDefault())
            val startDate = dateFormat.parse(dateTime)
            val endDate = dateFormat.parse(endDateTime)

            val meeting = Meeting(
                id = meeting?.id ?: 0,
                title = title,
                description = "",
                dateTime = startDate?.toInstant()?.atZone(java.time.ZoneId.systemDefault())
                    ?.toLocalDateTime()
                    ?: throw IllegalStateException("Invalid start date"),
                endDateTime = endDate?.toInstant()?.atZone(java.time.ZoneId.systemDefault())
                    ?.toLocalDateTime(),
                location = venue,
                agenda = agenda,
                isRecurring = isRecurring,
                createdBy = meeting?.createdBy ?: "",
                createdAt = meeting?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            viewModel.updateMeeting(meeting)

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

