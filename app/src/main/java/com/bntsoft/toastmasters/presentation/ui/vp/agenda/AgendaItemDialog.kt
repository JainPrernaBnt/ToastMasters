package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
import com.bntsoft.toastmasters.domain.repository.AssignedRoleRepository
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.databinding.DialogAddEditAgendaBinding
import com.bntsoft.toastmasters.utils.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AgendaItemDialog : DialogFragment() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_ToastMasters_Dialog)
    }
    @Inject
    lateinit var assignedRoleRepository: AssignedRoleRepository
    
    @Inject
    lateinit var meetingRepository: MeetingRepository

    private var _binding: DialogAddEditAgendaBinding? = null
    private val binding get() = _binding!!
    private var onSaveClickListener: ((AgendaItemDto) -> Unit)? = null
    private var agendaItem: AgendaItemDto? = null
    private var meetingId: String = ""
    private val roleOptions = mutableListOf<String>()

    private var greenTime: Int = 0
    private var yellowTime: Int = 0
    private var redTime: Int = 0
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private var selectedTime: Calendar = Calendar.getInstance()

    companion object {
        private const val ARG_AGENDA_ITEM = "agenda_item"
        private const val ARG_MEETING_ID = "meeting_id"

        fun newInstance(
            meetingId: String,
            agendaItem: AgendaItemDto? = null,
            onSave: (AgendaItemDto) -> Unit
        ): AgendaItemDialog {
            return AgendaItemDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_AGENDA_ITEM, agendaItem)
                    putString(ARG_MEETING_ID, meetingId)
                }
                onSaveClickListener = onSave
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditAgendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.let { window ->
            // Set soft input mode for keyboard
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            
            // Set dialog width to 90% of screen width
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get meetingId from arguments
        meetingId = arguments?.getString(ARG_MEETING_ID) ?: run {
            Log.e("AgendaItemDialog", "No meetingId provided")
            dismiss()
            return
        }

        Log.d("AgendaItemDialog", "Meeting ID: $meetingId")

        setupTimePicker()
        setupPresenters()

        arguments?.getParcelable<AgendaItemDto>(ARG_AGENDA_ITEM)?.let {
            agendaItem = it
            greenTime = it.greenTime
            yellowTime = it.yellowTime
            redTime = it.redTime
            populateFields(it)
        }

        binding.btnSave.setOnClickListener {
            if (!validateInputs()) {
                showError()
                return@setOnClickListener
            }

            val timeString = timeFormat.format(selectedTime.time)

            // Get the selected display text and extract member name and role
            val selectedDisplayText = binding.presenterInput.text.toString()
            val presenterMap = binding.presenterInput.getTag(R.id.presenter_map) as? Map<String, String>
            val memberNameOnly = presenterMap?.get(selectedDisplayText) ?: selectedDisplayText.split(" - ").firstOrNull() ?: selectedDisplayText
            
            // Extract role from display text
            val role = selectedDisplayText.split(" - ").getOrNull(1) ?: ""
            Log.d("AgendaItemDialog", "Selected role: $role for member: $memberNameOnly")
            
            // Check if role contains "Speaker" and fetch speaker details
            if (role.contains("Speaker", ignoreCase = true)) {
                Log.d("AgendaItemDialog", "Role contains Speaker, fetching speaker details...")
                fetchSpeakerDetailsAndSave(memberNameOnly, timeString)
            } else {
                Log.d("AgendaItemDialog", "Role does not contain Speaker, saving without speaker details")
                saveAgendaItem(memberNameOnly, timeString, "", 0, "")
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setupTimePicker() {
        binding.timeInput.apply {
            setOnClickListener { showTimePicker() }
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showTimePicker() }
            isFocusable = false  // Prevent keyboard from showing
            isFocusableInTouchMode = false
        }

        // Set initial time if available
        if (binding.timeInput.text.isNullOrEmpty()) {
            binding.timeInput.setText(timeFormat.format(selectedTime.time))
        }
    }

    private fun showTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedTime.set(Calendar.MINUTE, minute)
                binding.timeInput.setText(timeFormat.format(selectedTime.time))
            },
            selectedTime.get(Calendar.HOUR_OF_DAY),
            selectedTime.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun setupPresenters() {
        Log.d("AgendaItemDialog", "Setting up presenters...")

        // Validate meetingId before making the request
        if (meetingId.isBlank()) {
            Log.e("AgendaItemDialog", "Error: meetingId is empty")
            return
        }

        Log.d("AgendaItemDialog", "Loading presenters for meeting: $meetingId")

        // Load presenters from repository
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                assignedRoleRepository.getAssignedRoles(meetingId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val presenters = mutableListOf<String>()
                            val presenterMap = mutableMapOf<String, String>() // displayText -> memberName
                            result.data?.forEach { assignedRole ->
                                assignedRole.roles.forEach { role ->
                                    val displayText = "${assignedRole.memberName} - $role"
                                    if (!presenters.contains(displayText)) {
                                        presenters.add(displayText)
                                        presenterMap[displayText] = assignedRole.memberName
                                    }
                                }
                            }
                            
                            // Store the presenter mapping for later use
                            binding.presenterInput.setTag(R.id.presenter_map, presenterMap)

                            presenters.sort()
                            Log.d("AgendaItemDialog", "Loaded ${presenters.size} presenters")

                            activity?.runOnUiThread {
                                try {
                                    // Use MaterialAutoCompleteTextView helper to set items
                                    binding.presenterInput.setSimpleItems(presenters.toTypedArray())

                                    // Show dropdown on focus or click
                                    binding.presenterInput.setOnFocusChangeListener { _, hasFocus ->
                                        if (hasFocus && presenters.isNotEmpty()) {
                                            binding.presenterInput.showDropDown()
                                        }
                                    }
                                    binding.presenterInput.setOnClickListener {
                                        if (presenters.isNotEmpty()) {
                                            binding.presenterInput.showDropDown()
                                        }
                                    }

                                    // If already focused, ensure dropdown shows
                                    if (binding.presenterInput.hasFocus() && presenters.isNotEmpty()) {
                                        binding.presenterInput.post { binding.presenterInput.showDropDown() }
                                    }
                                } catch (e: Exception) {
                                    Log.e("AgendaItemDialog", "Error updating presenter dropdown", e)
                                }
                            }
                        }

                        is Resource.Error -> {
                            Log.e("AgendaItemDialog", "Error loading presenters: ${result.message}")
                        }

                        is Resource.Loading -> {
                            // optional loading state
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AgendaItemDialog", "Exception while loading presenters", e)
            }
        }
    }

    private fun validateInputs(): Boolean {
        val isValid = binding.activityInput.text?.isNotBlank() == true

        // Parse all three times fresh from input fields
        greenTime = parseTimeToSecondsOrMinutes(binding.greenCardInput.text?.toString() ?: "")
        yellowTime = parseTimeToSecondsOrMinutes(binding.yellowCardInput.text?.toString() ?: "")
        redTime   = parseTimeToSecondsOrMinutes(binding.redCardInput.text?.toString() ?: "")

        // Ensure red time is at least 1 minute (if provided)
        if (redTime <= 0) {
            redTime = 60 // default to 1 minute
        }

        // Validate that all three times are filled
        if (greenTime <= 0 || yellowTime <= 0 || redTime <= 0) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error)
                .setMessage("Please fill Green, Yellow, and Red times.")
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return false
        }

        // Ensure they are all different
        if (greenTime == yellowTime || yellowTime == redTime || greenTime == redTime) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error)
                .setMessage("Green, Yellow, and Red times must be different.")
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return false
        }

        return isValid
    }


    private fun populateFields(item: AgendaItemDto) {
        binding.apply {
            activityInput.setText(item.activity)
            presenterInput.setText(item.presenterName)
            timeInput.setText(item.time ?: timeFormat.format(selectedTime.time))

            // Set card times if they exist
            if (item.greenTime > 0) {
                greenCardInput.setText(formatSecondsToTime(item.greenTime))
            }
            if (item.yellowTime > 0) {
                yellowCardInput.setText(formatSecondsToTime(item.yellowTime))
            }
            // Set red time from item, or empty if not set
            redTime = if (item.redTime > 0) item.redTime else 0
            redCardInput.setText(if (item.redTime > 0) formatSecondsToTime(redTime) else "")

            // Parse the time if it exists
            item.time?.let { timeString ->
                try {
                    val time = timeFormat.parse(timeString)
                    time?.let {
                        selectedTime.time = it
                    }
                } catch (e: Exception) {
                    Log.e("AgendaItemDialog", "Error parsing time: ${e.message}")
                }
            }
        }
    }

    private fun showError() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error)
            .setMessage(R.string.please_fill_all_fields)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            val params = window.attributes
            val displayMetrics = resources.displayMetrics

            params.width = (displayMetrics.widthPixels * 0.90f).toInt()
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT

            window.attributes = params
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun parseTimeToSecondsOrMinutes(timeString: String): Int {
        if (timeString.isBlank()) return 0

        return if (timeString.contains(':')) {
            try {
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    val minutes = parts[0].trim().toIntOrNull() ?: 0
                    val seconds = parts[1].trim().toIntOrNull() ?: 0
                    (minutes * 60) + seconds
                } else {
                    0
                }
            } catch (e: Exception) {
                Log.e("AgendaItemDialog", "Error parsing time: $timeString", e)
                0
            }
        } else {
            // Assume it's just minutes
            (timeString.trim().toIntOrNull() ?: 0) * 60
        }
    }

    private fun formatSecondsToTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (remainingSeconds > 0) {
            "$minutes:$remainingSeconds"
        } else {
            "$minutes"
        }
    }
    
    private fun fetchSpeakerDetailsAndSave(memberName: String, timeString: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("AgendaItemDialog", "Fetching speaker details for member: $memberName in meeting: $meetingId")
                
                // Get all speaker details for the meeting using direct suspend function
                val speakerDetailsList = meetingRepository.getSpeakerDetailsForMeetingDirect(meetingId)
                Log.d("AgendaItemDialog", "Found ${speakerDetailsList.size} speaker details")
                
                // Log all speaker details for debugging
                speakerDetailsList.forEach { details ->
                    Log.d("AgendaItemDialog", "Speaker: ${details.name}, Track: ${details.pathwaysTrack}, Level: ${details.level}, Project: ${details.projectNumber}")
                }
                
                // Find speaker details for the selected member
                val speakerDetails = speakerDetailsList.find { it.name.equals(memberName, ignoreCase = true) }
                
                if (speakerDetails != null) {
                    Log.d("AgendaItemDialog", "Found speaker details: track=${speakerDetails.pathwaysTrack}, level=${speakerDetails.level}, project=${speakerDetails.projectNumber}")
                    saveAgendaItem(
                        memberName, 
                        timeString, 
                        speakerDetails.pathwaysTrack, 
                        speakerDetails.level, 
                        speakerDetails.projectNumber
                    )
                } else {
                    Log.w("AgendaItemDialog", "No speaker details found for member: $memberName")
                    Log.w("AgendaItemDialog", "Available speakers: ${speakerDetailsList.map { it.name }}")
                    saveAgendaItem(memberName, timeString, "", 0, "")
                }
            } catch (e: Exception) {
                Log.e("AgendaItemDialog", "Error fetching speaker details", e)
                saveAgendaItem(memberName, timeString, "", 0, "")
            }
        }
    }
    
    private fun saveAgendaItem(
        memberName: String, 
        timeString: String, 
        pathwaysTrack: String, 
        level: Int, 
        projectNumber: String
    ) {
        Log.d("AgendaItemDialog", "Saving agenda item with speaker details: track=$pathwaysTrack, level=$level, project=$projectNumber")
        
        val updatedItem = agendaItem?.copy(
            activity = binding.activityInput.text.toString(),
            presenterName = memberName,
            greenTime = parseTimeToSecondsOrMinutes(binding.greenCardInput.text.toString()),
            yellowTime = parseTimeToSecondsOrMinutes(binding.yellowCardInput.text.toString()),
            redTime = parseTimeToSecondsOrMinutes(binding.redCardInput.text.toString()),
            time = timeString,
            speakerPathwaysTrack = pathwaysTrack,
            speakerLevel = level,
            speakerProjectNumber = projectNumber
        ) ?: AgendaItemDto(
            id = "",
            meetingId = (parentFragment as? AgendaTableFragment)?.meetingId ?: "",
            activity = binding.activityInput.text.toString(),
            presenterName = memberName,
            greenTime = parseTimeToSecondsOrMinutes(binding.greenCardInput.text.toString()),
            yellowTime = parseTimeToSecondsOrMinutes(binding.yellowCardInput.text.toString()),
            redTime = parseTimeToSecondsOrMinutes(binding.redCardInput.text.toString()),
            time = timeString,
            speakerPathwaysTrack = pathwaysTrack,
            speakerLevel = level,
            speakerProjectNumber = projectNumber
        )

        val finalItem = updatedItem.copy(
            meetingId = updatedItem.meetingId.ifEmpty {
                (parentFragment as? AgendaTableFragment)?.meetingId ?: ""
            }
        )

        onSaveClickListener?.invoke(finalItem)
        dismiss()
    }
}
