package com.bntsoft.toastmasters.presentation.ui.vp.roles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.databinding.FragmentAssignRolesBinding
import com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter.MeetingAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AssignRolesFragment : Fragment() {
    private var _binding: FragmentAssignRolesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AssignRolesViewModel by viewModels()
    private lateinit var meetingAdapter: MeetingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssignRolesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        loadMeetings()
    }

    private fun setupRecyclerView() {
        meetingAdapter = MeetingAdapter(
            onItemClick = { meeting ->
                // Card click → navigate to MemberRoles
                navigateToMemberRoles(meeting.id)
            },
            onCreateAgendaClick = { meeting ->
                // btnCreateAgenda click → navigate to CreateAgenda
                val action = AssignRolesFragmentDirections
                    .actionAssignRolesFragmentToCreateAgendaFragment(meeting.id)
                findNavController().navigate(action)
            }
        )

        binding.rvMeetings.apply {
            adapter = meetingAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            itemAnimator = null // Disable animations to prevent flickering
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadMeetings()
        }
    }


    private fun loadMeetings() {
        viewModel.loadMeetings()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.meetings.collect { meetings ->
                    meetingAdapter.submitList(meetings) {
                        // Scroll to top when new data is loaded
                        if (meetings.isNotEmpty()) {
                            binding.layoutEmptyState.root.visibility = View.GONE
                            binding.rvMeetings.scrollToPosition(0)
                        } else {
                            binding.layoutEmptyState.root.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.error.collect { error ->
                    error?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun navigateToMemberRoles(meetingId: String) {
        val action = AssignRolesFragmentDirections
            .actionAssignRolesFragmentToMemberRoleAssignFragment(meetingId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}