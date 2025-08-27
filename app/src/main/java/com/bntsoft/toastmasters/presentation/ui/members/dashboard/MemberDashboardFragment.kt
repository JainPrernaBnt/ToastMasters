package com.bntsoft.toastmasters.presentation.ui.members.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bntsoft.toastmasters.databinding.FragmentMemberDashboardBinding
import com.bntsoft.toastmasters.presentation.ui.members.dashboard.adapter.MeetingWithRoleAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MemberDashboardFragment : Fragment() {
    private val TAG = "MemberDashboardFragment"
    
    private var _binding: FragmentMemberDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MemberDashboardViewModel by viewModels()
    private val adapter = MeetingWithRoleAdapter()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        
        setupRecyclerView()
        observeViewModel()
        setupSwipeToRefresh()
        
        Log.d(TAG, "View setup completed")
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")
        binding.rvMeetings.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = this@MemberDashboardFragment.adapter
            Log.d(TAG, "RecyclerView setup complete")
        }
    }

    private fun observeViewModel() {
        Log.d(TAG, "Starting to observe ViewModel")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d(TAG, "New UI State: ${state::class.simpleName}")
                    
                    when (state) {
                        is MemberDashboardUiState.Loading -> {
                            Log.d(TAG, "Loading data...")
                            showLoading(true)
                            showErrorState(false)
                            showEmptyState(false)
                        }

                        is MemberDashboardUiState.Success -> {
                            Log.d(TAG, "Data loaded successfully. Meetings count: ${state.meetings.size}")
                            showLoading(false)
                            showErrorState(false)
                            showEmptyState(false)
                            adapter.submitList(state.meetings)
                        }

                        is MemberDashboardUiState.Empty -> {
                            Log.d(TAG, "No data available")
                            showLoading(false)
                            showErrorState(false)
                            showEmptyState(true)
                        }

                        is MemberDashboardUiState.Error -> {
                            Log.e(TAG, "Error loading data: ${state.message}")
                            showLoading(false)
                            showEmptyState(false)
                            showErrorState(true, state.message)
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.swipeRefreshLayout.isRefreshing = isLoading
    }

    private fun showEmptyState(show: Boolean) {
        // Toggle visibility of empty state and recycler view
        binding.layoutEmptyState.root.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvMeetings.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun setupSwipeToRefresh() {
        Log.d(TAG, "Setting up swipe to refresh")
        try {
            // Get theme colors
            val typedValue = android.util.TypedValue()
            val theme = requireContext().theme
            
            // Get primary color
            theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
            val primaryColor = typedValue.data
            
            // Get primary dark color
            theme.resolveAttribute(android.R.attr.colorPrimaryDark, typedValue, true)
            val primaryDarkColor = typedValue.data
            
            // Get accent color
            theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
            val accentColor = typedValue.data
            
            Log.d(TAG, "Theme colors - Primary: $primaryColor, Dark: $primaryDarkColor, Accent: $accentColor")
            
            // Set the color scheme for the refresh indicator
            binding.swipeRefreshLayout.setColorSchemeColors(
                primaryColor,
                primaryDarkColor,
                accentColor
            )
            
            binding.swipeRefreshLayout.setOnRefreshListener {
                Log.d(TAG, "Refresh triggered")
                viewModel.loadAssignedMeetings()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up swipe refresh", e)
        }
    }

    private fun showErrorState(show: Boolean, message: String? = null) {
        binding.layoutError.visibility = if (show) View.VISIBLE else View.GONE
        if (show && !message.isNullOrEmpty()) {
            binding.errorText.text = message
        }

        // Set up retry button
        binding.retryButton.setOnClickListener {
            viewModel.loadAssignedMeetings()
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        _binding = null
        super.onDestroyView()
    }
    
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }
}