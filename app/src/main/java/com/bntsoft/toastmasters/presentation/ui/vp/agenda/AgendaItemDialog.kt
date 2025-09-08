package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
import com.bntsoft.toastmasters.databinding.DialogAddEditAgendaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgendaItemDialog : DialogFragment() {
    private var _binding: DialogAddEditAgendaBinding? = null
    private val binding get() = _binding!!
    private var onSaveClickListener: ((AgendaItemDto) -> Unit)? = null
    private var agendaItem: AgendaItemDto? = null
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private var selectedTime: Calendar = Calendar.getInstance()

    companion object {
        private const val ARG_AGENDA_ITEM = "agenda_item"

        fun newInstance(
            agendaItem: AgendaItemDto? = null,
            onSave: (AgendaItemDto) -> Unit
        ): AgendaItemDialog {
            return AgendaItemDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_AGENDA_ITEM, agendaItem)
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

        setupTimePicker()
        setupPresenters()

        arguments?.getParcelable<AgendaItemDto>(ARG_AGENDA_ITEM)?.let { item ->
            agendaItem = item
            binding.apply {
                activityInput.setText(item.activity)
                presenterInput.setText(item.presenterName)
                greenCardInput.setText(if (item.greenTime > 0) item.greenTime.toString() else "")
                yellowCardInput.setText(if (item.yellowTime > 0) item.yellowTime.toString() else "")
                redCardInput.setText(if (item.redTime > 0) item.redTime.toString() else "")

                // Set time if available
                if (item.time.isNotBlank()) {
                    val time = timeFormat.parse(item.time)
                    time?.let {
                        selectedTime.time = it
                        timeInput.setText(timeFormat.format(it))
                    }
                }
            }
        }

        // In AgendaItemDialog.kt

        binding.btnSave.setOnClickListener {
            if (!validateInputs()) {
                showError()
                return@setOnClickListener
            }

            val timeString = timeFormat.format(selectedTime.time)

            val updatedItem = agendaItem?.copy(
                activity = binding.activityInput.text.toString(),
                presenterName = binding.presenterInput.text.toString(),
                greenTime = binding.greenCardInput.text.toString().toIntOrNull() ?: 0,
                yellowTime = binding.yellowCardInput.text.toString().toIntOrNull() ?: 0,
                redTime = binding.redCardInput.text.toString().toIntOrNull() ?: 0,
                time = timeString
            ) ?: AgendaItemDto(
                id = "", // Will be set by the parent fragment
                meetingId = (parentFragment as? AgendaTableFragment)?.meetingId ?: "",
                activity = binding.activityInput.text.toString(),
                presenterName = binding.presenterInput.text.toString(),
                greenTime = binding.greenCardInput.text.toString().toIntOrNull() ?: 0,
                yellowTime = binding.yellowCardInput.text.toString().toIntOrNull() ?: 0,
                redTime = binding.redCardInput.text.toString().toIntOrNull() ?: 0,
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
        binding.timeInput.setOnClickListener {
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

        // Set initial time if available
        if (binding.timeInput.text.isNullOrEmpty()) {
            binding.timeInput.setText(timeFormat.format(selectedTime.time))
        }
    }

    private fun setupPresenters() {
        val presenters = listOf("John Doe", "Jane Smith", "Mike Johnson")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            presenters
        )
        binding.presenterInput.setAdapter(adapter)
    }

    private fun validateInputs(): Boolean {
        return binding.run {
            val timeValid = timeInput.text?.toString()
                ?.matches(Regex("^(0?[1-9]|1[0-2]):[0-5][0-9] [APMapm]{2}$")) ?: false
            val greenValid = greenCardInput.text?.toString()
                ?.matches(Regex("^([0-9]|0[0-9]|1[0-9]|2[0-3])(:[0-5][0-9])?$")) ?: false
            val yellowValid = yellowCardInput.text?.toString()
                ?.matches(Regex("^([0-9]|0[0-9]|1[0-9]|2[0-3])(:[0-5][0-9])?$")) ?: false
            val redValid = redCardInput.text?.toString()
                ?.matches(Regex("^([0-9]|0[0-9]|1[0-9]|2[0-3])(:[0-5][0-9])?$")) ?: false

            var allValid = true

            if (!timeValid) {
                timeInput.error = "Invalid time format (HH:MM AM/PM)"
                allValid = false
            } else {
                timeInput.error = null
            }

            if (!greenValid) {
                greenCardInput.error = "Invalid format (e.g., 2 or 02:30)"
                allValid = false
            } else {
                greenCardInput.error = null
            }

            if (!yellowValid) {
                yellowCardInput.error = "Invalid format (e.g., 2 or 02:30)"
                allValid = false
            } else {
                yellowCardInput.error = null
            }

            if (!redValid) {
                redCardInput.error = "Invalid format (e.g., 2 or 02:30)"
                allValid = false
            } else {
                redCardInput.error = null
            }

            allValid && !activityInput.text.isNullOrBlank() && !presenterInput.text.isNullOrBlank()
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
}
