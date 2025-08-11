package com.bntsoft.toastmasters.presentation.ui.vp.meetings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bntsoft.toastmasters.databinding.FragmentCreateMeetingBinding
import com.bntsoft.toastmasters.databinding.ItemMeetingFormBinding
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateMeetingFragment : Fragment() {

    private var _binding: FragmentCreateMeetingBinding? = null
    private val binding get() = _binding!!

    private val meetingForms = mutableListOf<ItemMeetingFormBinding>()

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
                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addNewMeetingForm() {
        val formBinding =
            ItemMeetingFormBinding.inflate(layoutInflater, binding.meetingFormsContainer, false)

        // Set default date to next Saturday
        val calendar = Calendar.getInstance().apply {
            // Move to next Saturday
            val today = get(Calendar.DAY_OF_WEEK)
            val daysUntilSaturday = (Calendar.SATURDAY - today + 7) % 7
            add(Calendar.DAY_OF_WEEK, if (daysUntilSaturday == 0) 7 else daysUntilSaturday)
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

        formBinding.apply {
            meetingDateInput.setText(dateFormat.format(calendar.time))
            startTimeInput.setText(timeFormat.format(startTime.time))
            endTimeInput.setText(timeFormat.format(endTime.time))

            // Setup date picker (implement showDatePicker properly)
            meetingDateInput.setOnClickListener {
                showDatePicker(calendar, meetingDateInput)
            }

            // Setup time pickers (implement showTimePicker properly)
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
                    meetingForms.remove(formBinding)
                }
            }

//            setupRoleManagement(formBinding)
        }

        binding.meetingFormsContainer.addView(formBinding.root)
        meetingForms.add(formBinding)

        // Scroll to the bottom
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

//    private fun setupRoleManagement(formBinding: ItemMeetingFormBinding) {
//        formBinding.addRoleButton.setOnClickListener {
//            formBinding.roleInputLayout.visibility = View.VISIBLE
//            formBinding.saveRoleButton.visibility = View.VISIBLE
//            formBinding.roleInput.requestFocus()
//        }
//
//        formBinding.saveRoleButton.setOnClickListener {
//            val role = formBinding.roleInput.text.toString().trim()
//            if (role.isNotEmpty()) {
//                addRoleChip(formBinding, role)
//                formBinding.roleInput.text?.clear()
//                formBinding.roleInputLayout.visibility = View.GONE
//                formBinding.saveRoleButton.visibility = View.GONE
//            }
//        }
//
//        formBinding.roleInput.setOnEditorActionListener { _, actionId, _ ->
//            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
//                val role = formBinding.roleInput.text.toString().trim()
//                if (role.isNotEmpty()) {
//                    addRoleChip(formBinding, role)
//                    formBinding.roleInput.text?.clear()
//                    formBinding.roleInputLayout.visibility = View.GONE
//                    formBinding.saveRoleButton.visibility = View.GONE
//                }
//                true
//            } else {
//                false
//            }
//        }
//    }

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

        return meetingForms.all { form ->
            form.meetingTitleInput.text?.isNotBlank() == true &&
                    form.meetingDateInput.text?.isNotBlank() == true &&
                    form.startTimeInput.text?.isNotBlank() == true &&
                    form.endTimeInput.text?.isNotBlank() == true &&
                    form.venueInput.text?.isNotBlank() == true &&
                    form.themeInput.text?.isNotBlank() == true
        }
    }

    private fun showDatePicker(calendar: Calendar, dateView: TextInputEditText) {
        // TODO: Implement actual date picker dialog
        // For now, just set the existing calendar date string
        dateView.setText(dateFormat.format(calendar.time))
    }

    private fun showTimePicker(calendar: Calendar, timeView: TextInputEditText) {
        // TODO: Implement actual time picker dialog
        // For now, just set the existing calendar time string
        timeView.setText(timeFormat.format(calendar.time))
    }

    private fun createMeetings() {
        val count = meetingForms.size
        val message = if (count == 1) "1 meeting created" else "$count meetings created"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
