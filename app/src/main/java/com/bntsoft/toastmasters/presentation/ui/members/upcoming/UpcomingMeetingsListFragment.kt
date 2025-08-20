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
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentUpcomingMeetingsBinding
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.utils.UserManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UpcomingMeetingsListFragment : Fragment() {

    @Inject
    lateinit var memberResponseRepository: MemberResponseRepository

    @Inject
    lateinit var userManager: UserManager

    private var _binding: FragmentUpcomingMeetingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UpcomingMeetingsListViewModel by viewModels()
    private var adapter: UpcomingMeetingsAdapter? = null

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

        // Set initial states
        binding.emptyStateLayout.isVisible = false
        binding.rvUpcomingMeetings.isVisible = false

        // Setup RecyclerView and load userId asynchronously
        setupRecyclerView()

        // Set up swipe refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadUpcomingMeetings()
        }

        // Observe ViewModel
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = UpcomingMeetingsAdapter(
            onMeetingClick = { meeting ->
                Timber.d("Meeting clicked: ${meeting.theme} (${meeting.id})")
                navigateToMeetingDetails(meeting)
            },
            onJoinClick = { meeting ->
                Timber.d("Join clicked for meeting: ${meeting.theme}")
                joinMeeting(meeting)
            },
            onViewResponsesClick = { meeting ->
                Timber.d("View responses for meeting: ${meeting.theme}")
                viewResponses(meeting)
            }
        )
        
        binding.rvUpcomingMeetings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@UpcomingMeetingsListFragment.adapter
            setHasFixedSize(true)
            itemAnimator = null // Disable animations for smoother updates
        }
        
        // Add item decoration if needed
        // addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        
        // Load userId in a coroutine
        viewLifecycleOwner.lifecycleScope.launch {
            val currentUserId = userManager.getCurrentUserId() ?: ""

            adapter = UpcomingMeetingsAdapter(
                currentUserId = currentUserId,
                onAvailabilityChanged = { meetingId, availability ->
                    viewModel.updateMemberAvailability(meetingId, availability)
                },
                onRolesUpdated = { meetingId, selectedRoles ->
                    viewModel.updatePreferredRoles(meetingId, selectedRoles)
                },
                memberResponseRepository = memberResponseRepository
            )

            binding.rvUpcomingMeetings.adapter = adapter
            adapter?.submitList(emptyList())

            // Load meetings after adapter is attached
            viewModel.loadUpcomingMeetings()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UpcomingMeetingsListViewModel.UpcomingMeetingsState.Loading -> {
                    Timber.d("Loading meetings...")
                    if (!binding.swipeRefreshLayout.isRefreshing) {
                        showLoading(true)
                        binding.emptyStateLayout.isVisible = false
                    }
                }
                is UpcomingMeetingsListViewModel.UpcomingMeetingsState.Success -> {
                    showLoading(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    
                    Timber.d("✅ Successfully loaded ${state.meetings.size} meetings")
                    state.meetings.forEachIndexed { index, meeting ->
                        Timber.d("${index + 1}. ${meeting.theme} | ${meeting.dateTime} | ID: ${meeting.id}")
                    }

                    if (state.meetings.isEmpty()) {
                        Timber.d("No meetings found")
                        showEmptyState()
                    } else {
                        binding.emptyStateLayout.isVisible = false
                        binding.rvUpcomingMeetings.isVisible = true
                        
                        // Update the adapter with the new list
                        adapter?.submitList(state.meetings.toList()) {
                            Timber.d("RecyclerView updated with ${state.meetings.size} items")
                            binding.rvUpcomingMeetings.scrollToPosition(0)
                        }
                        
                        // Force layout update
                        binding.rvUpcomingMeetings.invalidateItemDecorations()
                    }
                }
                is UpcomingMeetingsListViewModel.UpcomingMeetingsState.Error -> {
                    Timber.e("Error loading meetings: ${state.message}")
                    showLoading(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.emptyStateLayout.isVisible = true
                    binding.rvUpcomingMeetings.isVisible = false
                    showError(state.message)
                }
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Retry") { viewModel.loadUpcomingMeetings() }
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.rvUpcomingMeetings.isVisible = !show
    }

    private fun showEmptyState() {
        binding.emptyStateLayout.isVisible = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = UpcomingMeetingsListFragment()
    }
}

