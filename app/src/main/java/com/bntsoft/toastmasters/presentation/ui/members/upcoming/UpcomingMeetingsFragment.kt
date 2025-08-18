package com.bntsoft.toastmasters.presentation.ui.members.upcoming

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.databinding.FragmentUpcomingMeetingsBinding
import com.bntsoft.toastmasters.presentation.viewmodel.MeetingsViewModel
import com.bntsoft.toastmasters.presentation.viewmodel.UpcomingMeetingsStateWithCounts
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UpcomingMeetingsFragment : Fragment() {

    private var _binding: FragmentUpcomingMeetingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MeetingsViewModel by viewModels()
    private lateinit var upcomingMeetingAdapter: UpcomingMeetingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpcomingMeetingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeUpcomingMeetings()
    }

    private fun setupRecyclerView() {
        upcomingMeetingAdapter = UpcomingMeetingAdapter()
        binding.rvUpcomingMeetings.apply {
            adapter = upcomingMeetingAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeUpcomingMeetings() {
        lifecycleScope.launch {
            viewModel.upcomingMeetingsStateWithCounts.collect { state ->
                binding.progressBar.isVisible = state is UpcomingMeetingsStateWithCounts.Loading
                binding.emptyStateLayout.isVisible = state is UpcomingMeetingsStateWithCounts.Empty
                binding.rvUpcomingMeetings.isVisible = state is UpcomingMeetingsStateWithCounts.Success

                if (state is UpcomingMeetingsStateWithCounts.Success) {
                    upcomingMeetingAdapter.submitList(state.meetings)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUpcomingMeetings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}