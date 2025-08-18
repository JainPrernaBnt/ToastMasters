package com.bntsoft.toastmasters.presentation.ui.vp.meetings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bntsoft.toastmasters.databinding.FragmentCreateMeetingBinding
import com.bntsoft.toastmasters.databinding.ItemMeetingFormBinding
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingFormData
import com.bntsoft.toastmasters.presentation.viewmodel.CreateMeetingState
import com.bntsoft.toastmasters.presentation.viewmodel.MeetingsViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
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
                        viewModel.resetCreateMeetingState()
                        // Optionally, navigate back or clear forms
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

        // Set default date to next Saturday
        val calendar = Calendar.getInstance()
        if (meetingForms.isNotEmpty()) {
            // If there are existing forms, base the new date on the last one
            val lastMeetingCalendar = meetingForms.last().startCalendar
            calendar.time = lastMeetingCalendar.time
            calendar.add(Calendar.DAY_OF_MONTH, 7) // Add 7 days for the next week
        } else {
            // For the first form, find the next Saturday from today
            val today = calendar.get(Calendar.DAY_OF_WEEK)
            val daysUntilSaturday = (Calendar.SATURDAY - today + 7) % 7
            calendar.add(Calendar.DAY_OF_WEEK, if (daysUntilSaturday == 0) 7 else daysUntilSaturday)
        }

        // Set default start time (5:30 PM)
        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 17) // 5 PM
            set(Calendar.MINUTE, 30)
        }

        // Set default end time (7:30 PM)
        val endTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19) // 7 PM
            set(Calendar.MINUTE, 30)
        }

        val meetingFormData = MeetingFormData(formBinding, startTime, endTime)

        formBinding.apply {
            meetingDateInput.setText(dateFormat.format(calendar.time))
            startTimeInput.setText(timeFormat.format(startTime.time))
            endTimeInput.setText(timeFormat.format(endTime.time))

            // Setup date picker
            meetingDateInput.setOnClickListener {
                showDatePicker(startTime, meetingDateInput) // startTime keeps track of the date
            }

            // Setup time pickers
            startTimeInput.setOnClickListener {
                showTimePicker(startTime, startTimeInput)
            }
            endTimeInput.setOnClickListener {
                showTimePicker(endTime, endTimeInput)
            }

            // Remove button visibility and action
            if (meetingForms.isEmpty()) {
                removeMeetingButton.visibility = View.GONE
            } else {
                removeMeetingButton.visibility = View.VISIBLE
                removeMeetingButton.setOnClickListener {
                    binding.meetingFormsContainer.removeView(root)
                    meetingForms.remove(meetingFormData)
                }
            }

            setupRoleManagement(formBinding)
        }

        binding.meetingFormsContainer.addView(formBinding.root)
        meetingForms.add(meetingFormData)

        // Scroll to the bottom
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun setupRoleManagement(formBinding: ItemMeetingFormBinding) {
        formBinding.addRoleButton.setOnClickListener {
            formBinding.roleInputLayout.visibility = View.VISIBLE
            formBinding.saveRoleButton.visibility = View.VISIBLE
            formBinding.roleInput.requestFocus()
        }

        formBinding.saveRoleButton.setOnClickListener {
            val role = formBinding.roleInput.text.toString().trim()
            if (role.isNotEmpty()) {
                addRoleChip(formBinding, role)
                formBinding.roleInput.text?.clear()
            }
        }

        formBinding.roleInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val role = formBinding.roleInput.text.toString().trim()
                if (role.isNotEmpty()) {
                    addRoleChip(formBinding, role)
                    formBinding.roleInput.text?.clear()
                }
                true
            } else {
                false
            }
        }
    }

    private fun addRoleChip(formBinding: ItemMeetingFormBinding, role: String) {
        val chip = Chip(context).apply {
            text = role
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                formBinding.roleChipGroup.removeView(this)
            }
        }
        formBinding.roleChipGroup.addView(chip)
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

    private fun createMeetings() {
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

            val roles = formData.binding.roleChipGroup.children.map { (it as Chip).text.toString() }.toList()

            val meeting = Meeting(
                theme = theme, // Using theme as title
                dateTime = startLocalDateTime,
                endDateTime = endLocalDateTime,
                location = venue,
                preferredRoles = roles
            )
            viewModel.createMeeting(meeting)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
