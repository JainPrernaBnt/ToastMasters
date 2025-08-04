package com.bntsoft.toastmasters.ui.meetings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentCreateMeetingBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateMeetingFragment : Fragment() {

    private var _binding: FragmentCreateMeetingBinding? = null
    private val binding get() = _binding!!
    private val meetingForms = mutableListOf<View>()
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
        setupMeetingForm(binding.meetingFormContainer, isFirst = true)
        
        // Set click listener for "Add Another Meeting" button
        binding.addAnotherMeetingButton.setOnClickListener {
            addNewMeetingForm()
        }
        
        // Set click listener for "Create Meeting(s)" button
        binding.createMeetingsButton.setOnClickListener {
            createMeetings()
        }
    }

    private fun setupMeetingForm(container: ViewGroup, isFirst: Boolean = false) {
        val formView = layoutInflater.inflate(R.layout.item_meeting_form, container, false)
        
        // Set default date to next Saturday
        val calendar = Calendar.getInstance()
        calendar.apply {
            // Move to next Saturday
            add(Calendar.DAY_OF_WEEK, (Calendar.SATURDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7)
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
        
        // Initialize form fields
        formView.apply {
            // Set default values
            findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.meetingDateInput)?.setText(
                dateFormat.format(calendar.time)
            )
            findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.startTimeInput)?.setText(
                timeFormat.format(startTime.time)
            )
            findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.endTimeInput)?.setText(
                timeFormat.format(endTime.time)
            )
            
            // Set up date picker
            findViewById<View>(R.id.meetingDateInput)?.setOnClickListener { showDatePicker(calendar, it as com.google.android.material.textfield.TextInputEditText) }
            
            // Set up time pickers
            findViewById<View>(R.id.startTimeInput)?.setOnClickListener { showTimePicker(startTime, it as com.google.android.material.textfield.TextInputEditText) }
            findViewById<View>(R.id.endTimeInput)?.setOnClickListener { showTimePicker(endTime, it as com.google.android.material.textfield.TextInputEditText) }
            
            // Set up remove button (not shown for the first form)
            val removeButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.removeMeetingButton)
            if (isFirst) {
                removeButton.visibility = View.GONE
            } else {
                removeButton.visibility = View.VISIBLE
                removeButton.setOnClickListener {
                    container.removeView(formView)
                    meetingForms.remove(formView)
                    updateAddButtonPosition()
                }
            }
        }
        
        // Add the form to the container
        container.addView(formView, container.childCount - 1) // Add before the "Add Another" button
        meetingForms.add(formView)
        
        // Update the position of the "Add Another" button
        updateAddButtonPosition()
    }
    
    private fun addNewMeetingForm() {
        setupMeetingForm(binding.meetingFormContainer)
        
        // Scroll to the bottom to show the newly added form
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
    
    private fun updateAddButtonPosition() {
        // Update the position of the "Add Another" button to be after the last form
        binding.addAnotherMeetingButton.bringToFront()
        binding.createMeetingsButton.bringToFront()
    }
    
    private fun showDatePicker(calendar: Calendar, dateView: com.google.android.material.textfield.TextInputEditText) {
        // In a real implementation, you would show a DatePickerDialog here
        // For now, we'll just update the date when clicked
        dateView.setText(dateFormat.format(calendar.time))
    }
    
    private fun showTimePicker(calendar: Calendar, timeView: com.google.android.material.textfield.TextInputEditText) {
        // In a real implementation, you would show a TimePickerDialog here
        // For now, we'll just update the time when clicked
        timeView.setText(timeFormat.format(calendar.time))
    }
    
    private fun createMeetings() {
        // In a real implementation, you would collect data from all forms and create meetings
        // For now, we'll just show a toast
        val count = meetingForms.size
        val message = if (count == 1) "1 meeting created" else "$count meetings created"
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
