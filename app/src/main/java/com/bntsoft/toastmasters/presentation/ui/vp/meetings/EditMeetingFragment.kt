package com.bntsoft.toastmasters.presentation.ui.vp.meetings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.children
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
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.viewmodel.MeetingsViewModel
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
    private val meetingsViewModel: MeetingsViewModel by activityViewModels()
    private var meetingId: String = 0.toString()
    private var meeting: Meeting? = null
    private var currentOfficers = mutableMapOf<String, String>()

    // No separate list; we'll render chips directly and compute counts on save

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
        setupRoleManagement()
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

        // Add role button via manual text -> count dialog -> chips
        binding.btnAddRole.setOnClickListener {
            val role = binding.roleEditText.text?.toString()?.trim().orEmpty()
            if (role.isEmpty()) {
                binding.roleInputLayout.error = getString(R.string.error_required)
                return@setOnClickListener
            }
            binding.roleInputLayout.error = null
            showRoleCountDialog(role)
            binding.roleEditText.setText("")
            // Hide manual input after adding
            binding.roleInputLayout.visibility = View.GONE
            binding.btnAddRole.visibility = View.GONE
        }
    }

    private fun setupRoleManagement() {
        // Populate dropdown with preferred roles
        val roles = resources.getStringArray(R.array.Preferred_roles).toList()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            roles
        )

        val dropdown = binding.roleDropdown as MaterialAutoCompleteTextView
        dropdown.setAdapter(adapter)
        dropdown.setOnClickListener { if (adapter.count > 0) dropdown.showDropDown() }
        dropdown.setOnFocusChangeListener { _, hasFocus -> if (hasFocus && adapter.count > 0) dropdown.showDropDown() }
        dropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedRole = adapter.getItem(position) ?: return@setOnItemClickListener
            if (position == 0) {
                // "+ Add new role" selected: show manual input
                binding.roleInputLayout.visibility = View.VISIBLE
                binding.btnAddRole.visibility = View.VISIBLE
                binding.roleEditText.requestFocus()
                dropdown.setText("", false)
            } else {
                showRoleCountDialog(selectedRole)
                dropdown.setText("", false)
            }
            dropdown.clearFocus()
        }
        // Prevent typing
        dropdown.keyListener = null
        binding.roleDropdownLayout.isFocusable = false
        binding.roleDropdownLayout.isFocusableInTouchMode = false

        // Hide manual input initially
        binding.roleInputLayout.visibility = View.GONE
        binding.btnAddRole.visibility = View.GONE
    }

    private fun showRoleCountDialog(role: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_role_count, null)
        val input = dialogView.findViewById<TextInputEditText>(R.id.roleCountInput).apply {
            setText("1")
            setSelectAllOnFocus(true)
            requestFocus()
        }
        val til = dialogView.findViewById<TextInputLayout>(R.id.roleCountLayout)
        til.hint = "Number of $role"

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add $role")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val count = input.text?.toString()?.toIntOrNull() ?: 0
                if (count <= 0) {
                    til.error = getString(R.string.error_required)
                    return@setOnClickListener
                }
                til.error = null
                addRoleChips(role, count)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun addRoleChips(role: String, count: Int) {
        // Determine current count for this role to continue numbering
        val existingCount = binding.roleChipGroup.children.count { (it as? Chip)?.tag == role }
        val startIndex = existingCount + 1
        val endIndex = existingCount + count
        for (i in startIndex..endIndex) {
            val chip = Chip(requireContext()).apply {
                text = "$role $i"
                isCloseIconVisible = true
                tag = role
                setOnCloseIconClickListener { binding.roleChipGroup.removeView(this) }
            }
            binding.roleChipGroup.addView(chip)
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

            // Populate role chips from roleCounts
            roleChipGroup.removeAllViews()
            meeting.roleCounts.forEach { (role, count) ->
                addRoleChips(role, count)
            }
            
            // Update current officers
            currentOfficers.clear()
            currentOfficers.putAll(meeting.officers)
            
            // Update the latest officers in the ViewModel
            if (meeting.officers.isNotEmpty()) {
                meetingsViewModel.updateLatestOfficers(meeting.officers)
            }
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

            // Build roleCounts from chips
            val roleCounts = mutableMapOf<String, Int>()
            binding.roleChipGroup.children.forEach { view ->
                val chip = view as Chip
                val role = chip.tag?.toString() ?: return@forEach
                roleCounts[role] = (roleCounts[role] ?: 0) + 1
            }

            val updated = Meeting(
                id = (current.id.ifEmpty { meetingId }),
                dateTime = startLdt,
                endDateTime = endLdt,
                location = venue,
                theme = theme,
                roleCounts = roleCounts,
                officers = currentOfficers,
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

