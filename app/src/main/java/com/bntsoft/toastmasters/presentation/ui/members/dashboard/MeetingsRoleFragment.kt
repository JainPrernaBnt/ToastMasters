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
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.databinding.FragmentMeetingsRoleBinding
import com.bntsoft.toastmasters.data.model.MeetingRoleItem
import com.bntsoft.toastmasters.presentation.ui.members.dashboard.adapter.MeetingsRoleAdapter
import com.bntsoft.toastmasters.utils.UiState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MeetingsRoleFragment : Fragment() {

    private var _binding: FragmentMeetingsRoleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MeetingsRoleViewModel by viewModels()
    private lateinit var adapter: MeetingsRoleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeetingsRoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val meetingId = arguments?.getString("meetingId")
        Log.d("MeetingsRoleFragment", "Received meetingId: $meetingId")

        setupRecyclerView()
        setupSwipeRefresh()

        meetingId?.let { loadData(it) }
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = MeetingsRoleAdapter()
        binding.rvMeetings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@MeetingsRoleFragment.adapter
            itemAnimator = null // Disable animations to prevent flickering when updating the list
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            arguments?.getString("meetingId")?.let { loadData(it) }
        }
    }

    private fun loadData(meetingId: String) {
        viewModel.loadMemberRoles(meetingId)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            showLoading(true)
                            binding.layoutEmptyState.root.visibility = View.GONE
                            binding.layoutError.visibility = View.GONE
                        }
                        is UiState.Success -> {
                            showLoading(false)
                            binding.layoutEmptyState.root.visibility = View.GONE
                            binding.layoutError.visibility = View.GONE
                            adapter.submitList(state.data)
                        }
                        is UiState.Empty -> {
                            showLoading(false)
                            binding.layoutEmptyState.root.visibility = View.VISIBLE
                            binding.layoutError.visibility = View.GONE
                            adapter.submitList(emptyList())
                        }
                        is UiState.Error -> {
                            showLoading(false)
                            binding.layoutError.visibility = View.VISIBLE
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.swipeRefreshLayout.isRefreshing = isLoading
    }

    private fun showError(message: String) {
        view?.let { view ->
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}