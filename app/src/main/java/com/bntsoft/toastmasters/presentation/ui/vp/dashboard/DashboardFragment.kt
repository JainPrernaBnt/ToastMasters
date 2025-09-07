package com.bntsoft.toastmasters.presentation.ui.vp.dashboard

import android.os.Bundle
import android.util.Log
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
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter.MeetingAdapter
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.viewmodel.DashboardViewModel
import com.bntsoft.toastmasters.utils.Constants.EXTRA_MEETING_ID
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var meetingAdapter: MeetingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        setupRecyclerView()
        observeUpcomingMeetings()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("DashboardDebug", "onViewCreated: Fragment view created")
        Log.d("DashboardDebug", "ViewModel: $viewModel")
        Log.d("DashboardDebug", "Adapter initialized: ${::meetingAdapter.isInitialized}")

        // Set up RecyclerView if not already done
        if (!::meetingAdapter.isInitialized) {
            setupRecyclerView()
        }

        // Force refresh data
        Log.d("DashboardDebug", "Loading upcoming meetings...")
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
                val bundle = bundleOf("meeting_id" to meetingId)
                findNavController().navigate(
                    R.id.action_dashboardFragment_to_editMeetingFragment,
                    bundle
                )
            },
            onDelete = { meetingId ->
                showDeleteConfirmationDialog(meetingId)
            },
            onComplete = { meetingId ->
                showCompleteConfirmationDialog(meetingId)
            },
            onItemClick = { meetingId ->
                findNavController().navigate(
                    R.id.action_dashboardFragment_to_memberResponseFragment,
                    bundleOf(EXTRA_MEETING_ID to meetingId)
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
            Log.d("DashboardDebug", "Starting to observe upcoming meetings")
            viewModel.upcomingMeetingsStateWithCounts.collect { state ->
                Log.d("DashboardDebug", "Received state: ${state.javaClass.simpleName}")
                val message = when (state) {
                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Success -> {
                        Log.d("DashboardDebug", "Received ${state.meetings.size} meetings from ViewModel")
                        if (state.meetings.isEmpty()) {
                            Log.d("DashboardDebug", "No meetings received from ViewModel")
                        } else {
                            state.meetings.forEachIndexed { index, meeting ->
                                Log.d("DashboardDebug", "Meeting #${index + 1}: ${meeting.meeting.theme} (${meeting.meeting.id}) - " +
                                        "Status: ${meeting.meeting.status}, " +
                                        "Date: ${meeting.meeting.dateTime}, " +
                                        "Available: ${meeting.availableCount}, " +
                                        "Not Available: ${meeting.notAvailableCount}")
                            }
                        }
                        meetingAdapter.submitList(state.meetings) {
                            Log.d("DashboardDebug", "Adapter updated with ${state.meetings.size} meetings")
                            Log.d("DashboardDebug", "RecyclerView child count: ${binding.rvMeetings.childCount}")
                            Log.d("DashboardDebug", "RecyclerView adapter item count: ${meetingAdapter.itemCount}")
                        }
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

                // Also log to console for debugging
                Timber.tag("DashboardDebug").d(message)
                if (state is DashboardViewModel.UpcomingMeetingsStateWithCounts.Success) {
                    state.meetings.forEach { meeting ->
                        Timber.tag("DashboardDebug").d(
                            "%snull", "Meeting: ${meeting.meeting.theme}, " +
                                    "Available: ${meeting.availableCount}, " +
                                    "Not Available: ${meeting.notAvailableCount}, " +
                                    "Not Confirmed: ${meeting.notConfirmedCount}, "
                        )
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(meetingId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Meeting")
            .setMessage("Are you sure you want to delete this meeting?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.deleteMeeting(meetingId)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCompleteConfirmationDialog(meetingId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Complete Meeting")
            .setMessage("Mark this meeting as completed? This action cannot be undone.")
            .setPositiveButton("Complete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.completeMeeting(meetingId)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("DashboardFragment", "onDestroyView called")
        _binding = null
    }
}