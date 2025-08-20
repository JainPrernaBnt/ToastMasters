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
import com.bntsoft.toastmasters.presentation.viewmodel.MeetingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.bntsoft.toastmasters.presentation.viewmodel.UpcomingMeetingsStateWithCounts as UpcomingMeetingsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MeetingsViewModel by viewModels()
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
                if (!isDialogShowing) {
                    isDialogShowing = true
                    currentMeetingId = meetingId
                    findNavController().navigate(
                        R.id.action_dashboardFragment_to_upcomingMeetingFragment,
                        bundleOf("meetingId" to meetingId)
                    )
                }
            }
        )
        binding.rvMeetings.apply {
            adapter = meetingAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeUpcomingMeetings() {
        lifecycleScope.launch {
            viewModel.upcomingMeetingsStateWithCounts.collect { state ->
                when (state) {
                    is UpcomingMeetingsState.Success -> {
                        meetingAdapter.submitList(state.meetings)
                    }

                    is UpcomingMeetingsState.Empty -> {
                        meetingAdapter.submitList(emptyList())
                        // Optionally, show an empty state view
                    }

                    is UpcomingMeetingsState.Error -> {
                        // Handle error state, e.g., show a toast or an error message
                    }

                    is UpcomingMeetingsState.Loading -> {
                        // Handle loading state, e.g., show a progress bar
                    }

                    is UpcomingMeetingsState.Error -> TODO()
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