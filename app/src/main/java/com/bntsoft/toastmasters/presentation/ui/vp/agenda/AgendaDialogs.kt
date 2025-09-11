package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Toast
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
import com.bntsoft.toastmasters.databinding.DialogCustomSessionBinding
import com.bntsoft.toastmasters.databinding.DialogSessionSelectionBinding
import com.bntsoft.toastmasters.databinding.DialogTimeBreakBinding
import java.util.UUID

object AgendaDialogs {

    interface OnSessionSelectedListener {
        fun onSessionSelected(sessionName: String)
    }

    interface OnTimeBreakSetListener {
        fun onTimeBreakSet(minutes: Int, seconds: Int)
    }

    fun showSessionSelectionDialog(context: Context, listener: OnSessionSelectedListener) {
        val dialog = Dialog(context)
        val binding = DialogSessionSelectionBinding.inflate(LayoutInflater.from(context))
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)

        binding.btnPreparedSpeeches.setOnClickListener {
            listener.onSessionSelected("PREPARED SPEECHES SESSION")
            dialog.dismiss()
        }

        binding.btnEvaluation.setOnClickListener {
            listener.onSessionSelected("EVALUATION SESSION")
            dialog.dismiss()
        }

        binding.btnRolePlayers.setOnClickListener {
            listener.onSessionSelected("ROLE PLAYERS REPORT SESSION")
            dialog.dismiss()
        }

        binding.btnCustomSession.setOnClickListener {
            dialog.dismiss()
            showCustomSessionDialog(context, listener)
        }

        dialog.show()
    }

    private fun showCustomSessionDialog(context: Context, listener: OnSessionSelectedListener) {
        val dialog = Dialog(context)
        val binding = DialogCustomSessionBinding.inflate(LayoutInflater.from(context))
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)

        binding.btnAdd.setOnClickListener {
            val sessionName = binding.etSessionName.text.toString().trim()
            if (sessionName.isNotEmpty()) {
                listener.onSessionSelected(sessionName)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Please enter a session name", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showTimeBreakDialog(context: Context, listener: OnTimeBreakSetListener) {
        val dialog = Dialog(context)
        val binding = DialogTimeBreakBinding.inflate(LayoutInflater.from(context))
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)

        // Set time picker to only show minutes and seconds
        binding.timePicker.setIs24HourView(true)
        // Set up time picker to only show minutes and seconds
        binding.timePicker.hour = 0
        binding.timePicker.minute = 5
        binding.timePicker.setIs24HourView(true)  // Use 24-hour format for consistency
        binding.timePicker.visibility = View.VISIBLE
        
        // Since TimePicker doesn't directly support seconds, we'll use a workaround
        // by treating the minute picker as seconds when needed
        var isSelectingSeconds = false
        var selectedMinutes = 0
        
        binding.timePicker.setOnTimeChangedListener { _, hour, minute ->
            if (hour > 0) {
                binding.timePicker.hour = 0
            }
            if (!isSelectingSeconds) {
                selectedMinutes = minute
                binding.timePicker.minute = 0  // Reset to 0 for seconds selection
                isSelectingSeconds = true
            }
        }

        binding.btnSet.setOnClickListener {
            val minutes = if (isSelectingSeconds) selectedMinutes else binding.timePicker.minute
            val seconds = if (isSelectingSeconds) binding.timePicker.minute else 0
            listener.onTimeBreakSet(minutes, seconds)
            dialog.dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
