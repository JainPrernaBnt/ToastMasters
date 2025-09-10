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
    @Inject
    lateinit var assignedRoleRepository: AssignedRoleRepository

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
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            // Set dialog width to 90% of screen width
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
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

            val updatedItem = agendaItem?.copy(
                activity = binding.activityInput.text.toString(),
                presenterName = binding.presenterInput.text.toString(),
                greenTime = parseTimeToSecondsOrMinutes(binding.greenCardInput.text.toString()),
                yellowTime = parseTimeToSecondsOrMinutes(binding.yellowCardInput.text.toString()),
                redTime = parseTimeToSecondsOrMinutes(binding.redCardInput.text.toString()),
                time = timeString
            ) ?: AgendaItemDto(
                id = "",
                meetingId = (parentFragment as? AgendaTableFragment)?.meetingId ?: "",
                activity = binding.activityInput.text.toString(),
                presenterName = binding.presenterInput.text.toString(),
                greenTime = parseTimeToSecondsOrMinutes(binding.greenCardInput.text.toString()),
                yellowTime = parseTimeToSecondsOrMinutes(binding.yellowCardInput.text.toString()),
                redTime = parseTimeToSecondsOrMinutes(binding.redCardInput.text.toString()),
                time = timeString
            )

            val finalItem = updatedItem.copy(
                meetingId = updatedItem.meetingId.ifEmpty {
                    (parentFragment as? AgendaTableFragment)?.meetingId ?: ""
                }
            )

            onSaveClickListener?.invoke(finalItem)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setupTimePicker() {
        binding.timeInput.apply {
            setOnClickListener { showTimePicker() }
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showTimePicker() }
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
                            result.data?.forEach { assignedRole ->
                                assignedRole.roles.forEach { role ->
                                    val displayText = "${assignedRole.memberName} - $role"
                                    if (!presenters.contains(displayText)) {
                                        presenters.add(displayText)
                                    }
                                }
                            }

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
        return binding.run {
            val timeValid = !timeInput.text.isNullOrBlank()
            val redValid = !redCardInput.text.isNullOrBlank() &&
                    parseTimeToSecondsOrMinutes(redCardInput.text.toString()) > 0

            var allValid = true

            // Time validation
            if (!timeValid) {
                timeInput.error = "Please select a valid time"
                allValid = false
            } else {
                timeInput.error = null
            }

            // Red card validation (required)
            if (!redValid) {
                redCardInput.error = "Please enter a valid number"
                allValid = false
            } else {
                redCardInput.error = null
            }

            // Green and Yellow are optional → clear any error
            greenCardInput.error = null
            yellowCardInput.error = null

            allValid && !activityInput.text.isNullOrBlank() && !presenterInput.text.isNullOrBlank()
        }
    }

    private fun populateFields(agendaItem: AgendaItemDto) {
        binding.apply {
            activityInput.setText(agendaItem.activity)
            presenterInput.setText(agendaItem.presenterName)
            // Convert seconds to minutes for display
            greenCardInput.setText((agendaItem.greenTime / 60).toString())
            yellowCardInput.setText((agendaItem.yellowTime / 60).toString())
            redCardInput.setText((agendaItem.redTime / 60).toString())

            // Set time if available, otherwise use current time
            if (agendaItem.time.isNotEmpty()) {
                try {
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(agendaItem.time)
                    time?.let {
                        val calendar = Calendar.getInstance()
                        calendar.time = it
                        selectedTime = calendar
                        timeInput.setText(timeFormat.format(it))
                    } ?: run {
                        timeInput.setText(timeFormat.format(selectedTime.time))
                    }
                } catch (e: Exception) {
                    timeInput.setText(timeFormat.format(selectedTime.time))
                }
            } else {
                timeInput.setText(timeFormat.format(selectedTime.time))
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
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun parseTimeToSecondsOrMinutes(timeString: String): Int {
        val trimmed = timeString.trim()
        if (trimmed.isEmpty()) return 0

        return if (trimmed.contains(":")) {
            val parts = trimmed.split(":")
            val minutes = parts[0].toIntOrNull() ?: 0
            val seconds = parts.getOrNull(1)?.toIntOrNull() ?: 0
            (minutes * 60) + seconds
        } else {
            // Just a number (assume minutes)
            trimmed.toIntOrNull()?.times(60) ?: 0
        }
    }

}
