package com.bntsoft.toastmasters.presentation.ui.vp.agenda.front

import android.content.Context
import android.util.Log
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.AbbreviationItem
import com.bntsoft.toastmasters.databinding.FragmentCreateAgendaBinding
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.presentation.ui.vp.agenda.front.viewmodel.CreateAgendaViewModel
import com.bntsoft.toastmasters.utils.Result
import com.bntsoft.toastmasters.utils.UserManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@AndroidEntryPoint
class CreateAgendaFragment : Fragment() {

    private var _binding: FragmentCreateAgendaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreateAgendaViewModel by viewModels()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private lateinit var meetingId: String
    private var currentStep = 1

    @Inject
    lateinit var userManager: UserManager
    private var isVpEducation: Boolean = false

    private lateinit var abbreviationAdapter: AbbreviationAdapter

    private var isAbbreviationEditing = false

    private fun setupAbbreviationAdapter() {
        abbreviationAdapter = AbbreviationAdapter(
            onItemRemoved = { position ->
                val currentList = abbreviationAdapter.currentList.toMutableList()
                if (position >= 0 && position < currentList.size) {
                    val itemToRemove = currentList[position]
                    currentList.removeAt(position)
                    abbreviationAdapter.submitList(currentList)

                    // If the item has an abbreviation, delete it from Firebase
                    if (itemToRemove.abbreviation.isNotBlank()) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            Log.d("CreateAgendaFragment", "Deleting abbreviation: ${itemToRemove.abbreviation}")
                            val result = viewModel.deleteAbbreviation(meetingId, itemToRemove.abbreviation)
                            when (result) {
                                is Result.Success -> {
                                    Log.d("CreateAgendaFragment", "Abbreviation deleted successfully from Firebase")
                                    showMessage("Abbreviation deleted successfully")
                                }
                                is Result.Error -> {
                                    Log.e("CreateAgendaFragment", "Failed to delete abbreviation: ${result.exception?.message}")
                                    showError("Failed to delete abbreviation: ${result.exception?.message}")
                                }
                                else -> {
                                    Log.e("CreateAgendaFragment", "Unknown error deleting abbreviation")
                                    showError("Failed to delete abbreviation")
                                }
                            }
                        }
                    }

                    updateAbbreviationUI()
                }
            }
        )
    }

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
            updateStepView()
            setupAbbreviationsRecyclerView()
            setupClickListeners()
            setupMeetingDetails()
            observeViewModel()
            viewModel.loadMeeting(meetingId)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    isVpEducation = userManager.isVpEducation()
                    Log.d("CreateAgendaFragment", "VP Education status: $isVpEducation")
                    applyRoleBasedAccess()
                } catch (e: Exception) { 
                    Log.e("CreateAgendaFragment", "Error checking VP Education status", e)
                }
            }
        }
    }

    private fun applyRoleBasedAccess() {
        Log.d("CreateAgendaFragment", "Applying role-based access. isVpEducation: $isVpEducation")
        if (!isVpEducation) {
            // Disable club information editing for non-VP Education users
            binding.editClubInfoButton.visibility = View.GONE
            binding.saveClubInfoButton.visibility = View.GONE
            binding.cancelClubInfoButton.visibility = View.GONE

            binding.clubNameEditText.isEnabled = false
            binding.clubNumberEditText.isEnabled = false
            binding.districtEditText.isEnabled = false
            binding.areaEditText.isEnabled = false
            binding.missionEditText.isEnabled = false

            // Disable officers editing for non-VP Education users
            binding.presidentInputLayout.isEnabled = false
            binding.vpEducationInputLayout.isEnabled = false
            binding.vpMembershipInputLayout.isEnabled = false
            binding.secretaryInputLayout.isEnabled = false
            binding.treasurerInputLayout.isEnabled = false
            binding.saaInputLayout.isEnabled = false
            binding.ippInputLayout.isEnabled = false
            binding.vpPublicRelationInputLayout.isEnabled = false

            binding.editOfficersButton.visibility = View.GONE
            binding.saveOfficersButton.visibility = View.GONE
            binding.cancelOfficersButton.visibility = View.GONE

            // Disable grammarian editing for non-VP Education users
            binding.wordOfDayEditText.isEnabled = false
            binding.wordMeaningEditText.isEnabled = false
            binding.wordExamplesEditText.isEnabled = false
            binding.idiomEditText.isEnabled = false
            binding.idiomMeaningEditText.isEnabled = false
            binding.idiomExamplesEditText.isEnabled = false

            binding.editGrammarianButton.visibility = View.GONE
            binding.saveGrammarianButton.visibility = View.GONE
            binding.cancelGrammarianButton.visibility = View.GONE

            // Disable abbreviations editing for non-VP Education users
            isAbbreviationEditing = false
            binding.abbreviationsSummaryText.isClickable = false
            binding.saveAbbreviationsButton.visibility = View.GONE
            binding.addAbbreviationsButton.visibility = View.GONE
            abbreviationAdapter.setEditable(false)
        } else {
            // Enable functionality for VP Education users
            Log.d("CreateAgendaFragment", "Enabling functionality for VP Education user")
            binding.abbreviationsSummaryText.isClickable = true
            
            // For VP Education users, fields should start disabled (read mode) and be enabled only in edit mode
            // This will be controlled by the observeViewModel() method based on edit state
            binding.clubNameEditText.isEnabled = false
            binding.clubNumberEditText.isEnabled = false
            binding.districtEditText.isEnabled = false
            binding.areaEditText.isEnabled = false
            binding.missionEditText.isEnabled = false
            
            binding.presidentEditText.isEnabled = false
            binding.vpEducationEditText.isEnabled = false
            binding.vpMembershipEditText.isEnabled = false
            binding.secretaryEditText.isEnabled = false
            binding.treasurerEditText.isEnabled = false
            binding.saaEditText.isEnabled = false
            binding.ippEditText.isEnabled = false
            binding.vpPublicRelationEditText.isEnabled = false
        }
    }

    private fun setupClickListeners() {

        binding.nextButton.setOnClickListener {
            if (currentStep < 5) {
                currentStep++
                updateStepView()
            }
        }

        binding.previousButton.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepView()
            }
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
        binding.saveClubInfoButton.setOnClickListener {
            // Ensure all fields (even unedited/static) are captured before saving
            viewModel.updateClubName(
                binding.clubNameEditText.text?.toString().orEmpty()
            )
            viewModel.updateClubNumber(
                binding.clubNumberEditText.text?.toString().orEmpty()
            )
            viewModel.updateDistrict(
                binding.districtEditText.text?.toString().orEmpty()
            )
            viewModel.updateArea(binding.areaEditText.text?.toString().orEmpty())
            viewModel.updateClubMission(
                binding.missionEditText.text?.toString().orEmpty()
            )

            viewModel.saveClubInfo()
        }
        binding.cancelClubInfoButton.setOnClickListener {
            viewModel.toggleClubInfoEdit()
            // Optionally reset fields to ViewModel values
            updateUi(viewModel.uiState.value.agenda)
        }

        binding.editOfficersButton.setOnClickListener {
            viewModel.toggleOfficersEdit()
        }

        binding.saveOfficersButton.setOnClickListener {
            Log.d("CreateAgendaFragment", "Save officers button clicked")
            
            // Capture all officer fields from UI to ensure full save even if only one was edited
            val presidentValue = binding.presidentEditText.text?.toString().orEmpty()
            val vpeValue = binding.vpEducationEditText.text?.toString().orEmpty()
            val vpmValue = binding.vpMembershipEditText.text?.toString().orEmpty()
            val secretaryValue = binding.secretaryEditText.text?.toString().orEmpty()
            val treasurerValue = binding.treasurerEditText.text?.toString().orEmpty()
            val saaValue = binding.saaEditText.text?.toString().orEmpty()
            val ippValue = binding.ippEditText.text?.toString().orEmpty()
            val vpprValue = binding.vpPublicRelationEditText.text?.toString().orEmpty()
            
            Log.d("CreateAgendaFragment", "Officer values - President: $presidentValue, VPE: $vpeValue, VPM: $vpmValue, Secretary: $secretaryValue, Treasurer: $treasurerValue, SAA: $saaValue, IPP: $ippValue, VPPR: $vpprValue")
            
            // Update all officers synchronously to avoid race conditions
            viewModel.updateAllOfficers(mapOf(
                "President" to presidentValue,
                "VPE" to vpeValue,
                "VPM" to vpmValue,
                "Secretary" to secretaryValue,
                "Treasurer" to treasurerValue,
                "SAA" to saaValue,
                "IPP" to ippValue,
                "VPPR" to vpprValue
            ))

            // Add a small delay to ensure state is updated before saving
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100) // Small delay to ensure state update completes
                Log.d("CreateAgendaFragment", "Calling viewModel.saveOfficers()")
                viewModel.saveOfficers()
            }
        }
        binding.cancelOfficersButton.setOnClickListener {
            viewModel.toggleOfficersEdit()
            // Optionally reset fields to ViewModel values
            updateUi(viewModel.uiState.value.agenda)
        }

        binding.editGrammarianButton.setOnClickListener {
            toggleGrammarianEditMode(true)
        }

        binding.cancelGrammarianButton.setOnClickListener {
            toggleGrammarianEditMode(false)
            // Reset to current values from ViewModel
            viewModel.uiState.value.grammarianDetails.let { details ->
                binding.wordOfDayEditText.setText(details.wordOfTheDay)
                binding.wordMeaningEditText.setText(
                    details.wordMeaning.joinToString(
                        "\n"
                    )
                )
                binding.wordExamplesEditText.setText(
                    details.wordExamples.joinToString(
                        "\n"
                    )
                )
                binding.idiomEditText.setText(details.idiomOfTheDay)
                binding.idiomMeaningEditText.setText(details.idiomMeaning)
                binding.idiomExamplesEditText.setText(
                    details.idiomExamples.joinToString(
                        "\n"
                    )
                )
            }
        }

        binding.saveGrammarianButton.setOnClickListener {
            updateAndSaveGrammarianDetails()
        }

        // Abbreviation button click listeners are set up in setupAbbreviationsRecyclerView()
    }

    private fun setupAbbreviationsRecyclerView() {
        setupAbbreviationAdapter()
        binding.abbreviationsRecyclerView.adapter = abbreviationAdapter

        // Switch to edit mode on tapping summary
        binding.abbreviationsSummaryText.setOnClickListener {
            Log.d("CreateAgendaFragment", "Abbreviation summary clicked. isAbbreviationEditing: $isAbbreviationEditing, isVpEducation: $isVpEducation, isClickable: ${binding.abbreviationsSummaryText.isClickable}")
            if (!isAbbreviationEditing) {
                editAbbreviations()
            }
        }

        binding.addAbbreviationsButton.setOnClickListener {
            addNewAbbreviationItem()
        }

        binding.saveAbbreviationsButton.setOnClickListener {
            saveAbbreviations()
        }

        // Collect StateFlow from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.abbreviations.collect { abbreviations: Map<String, String> ->
                    val items = abbreviations.map { entry ->
                        AbbreviationItem(
                            id = entry.key,
                            abbreviation = entry.key,
                            meaning = entry.value,
                            isEditable = isAbbreviationEditing
                        )
                    }

                    abbreviationAdapter.submitList(items)
                    updateAbbreviationUI()
                }
            }
        }

        // Initial load
        viewModel.loadAbbreviations(meetingId)
    }

    private fun addNewAbbreviationItem() {
        val currentList = abbreviationAdapter.currentList.toMutableList()
        currentList.add(
            AbbreviationItem(
                id = UUID.randomUUID().toString(),
                abbreviation = "",
                meaning = "",
                isEditable = true
            )
        )
        abbreviationAdapter.submitList(currentList) {
            binding.abbreviationsRecyclerView.smoothScrollToPosition(currentList.size - 1)
        }
        updateAbbreviationUI()
    }

    private fun updateAbbreviationUI() {
        val hasItems = abbreviationAdapter.currentList.isNotEmpty()

        with(binding) {
            if (isAbbreviationEditing) {
                // In edit mode: show RecyclerView, hide summary, show buttons
                abbreviationsRecyclerView.isVisible = true
                abbreviationsSummaryText.isVisible = false
                saveAbbreviationsButton.isVisible = true
                addAbbreviationsButton.isVisible = true
            } else {
                // In read mode: hide RecyclerView, show summary
                abbreviationsRecyclerView.isVisible = false
                abbreviationsSummaryText.isVisible = true
                saveAbbreviationsButton.isVisible = false
                addAbbreviationsButton.isVisible = false
                
                if (hasItems) {
                    // Show abbreviations in summary format
                    val abbreviationsText = abbreviationAdapter.currentList
                        .filter { it.abbreviation.isNotBlank() && it.meaning.isNotBlank() }
                        .joinToString(", ") { "${it.abbreviation} (${it.meaning})" }
                    abbreviationsSummaryText.text = abbreviationsText
                } else {
                    // Show empty state message
                    abbreviationsSummaryText.text = "No abbreviations added. Tap to add."
                }
            }
        }
    }

    private fun editAbbreviations() {
        Log.d("CreateAgendaFragment", "Entering edit abbreviations mode")
        isAbbreviationEditing = true
        val currentList = abbreviationAdapter.currentList.toMutableList()

        if (currentList.isEmpty()) {
            Log.d("CreateAgendaFragment", "No abbreviations found, adding new item")
            addNewAbbreviationItem()
        } else {
            Log.d("CreateAgendaFragment", "Found ${currentList.size} abbreviations, making them editable")
            val updatedList = currentList.map { it.copy(isEditable = true) }
            abbreviationAdapter.submitList(updatedList)
        }

        updateAbbreviationUI()
    }

    private fun saveAbbreviations() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentList = abbreviationAdapter.currentList
                val abbreviations = currentList
                    .filter { it.abbreviation.isNotBlank() && it.meaning.isNotBlank() }
                    .associate { it.abbreviation to it.meaning }

                if (abbreviations.isEmpty()) {
                    showError("Please add at least one abbreviation")
                    return@launch
                }

                val agendaId = viewModel.uiState.value.agenda?.id ?: ""
                val result = viewModel.saveAbbreviations(meetingId, agendaId, abbreviations)

                when (result) {
                    is Result.Success -> {
                        // Switch back to read mode
                        isAbbreviationEditing = false

                        // Reload the agenda to get updated abbreviations from ViewModel
                        val currentAgenda = viewModel.uiState.value.agenda
                        if (currentAgenda != null) {
                            // Update the agenda with the saved abbreviations
                            val updatedAgenda = currentAgenda.copy(abbreviations = abbreviations)
                            
                            // Convert to AbbreviationItems for display
                            val items = abbreviations.map { (abbr, meaning) ->
                                AbbreviationItem(
                                    id = abbr,
                                    abbreviation = abbr,
                                    meaning = meaning,
                                    isEditable = false
                                )
                            }
                            abbreviationAdapter.submitList(items)
                        }

                        updateAbbreviationUI()
                        showMessage("Abbreviations saved successfully")
                    }
                    is Result.Error -> {
                        showError("Failed to save abbreviations: ${result.exception?.message}")
                    }
                    else -> {
                        showError("Failed to save abbreviations")
                    }
                }
            } catch (e: Exception) {
                showError("Failed to save abbreviations: ${e.message}")
            }
        }
    }


    private fun observeViewModel() {
        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    withContext(Dispatchers.Main) {
                        // Handle loading state
                        binding.progressBar.isVisible = state.isLoading

                        // Handle error state
                        state.error?.let { error ->
                            showError(error)
                            viewModel.clearError()
                        }

                        // Update UI with meeting data
                        state.agenda?.let { agenda ->
                            updateUi(agenda)

                            // Load abbreviations if not already loaded
                            if (abbreviationAdapter.itemCount == 0) {
                                viewModel.loadAbbreviations(meetingId)
                            }

                            // Update edit mode for Officers section
                            binding.presidentInputLayout.isEnabled =
                                state.isOfficersEditable
                            binding.vpEducationInputLayout.isEnabled =
                                state.isOfficersEditable
                            binding.vpMembershipInputLayout.isEnabled =
                                state.isOfficersEditable
                            binding.secretaryInputLayout.isEnabled =
                                state.isOfficersEditable
                            binding.treasurerInputLayout.isEnabled =
                                state.isOfficersEditable
                            binding.saaInputLayout.isEnabled =
                                state.isOfficersEditable
                            binding.ippInputLayout.isEnabled =
                                state.isOfficersEditable
                            binding.vpPublicRelationInputLayout.isEnabled =
                                state.isOfficersEditable

                            // Update edit button text for Officers
                            binding.editOfficersButton.text =
                                if (state.isOfficersEditable) {
                                    getString(R.string.cancel)
                                } else {
                                    getString(R.string.edit)
                                }

                            // Update grammarian details
                            state.grammarianDetails.let { details ->
                                if (!binding.wordOfDayEditText.isFocused) {
                                    binding.wordOfDayEditText.setText(details.wordOfTheDay)
                                }
                                if (!binding.wordMeaningEditText.isFocused) {
                                    binding.wordMeaningEditText.setText(
                                        details.wordMeaning.joinToString(
                                            "\n"
                                        )
                                    )
                                }
                                if (!binding.wordExamplesEditText.isFocused) {
                                    binding.wordExamplesEditText.setText(
                                        details.wordExamples.joinToString(
                                            "\n"
                                        )
                                    )
                                }
                                if (!binding.idiomEditText.isFocused) {
                                    binding.idiomEditText.setText(details.idiomOfTheDay)
                                }
                                if (!binding.idiomMeaningEditText.isFocused) {
                                    binding.idiomMeaningEditText.setText(details.idiomMeaning)
                                }
                                if (!binding.idiomExamplesEditText.isFocused) {
                                    binding.idiomExamplesEditText.setText(
                                        details.idiomExamples.joinToString(
                                            "\n"
                                        )
                                    )
                                }
                            }

                            // Handle loading and saving states
                            binding.progressBar.isVisible =
                                state.isGrammarianDetailsLoading || state.isGrammarianDetailsSaving
                            binding.saveGrammarianButton.isEnabled =
                                !state.isGrammarianDetailsSaving
                            binding.cancelGrammarianButton.isEnabled =
                                !state.isGrammarianDetailsSaving

                            // Show success/error messages
                            state.error?.let { error ->
                                if (error.contains(
                                        "successfully",
                                        ignoreCase = true
                                    )
                                ) {
                                    showMessage(error)
                                    toggleGrammarianEditMode(false)
                                } else {
                                    showError(error)
                                    viewModel.clearError()
                                }
                            }

                            // Update button visibilities
                            binding.editClubInfoButton.visibility =
                                if (state.isClubInfoEditable) View.GONE else View.VISIBLE
                            binding.saveClubInfoButton.visibility =
                                if (state.isClubInfoEditable) View.VISIBLE else View.GONE
                            binding.cancelClubInfoButton.visibility =
                                if (state.isClubInfoEditable) View.VISIBLE else View.GONE
                            
                            // Enable/disable club information fields based on edit mode (only for VP Education users)
                            if (isVpEducation) {
                                binding.clubNameEditText.isEnabled = state.isClubInfoEditable
                                binding.clubNumberEditText.isEnabled = state.isClubInfoEditable
                                binding.districtEditText.isEnabled = state.isClubInfoEditable
                                binding.areaEditText.isEnabled = state.isClubInfoEditable
                                binding.missionEditText.isEnabled = state.isClubInfoEditable
                            }
                            binding.editOfficersButton.visibility =
                                if (state.isOfficersEditable) View.GONE else View.VISIBLE
                            binding.saveOfficersButton.visibility =
                                if (state.isOfficersEditable) View.VISIBLE else View.GONE
                            binding.cancelOfficersButton.visibility =
                                if (state.isOfficersEditable) View.VISIBLE else View.GONE
                            
                            // Enable/disable officers fields based on edit mode (only for VP Education users)
                            if (isVpEducation) {
                                binding.presidentEditText.isEnabled = state.isOfficersEditable
                                binding.vpEducationEditText.isEnabled = state.isOfficersEditable
                                binding.vpMembershipEditText.isEnabled = state.isOfficersEditable
                                binding.secretaryEditText.isEnabled = state.isOfficersEditable
                                binding.treasurerEditText.isEnabled = state.isOfficersEditable
                                binding.saaEditText.isEnabled = state.isOfficersEditable
                                binding.ippEditText.isEnabled = state.isOfficersEditable
                                binding.vpPublicRelationEditText.isEnabled = state.isOfficersEditable
                            }

                            // Update abbreviations UI based on edit mode
                            binding.abbreviationsRecyclerView.isVisible = true
                            binding.saveAbbreviationsButton.isVisible =
                                isAbbreviationEditing
                            binding.addAbbreviationsButton.isVisible =
                                isAbbreviationEditing

                            if (state.isSaved) {
                                showMessage("Data saved successfully")
                                viewModel.clearSaveState()
                            }

                            updateUi(state.agenda)
                        } // End of withContext(Main)
                    } // End of collect
                } // End of repeatOnLifecycle
            } // End of launch
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
        binding.clubNameEditText.addTextChangedListener(object :
            android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateClubName(s?.toString() ?: "")
            }
        })

        binding.clubNumberEditText.addTextChangedListener(object :
            android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateClubNumber(s?.toString() ?: "")
            }
        })

        binding.areaEditText.addTextChangedListener(object :
            android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateArea(s?.toString() ?: "")
            }
        })

        binding.districtEditText.addTextChangedListener(object :
            android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateDistrict(s?.toString() ?: "")
            }
        })

        binding.missionEditText.addTextChangedListener(object :
            android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateClubMission(s?.toString() ?: "")
            }
        })
    }

    private
    var isInitialLoad = true

    private
    var currentOfficers: Map<String, String> = emptyMap()

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
        
        // Update club information fields (only if not currently focused to prevent cursor jumping)
        if (!binding.clubNameEditText.hasFocus() && binding.clubNameEditText.text?.toString() != agenda.clubName) {
            binding.clubNameEditText.setText(agenda.clubName)
        }
        if (!binding.clubNumberEditText.hasFocus() && binding.clubNumberEditText.text?.toString() != agenda.clubNumber) {
            binding.clubNumberEditText.setText(agenda.clubNumber)
        }
        if (!binding.districtEditText.hasFocus() && binding.districtEditText.text?.toString() != agenda.district) {
            binding.districtEditText.setText(agenda.district)
        }
        if (!binding.areaEditText.hasFocus() && binding.areaEditText.text?.toString() != agenda.area) {
            binding.areaEditText.setText(agenda.area)
        }
        if (!binding.missionEditText.hasFocus() && binding.missionEditText.text?.toString() != agenda.mission) {
            binding.missionEditText.setText(agenda.mission)
        }

        // Only update officer fields if they've actually changed
        if (currentOfficers != agenda.officers) {
            currentOfficers = agenda.officers
            setupOfficerFields(agenda.officers)
        }

        // Load and display abbreviations if they exist
        if (agenda.abbreviations.isNotEmpty()) {
            val items =
                agenda.abbreviations.map { (abbr, meaning) ->
                    AbbreviationItem(
                        id = abbr,
                        abbreviation = abbr,
                        meaning = meaning,
                        isEditable = isAbbreviationEditing
                    )
                }
            abbreviationAdapter.submitList(items)
        } else if (isAbbreviationEditing) {
            // If in edit mode but no abbreviations, add an empty item
            addNewAbbreviationItem()
        }
        
        // Always call updateAbbreviationUI to handle display logic consistently
        updateAbbreviationUI()

        isInitialLoad = false
    }

    private fun setupOfficerFields(officers: Map<String, String>) {
        // Only set up text watchers once
        if (isInitialLoad) {
            binding.ippEditText.setupOfficerTextWatcher("IPP")
            binding.presidentEditText.setupOfficerTextWatcher("President")
            binding.secretaryEditText.setupOfficerTextWatcher("Secretary")
            binding.saaEditText.setupOfficerTextWatcher("SAA")
            binding.treasurerEditText.setupOfficerTextWatcher("Treasurer")
            binding.vpEducationEditText.setupOfficerTextWatcher("VPE")
            binding.vpMembershipEditText.setupOfficerTextWatcher("VPM")
            binding.vpPublicRelationEditText.setupOfficerTextWatcher(
                "VPPR"
            )
        }
        // Only update text if it's different from current text and field is not focused
        fun updateField(
            editText: TextInputEditText,
            value: String?
        ) {
            if (!editText.hasFocus() && editText.text?.toString() != value) {
                editText.setText(value ?: "")
            }
        }

        // Update officer fields
        updateField(
            binding.presidentEditText,
            officers["President"]
        )
        updateField(binding.vpEducationEditText, officers["VPE"])
        updateField(binding.vpMembershipEditText, officers["VPM"])
        updateField(
            binding.secretaryEditText,
            officers["Secretary"]
        )
        updateField(
            binding.treasurerEditText,
            officers["Treasurer"]
        )
        updateField(binding.saaEditText, officers["SAA"])
        updateField(binding.ippEditText, officers["IPP"])
        updateField(
            binding.vpPublicRelationEditText,
            officers["VPPR"]
        )
    }

    private fun TextInputEditText.setupOfficerTextWatcher(
        role: String
    ) {
        // Clear existing text watchers
        this.tag?.let { oldWatcher ->
            if (oldWatcher is android.text.TextWatcher) {
                this.removeTextChangedListener(oldWatcher)
            }
        }

        // Add debounce to prevent rapid updates
        var debounceJob = viewLifecycleOwner.lifecycleScope.launch {
            var debounceJob: Job? = null
            val textWatcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                }

                override fun afterTextChanged(s: android.text.Editable?) {
                    debounceJob?.cancel()
                    debounceJob =
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(300) // 300ms debounce time
                            if (isActive) {
                                viewModel.updateOfficer(
                                    role,
                                    s?.toString() ?: ""
                                )
                            }
                        }
                }
            }

            this@setupOfficerTextWatcher.addTextChangedListener(
                textWatcher
            )
            this@setupOfficerTextWatcher.tag = textWatcher

            // Clean up when the view is destroyed
            awaitCancellation()
            this@setupOfficerTextWatcher.removeTextChangedListener(
                textWatcher
            )
        }

        // Store the job to cancel it if needed
        this.tag = debounceJob

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

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                debounceJob?.cancel()
                debounceJob =
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(500) // 500ms debounce
                        if (isAttachedToWindow && isLaidOut) {
                            viewModel.updateOfficer(
                                role,
                                s?.toString() ?: ""
                            )
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
        view?.let { view ->
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun toggleGrammarianEditMode(enable: Boolean) {
        val isSaving =
            viewModel.uiState.value.isGrammarianDetailsSaving

        binding.wordOfDayEditText.isEnabled = enable && !isSaving
        binding.wordMeaningEditText.isEnabled = enable && !isSaving
        binding.wordExamplesEditText.isEnabled = enable && !isSaving
        binding.idiomEditText.isEnabled = enable && !isSaving
        binding.idiomMeaningEditText.isEnabled = enable && !isSaving
        binding.idiomExamplesEditText.isEnabled =
            enable && !isSaving

        binding.editGrammarianButton.visibility =
            if (enable) View.GONE else View.VISIBLE
        binding.saveGrammarianButton.visibility =
            if (enable) View.VISIBLE else View.GONE
        binding.cancelGrammarianButton.visibility =
            if (enable) View.VISIBLE else View.GONE

        // Update button states
        binding.saveGrammarianButton.isEnabled = !isSaving
        binding.cancelGrammarianButton.isEnabled = !isSaving

        // Request focus when entering edit mode
        if (enable) {
            binding.wordOfDayEditText.requestFocus()

            // Show keyboard
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(
                binding.wordOfDayEditText,
                InputMethodManager.SHOW_IMPLICIT
            )
        }
    }

    private fun updateAndSaveGrammarianDetails() {
        val wordOfTheDay = binding.wordOfDayEditText.text.toString()
        val wordMeaning =
            binding.wordMeaningEditText.text.toString()
        val wordExamples =
            binding.wordExamplesEditText.text.toString()
        val idiomOfTheDay = binding.idiomEditText.text.toString()
        val idiomMeaning =
            binding.idiomMeaningEditText.text.toString()
        val idiomExamples =
            binding.idiomExamplesEditText.text.toString()

        // Basic validation
        if (wordOfTheDay.isBlank() || idiomOfTheDay.isBlank()) {
            showError("Word of the day and Idiom of the day are required")
            return
        }

        viewModel.updateGrammarianDetails(
            wordOfTheDay = wordOfTheDay,
            wordMeaning = wordMeaning,
            wordExamples = wordExamples,
            idiomOfTheDay = idiomOfTheDay,
            idiomMeaning = idiomMeaning,
            idiomExamples = idiomExamples
        )

        viewModel.saveGrammarianDetails()
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
            _binding = null
        }
        super.onDestroyView()
    }

    private fun updateStepView() {
        binding.meetingDetailsCard.visibility =
            if (currentStep == 1) View.VISIBLE else View.GONE
        binding.ClubInformationCard.visibility =
            if (currentStep == 2) View.VISIBLE else View.GONE
        binding.officersCard.visibility =
            if (currentStep == 3) View.VISIBLE else View.GONE
        binding.grammarianCard.visibility =
            if (currentStep == 4) View.VISIBLE else View.GONE
        if (currentStep == 4) {
            // Load grammarian details when the grammarian step is active
            viewModel.loadGrammarianDetails(meetingId)
        }
        binding.abbreviationsCard.visibility =
            if (currentStep == 5) View.VISIBLE else View.GONE

        binding.previousButton.visibility =
            if (currentStep > 1) View.VISIBLE else View.INVISIBLE
        binding.nextButton.visibility =
            if (currentStep < 5) View.VISIBLE else View.GONE
        binding.agendaItemsButton.visibility =
            if (currentStep == 5) View.VISIBLE else View.GONE

        // Update step indicators
        val indicators = listOf(
            binding.step1Indicator,
            binding.step2Indicator,
            binding.step3Indicator,
            binding.step4Indicator,
            binding.step5Indicator
        )
        indicators.forEachIndexed { index, indicator ->
            val step = index + 1
            when {
                step < currentStep -> indicator.setStepState(2) // Completed
                step == currentStep -> indicator.setStepState(0) // Active
                else -> indicator.setStepState(1) // Inactive
            }
        }
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