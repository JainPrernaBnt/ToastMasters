package com.bntsoft.toastmasters.presentation.ui.common.leaderboard

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.GemMemberData
import com.bntsoft.toastmasters.databinding.FragmentGemOfMonthBinding
import com.bntsoft.toastmasters.presentation.ui.common.leaderboard.adapter.GemMemberAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class GemOfMonthFragment : Fragment() {

    private var _binding: FragmentGemOfMonthBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GemOfMonthViewModel by viewModels()
    private lateinit var gemMemberAdapter: GemMemberAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGemOfMonthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUI() {
        gemMemberAdapter = GemMemberAdapter { memberData ->
            val uiState = viewModel.uiState.value
            Log.d("GemOfMonthFragment", "Member clicked - canEdit: ${uiState.canEdit}, showAllMembers: ${uiState.showAllMembers}, isVpEducation: ${uiState.isVpEducation}, isEditMode: ${uiState.isEditMode}")
            
            when {
                // VP_EDUCATION can always select if in edit mode or no gem selected
                uiState.canEdit && uiState.showAllMembers -> {
                    showSelectGemDialog(memberData)
                }
                // VP_EDUCATION viewing selected gem - show option to edit
                uiState.canEdit && !uiState.showAllMembers && !uiState.isEditMode -> {
                    Toast.makeText(requireContext(), "Click 'Edit Selection' to change the Gem of the Month", Toast.LENGTH_SHORT).show()
                }
                // Member role - read only
                !uiState.canEdit -> {
                    Toast.makeText(requireContext(), "Only VP Education can select Gem of the Month", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    showSelectGemDialog(memberData)
                }
            }
        }
        
        binding.membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = gemMemberAdapter
        }
    }

    private fun setupClickListeners() {

        binding.monthSelector.setOnClickListener {
            showMonthYearPicker()
        }
        
        binding.sortButton.setOnClickListener {
            showSortMenu()
        }
        
        binding.editSelectionButton.setOnClickListener {
            val uiState = viewModel.uiState.value
            if (uiState.isEditMode) {
                // Cancel edit mode
                viewModel.exitEditMode()
            } else {
                // Enter edit mode
                viewModel.enterEditMode()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                updateUI(uiState)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedMonth.collect { month ->
                updateMonthDisplay()
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedYear.collect { year ->
                updateMonthDisplay()
            }
        }
    }

    private fun updateUI(uiState: GemOfMonthUiState) {
        android.util.Log.d("GemOfMonthFragment", "UI State - isVpEducation: ${uiState.isVpEducation}, canEdit: ${uiState.canEdit}, showAllMembers: ${uiState.showAllMembers}, isLoading: ${uiState.isLoading}, hasData: ${uiState.hasData}")
        
        // Hide member count chip and sort row for regular members
        binding.memberCountChip.visibility = if (uiState.isVpEducation) View.VISIBLE else View.GONE
        binding.sortRow.visibility = if (uiState.isVpEducation) View.VISIBLE else View.GONE
        
        // Update member count display
        binding.memberCountChip.text = "${uiState.eligibleMembersCount} Members"
        
        // Handle visibility based on loading state and data availability
        when {
            uiState.isLoading && !uiState.hasData -> {
                // First time loading - show progress bar
                binding.progressBar.visibility = View.VISIBLE
                binding.membersRecyclerView.visibility = View.GONE
                binding.emptyStateCard.visibility = View.GONE
            }
            uiState.isLoading && uiState.hasData -> {
                // Loading with existing data - show data with subtle loading indicator
                binding.progressBar.visibility = View.GONE
                binding.membersRecyclerView.visibility = View.VISIBLE
                binding.emptyStateCard.visibility = View.GONE
                
                // Show existing data while loading
                val displayList = if (uiState.showAllMembers) {
                    uiState.memberDataList
                } else {
                    uiState.selectedGem?.let { listOf(it) } ?: emptyList()
                }
                
                gemMemberAdapter.submitList(displayList)
                gemMemberAdapter.setSelectedGem(uiState.selectedGem)
            }
            uiState.hasData -> {
                // Show data, hide progress bar and empty state
                binding.progressBar.visibility = View.GONE
                binding.membersRecyclerView.visibility = View.VISIBLE
                binding.emptyStateCard.visibility = View.GONE
                
                // Show all members or only selected gem based on state
                val displayList = if (uiState.showAllMembers) {
                    uiState.memberDataList
                } else {
                    // Show only selected gem
                    uiState.selectedGem?.let { listOf(it) } ?: emptyList()
                }
                
                gemMemberAdapter.submitList(displayList)
                gemMemberAdapter.setSelectedGem(uiState.selectedGem)
            }
            else -> {
                // No data and not loading - show empty state
                binding.progressBar.visibility = View.GONE
                binding.membersRecyclerView.visibility = View.GONE
                binding.emptyStateCard.visibility = View.VISIBLE
            }
        }
        
        // Update Edit button visibility and text
        updateEditButtonVisibility(uiState)
        
        uiState.error?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
        
        uiState.showSuccessMessage?.let { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }
    
    private fun updateEditButtonVisibility(uiState: GemOfMonthUiState) {
        // Show edit button only for VP_EDUCATION when there's a selected gem
        val shouldShowEditButton = uiState.isVpEducation && uiState.selectedGem != null
        
        android.util.Log.d("GemOfMonthFragment", "Edit button - isVpEducation: ${uiState.isVpEducation}, selectedGem: ${uiState.selectedGem?.user?.name}, showEditButton: $shouldShowEditButton, isEditMode: ${uiState.isEditMode}")
        
        binding.editSelectionButton.visibility = if (shouldShowEditButton) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        // Update button text based on edit mode
        binding.editSelectionButton.text = if (uiState.isEditMode) {
            "Cancel Edit"
        } else {
            "Edit Selection"
        }
    }

    private fun updateMonthDisplay() {
        binding.monthSelector.text = viewModel.getMonthYearString()
    }

    private fun showMonthYearPicker() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_month_year_picker)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val monthPicker = dialog.findViewById<NumberPicker>(R.id.monthPicker)
        val yearPicker = dialog.findViewById<NumberPicker>(R.id.yearPicker)
        val okButton = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.okButton)
        val cancelButton = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)

        // Get current selected values from ViewModel
        val currentMonth = viewModel.selectedMonth.value - 1 // Convert to 0-based
        val currentYear = viewModel.selectedYear.value

        // Setup month picker
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        monthPicker.wrapSelectorWheel = true
        monthPicker.value = currentMonth // Set current selected month

        // Setup year picker
        val calendarYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = calendarYear - 5
        yearPicker.maxValue = calendarYear + 5
        yearPicker.wrapSelectorWheel = false
        yearPicker.value = currentYear // Set current selected year

        okButton.setOnClickListener {
            val selectedMonth = monthPicker.value + 1 // Convert to 1-based
            val selectedYear = yearPicker.value

            android.util.Log.d("GemOfMonthFragment", "Month selected: $selectedMonth/$selectedYear")

            // Only reload if month/year actually changed
            if (selectedMonth != viewModel.selectedMonth.value || selectedYear != viewModel.selectedYear.value) {
                viewModel.selectMonth(selectedYear, selectedMonth)
            }

            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun showSortMenu() {
        val popup = PopupMenu(requireContext(), binding.sortButton)
        
        SortType.values().forEach { sortType ->
            popup.menu.add(sortType.displayName)
        }
        
        popup.setOnMenuItemClickListener { menuItem ->
            val sortType = SortType.values().find { it.displayName == menuItem.title }
            sortType?.let { viewModel.sortMembers(it) }
            true
        }
        
        popup.show()
    }

    private fun showSelectGemDialog(memberData: GemMemberData) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Gem of the Month")
            .setMessage("Are you sure you want to select ${memberData.user.name} as the Gem of the Month for ${viewModel.getMonthYearString()}?")
            .setPositiveButton("Select") { _, _ ->
                viewModel.selectGemOfTheMonth(memberData)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
