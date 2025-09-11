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
        dialog.window?.apply {
            setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            val lp = attributes
            lp.dimAmount = 0f
            attributes = lp
            clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
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
        dialog.window?.apply {
            setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            val lp = attributes
            lp.dimAmount = 0f
            attributes = lp
            clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    fun showTimeBreakDialog(context: Context, listener: OnTimeBreakSetListener) {
        val dialog = Dialog(context)
        val binding = DialogTimeBreakBinding.inflate(LayoutInflater.from(context))
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)

        // Use explicit minutes/seconds NumberPickers from the layout
        val npMinutes = binding.root.findViewById<android.widget.NumberPicker>(R.id.npMinutes)
        val npSeconds = binding.root.findViewById<android.widget.NumberPicker>(R.id.npSeconds)
        npMinutes.minValue = 0
        npMinutes.maxValue = 59
        npMinutes.value = 5
        npMinutes.wrapSelectorWheel = true
        npSeconds.minValue = 0
        npSeconds.maxValue = 59
        npSeconds.value = 0
        npSeconds.wrapSelectorWheel = true

        binding.btnSet.setOnClickListener {
            val minutes = npMinutes.value
            val seconds = npSeconds.value
            listener.onTimeBreakSet(minutes, seconds)
            dialog.dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.apply {
            setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            val lp = attributes
            lp.dimAmount = 0f
            attributes = lp
            clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }
}
