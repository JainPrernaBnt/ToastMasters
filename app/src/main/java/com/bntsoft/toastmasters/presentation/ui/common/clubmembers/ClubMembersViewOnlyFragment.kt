package com.bntsoft.toastmasters.presentation.ui.common.clubmembers

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.databinding.FragmentClubMembersViewOnlyBinding
import com.bntsoft.toastmasters.presentation.ui.vp.clubmembers.ClubMembersViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ClubMembersViewOnlyFragment : Fragment() {
    private var _binding: FragmentClubMembersViewOnlyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ClubMembersViewModel by viewModels()
    private lateinit var membersAdapter: ClubMembersViewOnlyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClubMembersViewOnlyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchFunctionality()
        observeUiState()
    }

    private fun setupRecyclerView() {
        membersAdapter = ClubMembersViewOnlyAdapter()
        
        binding.recyclerViewMembers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = membersAdapter
        }
    }

    private fun setupSearchFunctionality() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                viewModel.searchMembers(query)
            }
        })
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                
                if (state.filteredMembers.isNotEmpty()) {
                    binding.recyclerViewMembers.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                    membersAdapter.submitList(state.filteredMembers)
                } else if (!state.isLoading) {
                    binding.recyclerViewMembers.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                }
                
                state.error?.let { error ->
                    showError(error)
                    viewModel.clearError()
                }
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
