package com.bntsoft.toastmasters.presentation.ui.vp.roles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentAssignRolesBinding
import com.bntsoft.toastmasters.presentation.ui.vp.roles.model.MeetingListItem
import com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter.MeetingAdapter
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
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
        meetingAdapter = MeetingAdapter { meeting ->
            navigateToMemberRoles(meeting)
        }
        binding.rvMeetings.apply {
            adapter = meetingAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            itemAnimator = null // Disable animations to prevent flickering
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

    private fun navigateToMemberRoles(meeting: MeetingListItem) {
        val action = AssignRolesFragmentDirections.actionAssignRolesToMemberRoles(meeting.meeting)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}