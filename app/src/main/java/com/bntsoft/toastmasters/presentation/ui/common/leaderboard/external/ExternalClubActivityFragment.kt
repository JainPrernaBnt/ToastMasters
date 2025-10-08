package com.bntsoft.toastmasters.presentation.ui.common.leaderboard.external

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentExternalClubActivityBinding
import com.bntsoft.toastmasters.presentation.ui.common.leaderboard.external.ExternalClubActivityAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExternalClubActivityFragment : Fragment() {

    private var _binding: FragmentExternalClubActivityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExternalClubActivityViewModel by viewModels()
    private lateinit var adapter: ExternalClubActivityAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExternalClubActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = ExternalClubActivityAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onItemClick = { activity ->
                // Handle item click if needed
            },
            onEditClick = { activity ->
                editActivity(activity)
            },
            onDeleteClick = { activity ->
                showDeleteConfirmation(activity)
            }
        )
        
        binding.recyclerViewActivities.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExternalClubActivityFragment.adapter
        }
    }

    private fun editActivity(activity: com.bntsoft.toastmasters.domain.model.ExternalClubActivity) {
        val bundle = Bundle().apply {
            putParcelable("activity", activity)
        }
        findNavController().navigate(R.id.action_externalClubActivityFragment_to_addExternalActivityFragment, bundle)
    }

    private fun showDeleteConfirmation(activity: com.bntsoft.toastmasters.domain.model.ExternalClubActivity) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Activity")
            .setMessage("Are you sure you want to delete this activity?")
            .setPositiveButton("Delete") { _, _ ->
                deleteActivity(activity)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteActivity(activity: com.bntsoft.toastmasters.domain.model.ExternalClubActivity) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteActivity(activity.id)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.swipeRefresh.isRefreshing = false
                
                binding.emptyStateLayout.visibility = if (state.isEmpty && !state.isLoading) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                
                state.error?.let { error ->
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activities.collect { activities ->
                adapter.submitList(activities)
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddActivity.setOnClickListener {
            findNavController().navigate(R.id.action_externalClubActivityFragment_to_addExternalActivityFragment)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshActivities()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
