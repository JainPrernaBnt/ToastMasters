package com.bntsoft.toastmasters.presentation.ui.members.upcoming

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentUpcomingMeetingsBinding
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingAvailability
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpcomingMeetingsFragment : Fragment() {

    private var _binding: FragmentUpcomingMeetingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UpcomingMeetingsListViewModel by viewModels()
    private var currentMeetings: List<Meeting> = emptyList()
    private val adapter = UpcomingMeetingsAdapter().apply {
        onAvailabilitySubmitted = { meetingId, isAvailable, preferredRoles ->
            viewModel.saveMeetingAvailability(meetingId, isAvailable, preferredRoles)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpcomingMeetingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvUpcomingMeetings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@UpcomingMeetingsFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchUpcomingMeetings()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UpcomingMeetingsUiState.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    showError(state.message)
                }

                UpcomingMeetingsUiState.Loading -> {
                    if (!binding.swipeRefreshLayout.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }

                is UpcomingMeetingsUiState.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    if (state.meetings.isEmpty()) {
                        showEmptyState()
                    } else {
                        binding.emptyStateLayout.visibility = View.GONE
                        binding.rvUpcomingMeetings.visibility = View.VISIBLE
                        currentMeetings = state.meetings
                        adapter.submitList(currentMeetings)
                    }
                }
            }
        }

        viewModel.showSuccessDialog.observe(viewLifecycleOwner) { show ->
            if (show) {
                showSuccessDialog()
            }
        }

        // Observe the meetings LiveData which will be updated with availability
        viewModel.meetings.observe(viewLifecycleOwner) { meetings ->
            if (meetings != null && meetings.isNotEmpty()) {
                currentMeetings = meetings
                adapter.submitList(meetings)
            }
        }
    }

    private fun showSuccessDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.success)
            .setMessage(R.string.availability_saved_successfully)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                viewModel.onDialogDismissed()
                dialog.dismiss()
            }
            .setOnDismissListener {
                viewModel.onDialogDismissed()
            }
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showEmptyState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.rvUpcomingMeetings.visibility = View.GONE
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

