package com.bntsoft.toastmasters.presentation.ui.common.leaderboard.external

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bntsoft.toastmasters.databinding.FragmentAddExternalActivityBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddExternalActivityFragment : Fragment() {

    private var _binding: FragmentAddExternalActivityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddExternalActivityViewModel by viewModels()
    private var selectedDate: Date? = null
    private var editingActivity: com.bntsoft.toastmasters.domain.model.ExternalClubActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExternalActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Check if we're editing an existing activity
        editingActivity = arguments?.getParcelable("activity")
        
        setupRoleDropdown()
        setupDatePicker()
        observeViewModel()
        setupClickListeners()
        
        // Pre-fill form if editing
        editingActivity?.let { activity ->
            prefillForm(activity)
            binding.btnSubmit.text = "Update"
        }
    }

    private fun prefillForm(activity: com.bntsoft.toastmasters.domain.model.ExternalClubActivity) {
        binding.apply {
            etClubName.setText(activity.clubName)
            etClubLocation.setText(activity.clubLocation)
            actvRolePlayed.setText(activity.rolePlayed, false)
            etNotes.setText(activity.notes)
            
            selectedDate = activity.meetingDate
            val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            etMeetingDate.setText(dateFormat.format(activity.meetingDate))
        }
    }

    private fun setupRoleDropdown() {
        val roles = viewModel.getRoleOptions()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        binding.actvRolePlayed.setAdapter(adapter)

        // Hide keyboard when tapped & show dropdown
        binding.actvRolePlayed.setOnTouchListener { view, event ->
            view.performClick()

            // Hide keyboard
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)

            // Show dropdown
            binding.actvRolePlayed.showDropDown()
            true // consume the event
        }

        // Handle selection
        binding.actvRolePlayed.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateRolePlayed(roles[position])
        }
    }


    private fun setupDatePicker() {
        binding.etMeetingDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDate?.let { calendar.time = it }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
                
                val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                binding.etMeetingDate.setText(dateFormat.format(calendar.time))
                
                viewModel.updateMeetingDate(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.btnSubmit.isEnabled = state.isFormValid && !state.isLoading
                
                if (state.isSuccess) {
                    val message = if (editingActivity != null) {
                        "Activity updated successfully! ✨"
                    } else {
                        "Activity added successfully! ✨"
                    }
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                
                state.error?.let { error ->
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSubmit.setOnClickListener {
            collectFormData()
            if (editingActivity != null) {
                viewModel.updateActivity(editingActivity!!.id)
            } else {
                viewModel.submitActivity()
            }
        }

        binding.etClubName.setOnFocusChangeListener { _, _ ->
            viewModel.updateClubName(binding.etClubName.text.toString())
        }

        binding.etClubLocation.setOnFocusChangeListener { _, _ ->
            viewModel.updateClubLocation(binding.etClubLocation.text.toString())
        }

        binding.etNotes.setOnFocusChangeListener { _, _ ->
            viewModel.updateNotes(binding.etNotes.text.toString())
        }
    }

    private fun collectFormData() {
        viewModel.updateClubName(binding.etClubName.text.toString())
        viewModel.updateClubLocation(binding.etClubLocation.text.toString())
        viewModel.updateNotes(binding.etNotes.text.toString())
        selectedDate?.let { viewModel.updateMeetingDate(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
