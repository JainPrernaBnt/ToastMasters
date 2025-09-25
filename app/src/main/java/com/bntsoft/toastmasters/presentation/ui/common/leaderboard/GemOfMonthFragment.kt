package com.bntsoft.toastmasters.presentation.ui.common.leaderboard

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.GemMemberData
import com.bntsoft.toastmasters.databinding.FragmentGemOfMonthBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
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
            showSelectGemDialog(memberData)
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
        
        binding.selectGemFab.setOnClickListener {
            Toast.makeText(requireContext(), "Please select a member from the list", Toast.LENGTH_SHORT).show()
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
        binding.progressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE
        
        binding.memberCountChip.text = "${uiState.eligibleMembersCount} Members"
        
        if (uiState.hasData) {
            binding.membersRecyclerView.visibility = View.VISIBLE
            binding.emptyStateCard.visibility = View.GONE
            gemMemberAdapter.submitList(uiState.memberDataList)
        } else if (!uiState.isLoading) {
            binding.membersRecyclerView.visibility = View.GONE
            binding.emptyStateCard.visibility = View.VISIBLE
        }
        
        uiState.error?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
        
        uiState.showSuccessMessage?.let { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    private fun updateMonthDisplay() {
        binding.monthSelector.text = viewModel.getMonthYearString()
    }

    private fun showMonthYearPicker() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_month_year_picker)
        dialog.setCancelable(true)

        val monthPicker = dialog.findViewById<NumberPicker>(R.id.monthPicker)
        val yearPicker = dialog.findViewById<NumberPicker>(R.id.yearPicker)
        val okButton = dialog.findViewById<Button>(R.id.okButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        // Months Jan-Dec
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        monthPicker.wrapSelectorWheel = true

        // Year range
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = currentYear - 10
        yearPicker.maxValue = currentYear + 10
        yearPicker.value = currentYear

        okButton.setOnClickListener {
            val selectedMonth = monthPicker.value      // 0-based
            val selectedYear = yearPicker.value

            // Calculate last day of month
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, selectedYear)
            calendar.set(Calendar.MONTH, selectedMonth)
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))

            // Pass month/year to ViewModel
            viewModel.selectMonth(selectedYear, selectedMonth + 1)

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
