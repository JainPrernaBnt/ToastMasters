package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentCreateAgendaBinding
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreateAgendaFragment : Fragment() {

    private var _binding: FragmentCreateAgendaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreateAgendaViewModel by viewModels()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val calendar: Calendar by lazy { Calendar.getInstance() }
    private lateinit var meetingId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateAgendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        meetingId = arguments?.getString(ARG_MEETING_ID) ?: return

        setupClickListeners()
        setupMeetingDetails()
        observeViewModel()
        viewModel.loadMeeting(meetingId)
    }


    private fun setupClickListeners() {
        // No click listeners for read-only fields
        binding.saveButton.setOnClickListener { viewModel.saveAgenda() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.saveButton.isEnabled = !state.isLoading && !state.isSaving
                    binding.saveButton.text =
                        if (state.isSaving) getString(R.string.saving) else getString(
                            R.string.save
                        )

                    if (state.isSaved) {
                        showMessage(getString(R.string.agenda_saved))
                    }

                    state.error?.let { error ->
                        showError(error)
                        viewModel.clearError()
                    }

                    updateUi(state.agenda)
                }
            }
        }
    }

    private fun setupMeetingDetails() {
        // Meeting details will be loaded from the ViewModel
        binding.themeEditText.apply {
            isEnabled = false
            isFocusable = false
            isClickable = false
            background = null
        }

        binding.dateEditText.apply {
            isEnabled = false
            isFocusable = false
            isClickable = false
            background = null
        }

        binding.startTimeEditText.apply {
            isEnabled = false
            isFocusable = false
            isClickable = false
            background = null
        }

        binding.endTimeEditText.apply {
            isEnabled = false
            isFocusable = false
            isClickable = false
            background = null
        }

        binding.venueEditText.apply {
            isEnabled = false
            isFocusable = false
            isClickable = false
            background = null
        }
    }

    private val officerRoles = listOf(
        "President",
        "VP Education",
        "VP Membership",
        "VP Public Relations",
        "SAA",
        "Secretary",
        "Treasurer",
        "Immediate Past President"
    )

    private fun updateUi(agenda: MeetingAgenda) {
        // Update meeting details
        binding.themeEditText.setText(agenda.meeting.theme)
        agenda.meetingDate?.let { timestamp ->
            binding.dateEditText.setText(dateFormat.format(Date(timestamp.seconds * 1000)))
        }
        binding.startTimeEditText.setText(agenda.startTime)
        binding.endTimeEditText.setText(agenda.endTime)
        binding.venueEditText.setText(agenda.meeting.location)

        // Update officers list
        binding.officersContainer.removeAllViews()
        officerRoles.forEach { role ->
            val name = agenda.officers[role] ?: ""
            addOfficerInput(role, name)
        }
    }

    private fun addOfficerInput(role: String, name: String) {
        // Create a new TextInputLayout programmatically
        val inputLayout = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.spacing_small)
                marginEnd = resources.getDimensionPixelSize(R.dimen.spacing_small)
                bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_medium)
            }
            hint = role
            isHintEnabled = true
            boxBackgroundMode =
                com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = Color.TRANSPARENT
            setHintTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Medium)

            // Add TextInputEditText
            addView(TextInputEditText(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setText(name)
                setTextAppearance(android.R.style.TextAppearance_Medium)
                textSize = 16f

                // Update the officer name when focus is lost
                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        viewModel.updateOfficer(role, text.toString())
                    }
                }
            })
        }

        binding.officersContainer.addView(inputLayout)
    }


    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.error_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MEETING_ID = "meeting_id"

        fun newInstance(meetingId: String): CreateAgendaFragment {
            return CreateAgendaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MEETING_ID, meetingId)
                }
            }
        }
    }
}