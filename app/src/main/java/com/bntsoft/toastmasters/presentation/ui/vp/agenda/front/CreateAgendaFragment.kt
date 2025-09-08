package com.bntsoft.toastmasters.presentation.ui.vp.agenda.front

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentCreateAgendaBinding
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        meetingId = arguments?.getString(ARG_MEETING_ID) ?: run {
            findNavController().navigateUp()
            return
        }

        // Post the setup to ensure view hierarchy is ready
        view.post {
            setupClickListeners()
            setupMeetingDetails()
            observeViewModel()
            viewModel.loadMeeting(meetingId)
        }
    }


    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            showConfirmationDialog()
        }
        
        binding.agendaItemsButton.setOnClickListener {
            findNavController().navigate(
                CreateAgendaFragmentDirections.actionCreateAgendaFragmentToAgendaTableFragment(
                    meetingId = meetingId
                )
            )
        }
        
        binding.editClubInfoButton.setOnClickListener {
            viewModel.toggleClubInfoEdit()
        }
        
        binding.editOfficersButton.setOnClickListener {
            viewModel.toggleOfficersEdit()
        }
    }

    private fun showConfirmationDialog() {
        context?.let { ctx ->
            MaterialAlertDialogBuilder(ctx)
                .setTitle(getString(R.string.confirm_save_title))
                .setMessage(getString(R.string.confirm_save_message))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    // User confirmed, proceed with save
                    viewModel.saveAgenda()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .setOnDismissListener {}
                .show()
        }
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
                    
                    // Update edit mode for Club Information section
                    binding.clubNameInputLayout.isEnabled = state.isClubInfoEditable
                    binding.clubNumberInputLayout.isEnabled = state.isClubInfoEditable
                    binding.areaInputLayout.isEnabled = state.isClubInfoEditable
                    binding.districtInputLayout.isEnabled = state.isClubInfoEditable
                    binding.missionInputLayout.isEnabled = state.isClubInfoEditable
                    
                    // Update edit button text and icon for Club Information
                    val clubInfoButtonText = if (state.isClubInfoEditable) 
                        getString(R.string.cancel) else getString(R.string.edit)
                    binding.editClubInfoButton.text = clubInfoButtonText
                    
                    // Update edit mode for Officers section
                    binding.presidentInputLayout.isEnabled = state.isOfficersEditable
                    binding.vpEducationInputLayout.isEnabled = state.isOfficersEditable
                    binding.vpMembershipInputLayout.isEnabled = state.isOfficersEditable
                    binding.secretaryInputLayout.isEnabled = state.isOfficersEditable
                    binding.treasurerInputLayout.isEnabled = state.isOfficersEditable
                    binding.saaInputLayout.isEnabled = state.isOfficersEditable
                    binding.ippInputLayout.isEnabled = state.isOfficersEditable
                    
                    // Update edit button text and icon for Officers
                    val officersButtonText = if (state.isOfficersEditable) 
                        getString(R.string.cancel) else getString(R.string.edit)
                    binding.editOfficersButton.text = officersButtonText

                    if (state.isSaved) {
                        showMessage(getString(R.string.agenda_saved))
                        // Post the navigation to ensure UI updates complete
                        view?.postDelayed({
                            if (isAdded && !isDetached) {
                                // Navigate back to MemberRoleAssignFragment
                                findNavController().navigateUp()
                            }
                        }, 300) // Small delay to ensure UI updates complete
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
        
        // Set up Club Information fields
        binding.clubNameEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateClubName(s?.toString() ?: "")
            }
        })
        
        binding.clubNumberEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateClubNumber(s?.toString() ?: "")
            }
        })
        
        binding.areaEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateArea(s?.toString() ?: "")
            }
        })
        
        binding.districtEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateDistrict(s?.toString() ?: "")
            }
        })
        
        binding.missionEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateClubMission(s?.toString() ?: "")
            }
        })
    }


    private var isInitialLoad = true
    private var currentOfficers: Map<String, String> = emptyMap()

    private fun updateUi(agenda: MeetingAgenda) {
        // Only update theme if it's changed
        if (binding.themeEditText.text?.toString() != agenda.meeting.theme) {
            binding.themeEditText.setText(agenda.meeting.theme)
        }
        
        // Only update date if it's changed
        val formattedDate = agenda.meetingDate?.let { timestamp ->
            dateFormat.format(Date(timestamp.seconds * 1000))
        } ?: ""
        if (binding.dateEditText.text?.toString() != formattedDate) {
            binding.dateEditText.setText(formattedDate)
        }
        
        // Only update times if they're changed
        if (binding.startTimeEditText.text?.toString() != agenda.startTime) {
            binding.startTimeEditText.setText(agenda.startTime)
        }
        if (binding.endTimeEditText.text?.toString() != agenda.endTime) {
            binding.endTimeEditText.setText(agenda.endTime)
        }
        
        // Only update venue if it's changed
        if (binding.venueEditText.text?.toString() != agenda.meeting.location) {
            binding.venueEditText.setText(agenda.meeting.location)
        }

        // Only update officer fields if they've actually changed
        if (currentOfficers != agenda.officers) {
            currentOfficers = agenda.officers
            setupOfficerFields(agenda.officers)
        }
        
        isInitialLoad = false
    }

    private fun setupOfficerFields(officers: Map<String, String>) {
        // Only set up text watchers once
        if (isInitialLoad) {
            binding.ippEditText.setupOfficerTextWatcher("Immediate Past President")
            binding.presidentEditText.setupOfficerTextWatcher("President")
            binding.secretaryEditText.setupOfficerTextWatcher("Secretary")
            binding.saaEditText.setupOfficerTextWatcher("Sergeant at Arms")
            binding.treasurerEditText.setupOfficerTextWatcher("Treasurer")
            binding.vpEducationEditText.setupOfficerTextWatcher("VP Education")
            binding.vpMembershipEditText.setupOfficerTextWatcher("VP Membership")
        }
        // Only update text if it's different from current text
        fun updateField(editText: TextInputEditText, value: String?) {
            if (editText.text?.toString() != value) {
                editText.setText(value ?: "")
            }
        }

        // Update officer fields
        updateField(binding.ippEditText, officers["Immediate Past President"])
        updateField(binding.presidentEditText, officers["President"])
        updateField(binding.secretaryEditText, officers["Secretary"])
        updateField(binding.saaEditText, officers["Sergeant at Arms"])
        updateField(binding.treasurerEditText, officers["Treasurer"])
        updateField(binding.vpEducationEditText, officers["VP Education"])
        updateField(binding.vpMembershipEditText, officers["VP Membership"])
    }

    private fun TextInputEditText.setupOfficerTextWatcher(role: String) {
        // Clear existing text watchers
        this.tag?.let { oldWatcher ->
            if (oldWatcher is android.text.TextWatcher) {
                this.removeTextChangedListener(oldWatcher)
            }
        }

        // Clear existing focus change listeners
        this.onFocusChangeListener = null

        // Add focus change listener
        this.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.updateOfficer(role, this.text.toString())
            }
        }

        // Add text changed listener
        val textWatcher = object : android.text.TextWatcher {
            private var debounceJob: Job? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                debounceJob?.cancel()
                debounceJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(500) // 500ms debounce
                    if (isAttachedToWindow && isLaidOut) {
                        viewModel.updateOfficer(role, s?.toString() ?: "")
                    }
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        // Store the watcher in the view's tag to avoid leaks
        this.tag = textWatcher
        this.addTextChangedListener(textWatcher)
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
        // Clear all view references to prevent memory leaks
        _binding?.let { binding ->
            // Clear any listeners or callbacks here if needed
            binding.saveButton.setOnClickListener(null)
            _binding = null
        }
        super.onDestroyView()
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