package com.bntsoft.toastmasters.presentation.ui.vp.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bntsoft.toastmasters.databinding.FragmentDashboardBinding
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bntsoft.toastmasters.presentation.viewmodel.MeetingsViewModel
import com.bntsoft.toastmasters.presentation.viewmodel.UpcomingMeetingsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
        private val binding get() = _binding!!

    private val viewModel: MeetingsViewModel by viewModels()

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
        observeUpcomingMeetings()
        return binding.root
    }

    private fun observeUpcomingMeetings() {
        lifecycleScope.launch {
            viewModel.upcomingMeetingsState.collect { state ->
                when (state) {
                    is UpcomingMeetingsState.Success -> {
                        val mostRecentMeeting = state.meetings.firstOrNull()
                        if (mostRecentMeeting != null) {
                            binding.cardRecentMeeting.visibility = View.VISIBLE
                            binding.tvRecentMeetingTitle.text = mostRecentMeeting.theme
                            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                            binding.tvRecentMeetingDate.text = mostRecentMeeting.dateTime.format(formatter)
                            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                            val time = "${mostRecentMeeting.dateTime.format(timeFormatter)} - ${mostRecentMeeting.endDateTime?.format(timeFormatter)}"
                            binding.tvRecentMeetingTime.text = time
                                                        binding.tvRecentMeetingVenue.text = mostRecentMeeting.location
                        } else {
                            binding.cardRecentMeeting.visibility = View.GONE
                        }
                    }
                    is UpcomingMeetingsState.Empty -> {
                        binding.cardRecentMeeting.visibility = View.GONE
                    }
                    is UpcomingMeetingsState.Error -> {
                        // Handle error
                    }
                    is UpcomingMeetingsState.Loading -> {
                        // Handle loading
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