package com.bntsoft.toastmasters.presentation.ui.vp.dashboard

import android.os.Bundle
import android.util.Log
import timber.log.Timber
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentDashboardBinding
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.DashboardViewModel
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var meetingAdapter: MeetingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DashboardFragment", "onCreate called")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("DashboardFragment", "onCreateView called")
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        setupRecyclerView()
        observeUpcomingMeetings()
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Basic log to verify fragment is loading
        android.util.Log.d("DashboardDebug", "DashboardFragment loaded successfully")
        
        // Debug: Print ViewModel and adapter state
        Log.d("DashboardDebug", "ViewModel: $viewModel")
        Log.d("DashboardDebug", "Adapter initialized: ${::meetingAdapter.isInitialized}")
        
        // Force refresh data
        viewModel.loadUpcomingMeetings()
    }

    private var isDialogShowing = false
    private var currentMeetingId: String? = null

    override fun onResume() {
        super.onResume()
        viewModel.loadUpcomingMeetings()
        // Reset dialog state when coming back to this fragment
        isDialogShowing = false
        currentMeetingId = null
    }

    private fun setupRecyclerView() {
        meetingAdapter = MeetingAdapter(
            onEdit = { meetingId ->
                findNavController().navigate(
                    R.id.action_dashboardFragment_to_editMeetingFragment,
                    bundleOf("meeting_id" to meetingId)
                )
            },
            onDelete = { meetingId ->
                // Confirm and delete directly
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Meeting")
                    .setMessage("Are you sure you want to delete this meeting?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteMeeting(meetingId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onItemClick = { meetingId ->
                findNavController().navigate(
                    R.id.action_dashboardFragment_to_memberResponseFragment,
                    bundleOf("meeting_id" to meetingId)
                )

            }
        )
        binding.rvMeetings.apply {
            adapter = meetingAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeUpcomingMeetings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.upcomingMeetingsStateWithCounts.collect { state ->
                val message = when (state) {
                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Success -> {
                        meetingAdapter.submitList(state.meetings)
                        "Loaded ${state.meetings.size} meetings"
                    }
                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Empty -> {
                        meetingAdapter.submitList(emptyList())
                        "No upcoming meetings found"
                    }
                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Error -> {
                        "Error: ${state.message}"
                    }
                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Loading -> {
                        "Loading meetings..."
                    }
                }
                
                // Show a toast with the current state
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                
                // Also log to console for debugging
                android.util.Log.d("DashboardDebug", message)
                if (state is DashboardViewModel.UpcomingMeetingsStateWithCounts.Success) {
                    state.meetings.forEach { meeting ->
                        android.util.Log.d("DashboardDebug", "Meeting: ${meeting.meeting.theme}, " +
                                "Available: ${meeting.availableCount}, " +
                                "Not Available: ${meeting.notAvailableCount}, " +
                                "Not Confirmed: ${meeting.notConfirmedCount}, " +
                                "Not Responded: ${meeting.notResponded}")
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("DashboardFragment", "onDestroyView called")
        _binding = null
    }
}