package com.bntsoft.toastmasters.presentation.ui.vp.meetings

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentCreateMeetingBinding
import com.bntsoft.toastmasters.databinding.ItemMeetingFormBinding
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingFormData
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.uistates.CreateMeetingState
import com.bntsoft.toastmasters.presentation.ui.vp.meetings.viewmodel.MeetingsViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class CreateMeetingFragment : Fragment() {

    private val viewModel: MeetingsViewModel by viewModels()

    private var _binding: FragmentCreateMeetingBinding? = null
    private val binding get() = _binding!!

    private val meetingForms = mutableListOf<MeetingFormData>()

    private val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateMeetingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the first meeting form
        addNewMeetingForm()

        // Set click listener for "Add Another Meeting" button
        binding.addAnotherMeetingButton.setOnClickListener {
            addNewMeetingForm()
        }

        // Set click listener for "Create Meeting(s)" button
        binding.createMeetingsButton.setOnClickListener {
            if (validateForms()) {
                createMeetings()
            } else {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            viewModel.createMeetingState.collect { state ->
                when (state) {
                    is CreateMeetingState.Success -> {
                        Toast.makeText(
                            context,
                            "Meeting '${state.meeting.theme}' created successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.meetingFormsContainer.removeAllViews()
                        meetingForms.clear()
                        addNewMeetingForm()
                        viewModel.resetCreateMeetingState()
                        findNavController().navigate(
                            R.id.action_createMeetingFragment_to_dashboardFragment
                        )
                    }

                    is CreateMeetingState.Duplicate -> {
                        showDuplicateMeetingDialog(state.meeting)
                        viewModel.resetCreateMeetingState()
                    }

                    is CreateMeetingState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetCreateMeetingState()
                    }

                    is CreateMeetingState.Loading -> {
                        // Show a loading indicator if you have one
                    }

                    is CreateMeetingState.Idle -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    private fun addNewMeetingForm() {
        val formBinding =
            ItemMeetingFormBinding.inflate(layoutInflater, binding.meetingFormsContainer, false)

        // Initialize with default values
        val calendar = Calendar.getInstance()
        val startTime = Calendar.getInstance()
        val endTime = Calendar.getInstance()

        // Calculate the base date (next Saturday after last meeting or today)
        viewLifecycleOwner.lifecycleScope.launch {
            val lastMeetingDate = viewModel.getLastMeetingDate()
            val baseCalendar = if (lastMeetingDate != null) {
                // Start from the last meeting date
                Calendar.getInstance().apply { time = lastMeetingDate }
            } else {
                // No meetings exist, start from today
                Calendar.getInstance()
            }
            
            // Move to the next Saturday from the base date
            val dayOfWeek = baseCalendar.get(Calendar.DAY_OF_WEEK)
            val daysUntilSaturday = (Calendar.SATURDAY - dayOfWeek + 7) % 7
            baseCalendar.add(Calendar.DAY_OF_MONTH, 
                if (daysUntilSaturday == 0) 7 else daysUntilSaturday
            )
            
            // For subsequent forms, add 7 days for each form after the first one
            val formIndex = meetingForms.size
            if (formIndex > 0) {
                baseCalendar.add(Calendar.DAY_OF_MONTH, 7 * formIndex)
            }
            
            // Update the working calendar
            calendar.time = baseCalendar.time

            // Set the times
            startTime.time = calendar.time
            startTime.set(Calendar.HOUR_OF_DAY, 17)
            startTime.set(Calendar.MINUTE, 30)
            startTime.set(Calendar.SECOND, 0)
            startTime.set(Calendar.MILLISECOND, 0)

            endTime.time = calendar.time
            endTime.set(Calendar.HOUR_OF_DAY, 19)
            endTime.set(Calendar.MINUTE, 30)
            endTime.set(Calendar.SECOND, 0)
            endTime.set(Calendar.MILLISECOND, 0)

            // Update the UI with the calculated date and times
            formBinding.apply {
                meetingDateInput.setText(dateFormat.format(calendar.time))
                startTimeInput.setText(timeFormat.format(startTime.time))
                endTimeInput.setText(timeFormat.format(endTime.time))
            }

            // Store the calendar instances in the form data
            val meetingFormData = MeetingFormData(formBinding, startTime, endTime)
            meetingForms.add(meetingFormData)

            binding.meetingFormsContainer.addView(formBinding.root)
            binding.scrollView.post {
                binding.scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }

        formBinding.apply {

            // Setup date picker
            meetingDateInput.setOnClickListener {
                // Find the meeting form data for this binding
                val formData = meetingForms.find { it.binding === formBinding }
                formData?.let {
                    showDatePicker(it.startCalendar, meetingDateInput)
                }
            }

            // Setup time pickers
            startTimeInput.setOnClickListener {
                val formData = meetingForms.find { it.binding === formBinding }
                formData?.let {
                    showTimePicker(it.startCalendar, startTimeInput)
                }
            }

            endTimeInput.setOnClickListener {
                val formData = meetingForms.find { it.binding === formBinding }
                formData?.let {
                    showTimePicker(it.endCalendar, endTimeInput)
                }
            }

            // Remove button visibility and action
            if (meetingForms.isEmpty()) {
                removeMeetingButton.visibility = View.GONE
            } else {
                removeMeetingButton.visibility = View.VISIBLE
                removeMeetingButton.setOnClickListener {
                    val formData = meetingForms.find { it.binding === formBinding }
                    formData?.let {
                        binding.meetingFormsContainer.removeView(root)
                        meetingForms.remove(it)
                    }
                }
            }

            setupRoleManagement(formBinding)
        }

        // Scroll to the bottom
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun setupRoleManagement(formBinding: ItemMeetingFormBinding) {
        // Hide the manual input initially
        formBinding.roleInputLayout.visibility = View.GONE
        formBinding.saveRoleButton.visibility = View.GONE

        // Get preferred roles from string array
        val roles = resources.getStringArray(R.array.Preferred_roles).toList()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            roles
        )

        // Set up the dropdown
        formBinding.apply {
            val dropdown = roleDropdown as MaterialAutoCompleteTextView
            dropdown.apply {
                setAdapter(adapter)
                setOnClickListener {
                    if (adapter.count > 0) {
                        showDropDown()
                    }
                }
                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus && adapter.count > 0) {
                        showDropDown()
                    }
                }
                setOnItemClickListener { _, _, position, _ ->
                    val selectedRole = adapter.getItem(position) ?: return@setOnItemClickListener

                    if (position == 0) { // "+ Add new role" option
                        roleInputLayout.visibility = View.VISIBLE
                        saveRoleButton.visibility = View.VISIBLE
                        roleInput.requestFocus()
                        setText("", false) // Clear the text
                    } else {
                        addRoleChip(formBinding, selectedRole)
                        setText("", false) // Clear the text
                    }
                    clearFocus()
                }
                keyListener = null // Disable text input
            }

            // Make sure the TextInputLayout doesn't steal focus
            roleDropdownLayout.isFocusable = false
            roleDropdownLayout.isFocusableInTouchMode = false
        }

        // Handle manual role addition
        formBinding.saveRoleButton.setOnClickListener {
            val role = formBinding.roleInput.text.toString().trim()
            if (role.isNotEmpty()) {
                addRoleChip(formBinding, role)
                formBinding.roleInput.text?.clear()
                formBinding.roleInputLayout.visibility = View.GONE
                formBinding.saveRoleButton.visibility = View.GONE
            }
        }

        formBinding.roleInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val role = formBinding.roleInput.text.toString().trim()
                if (role.isNotEmpty()) {
                    addRoleChip(formBinding, role)
                    formBinding.roleInput.text?.clear()
                    formBinding.roleInputLayout.visibility = View.GONE
                    formBinding.saveRoleButton.visibility = View.GONE
                }
                true
            } else {
                false
            }
        }
    }

    private fun addRoleChip(formBinding: ItemMeetingFormBinding, role: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_role_count, null)
        val input = dialogView.findViewById<TextInputEditText>(R.id.roleCountInput).apply {
            setText("1")  // Default to 1
            setSelectAllOnFocus(true)
            requestFocus()
        }
        
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.roleCountLayout)
        textInputLayout.hint = "Number of $role"
        
        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_ToastMasters_Dialog)
            .setTitle("Add $role")
            .setView(dialogView)
            .setPositiveButton("Add") { dialogInterface, _ ->
                val count = input.text.toString().toIntOrNull() ?: 0
                if (count > 0) {
                    for (i in 1..count) {
                        val chip = Chip(context).apply {
                            text = "$role $i"
                            isCloseIconVisible = true
                            setOnCloseIconClickListener {
                                formBinding.roleChipGroup.removeView(this)
                            }
                            tag = role
                            setChipBackgroundColorResource(R.color.chip_background)
                            setTextAppearance(R.style.ChipTextAppearance)
                        }
                        formBinding.roleChipGroup.addView(chip)
                    }
                }
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()
            
        // Show keyboard when dialog appears
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
        
        // Set input filter to allow only numbers 1-99
        input.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(2), InputFilter { source, _, _, _, _, _ ->
            if (source.isNotEmpty() && source.toString().matches("[0-9]+".toRegex())) {
                val value = source.toString().toInt()
                if (value in 1..99) {
                    null // Accept the input
                } else {
                    "" // Reject the input
                }
            } else if (source.isEmpty()) {
                null // Allow empty input (for backspace)
            } else {
                "" // Reject non-numeric input
            }
        })
    }

    private fun validateForms(): Boolean {
        if (meetingForms.isEmpty()) return false

        return meetingForms.all { formData ->
            formData.binding.meetingDateInput.text?.isNotBlank() == true &&
                    formData.binding.startTimeInput.text?.isNotBlank() == true &&
                    formData.binding.endTimeInput.text?.isNotBlank() == true &&
                    formData.binding.venueInput.text?.isNotBlank() == true &&
                    formData.binding.themeInput.text?.isNotBlank() == true
        }
    }

    private fun showDatePicker(calendar: Calendar, dateView: TextInputEditText) {
        val datePicker =
            com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(calendar.timeInMillis)
                .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            calendar.timeInMillis = selection
            dateView.setText(dateFormat.format(calendar.time))
        }

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker(calendar: Calendar, timeView: TextInputEditText) {
        val isSystem24Hour = android.text.format.DateFormat.is24HourFormat(requireContext())
        val clockFormat =
            if (isSystem24Hour) com.google.android.material.timepicker.TimeFormat.CLOCK_24H else com.google.android.material.timepicker.TimeFormat.CLOCK_12H

        val timePicker =
            com.google.android.material.timepicker.MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText("Select time")
                .build()

        timePicker.addOnPositiveButtonClickListener {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)
            timeView.setText(timeFormat.format(calendar.time))
        }

        timePicker.show(childFragmentManager, "TIME_PICKER")
    }

    private fun createMeetings(forceCreate: Boolean = false) {
        meetingForms.forEach { formData ->
            val theme = formData.binding.themeInput.text.toString()
            val venue = formData.binding.venueInput.text.toString()

            // Combine date from startCalendar with time from start and end calendars
            val startCal = formData.startCalendar
            val endCal = formData.endCalendar

            val finalStartCalendar = Calendar.getInstance().apply {
                time = startCal.time // This sets the date and time
            }

            val finalEndCalendar = Calendar.getInstance().apply {
                time = startCal.time // Start with the same date
                set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY)) // Set hour from endCal
                set(Calendar.MINUTE, endCal.get(Calendar.MINUTE)) // Set minute from endCal
            }

            if (finalStartCalendar.after(finalEndCalendar)) {
                Toast.makeText(
                    context,
                    "Error: End time must be after start time.",
                    Toast.LENGTH_LONG
                ).show()
                return@forEach
            }

            val startLocalDateTime =
                LocalDateTime.ofInstant(finalStartCalendar.toInstant(), ZoneId.systemDefault())
            val endLocalDateTime =
                LocalDateTime.ofInstant(finalEndCalendar.toInstant(), ZoneId.systemDefault())

            // Extract roles and their counts from chips
            val roleCounts = mutableMapOf<String, Int>()
            formData.binding.roleChipGroup.children.forEach { view ->
                val chip = view as Chip
                val role = chip.tag?.toString() ?: return@forEach
                roleCounts[role] = roleCounts.getOrDefault(role, 0) + 1
            }
            
            val meeting = Meeting(
                theme = theme,
                dateTime = startLocalDateTime,
                endDateTime = endLocalDateTime,
                location = venue,
                roleCounts = roleCounts
            )
            viewModel.createMeeting(meeting, forceCreate)
        }
    }

    private fun showDuplicateMeetingDialog(meeting: Meeting) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Meeting already exists")
            .setMessage("A meeting is already scheduled on this date and time. Do you want to create it again?")
            .setPositiveButton("Yes") { _, _ ->
                createMeetings(forceCreate = true)
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
