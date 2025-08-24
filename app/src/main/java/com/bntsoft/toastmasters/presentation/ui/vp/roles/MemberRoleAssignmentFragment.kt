package com.bntsoft.toastmasters.presentation.ui.vp.roles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bntsoft.toastmasters.R
import androidx.navigation.fragment.findNavController
import com.bntsoft.toastmasters.data.model.Meeting
import com.bntsoft.toastmasters.databinding.FragmentMemberRoleAssignmentBinding
import com.bntsoft.toastmasters.domain.model.role.Role
import com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter.MemberRoleAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MemberRoleAssignmentFragment : Fragment() {
    private var _binding: FragmentMemberRoleAssignmentBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var currentUserId: String
    
    private val args: MemberRoleAssignmentFragmentArgs by navArgs()
    private val viewModel: MemberRoleAssignmentViewModel by viewModels()
    private lateinit var memberRoleAdapter: MemberRoleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberRoleAssignmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        
        // Load initial data
        viewModel.loadMembers(args.meeting)
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.assign_roles)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        memberRoleAdapter = MemberRoleAdapter { member, selectedRole ->
            viewModel.assignRole(member.userId, selectedRole, currentUserId)
        }
        binding.rvMembers.apply {
            adapter = memberRoleAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MemberRoleAssignmentViewModel.UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is MemberRoleAssignmentViewModel.UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    memberRoleAdapter.submitList(state.memberRoleItems)
                }
                is MemberRoleAssignmentViewModel.UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.message.isNotBlank()) {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        // If there was an error, refresh the data to ensure consistency
                        viewModel.loadMembers(args.meeting)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
