package com.bntsoft.toastmasters.presentation.ui.vp.roles

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.databinding.FragmentMemberRoleAssignBinding
import com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter.MemberRoleAssignAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MemberRoleAssignFragment : Fragment() {

    private var _binding: FragmentMemberRoleAssignBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MemberRoleAssignViewModel by viewModels()
    private lateinit var adapter: MemberRoleAssignAdapter
    private var currentMeetingId: String = ""
    
    // Flags to track data loading states
    private var isRoleAssignmentsLoaded = false
    private var isAssignableRolesLoaded = false
    private var isAvailableMembersLoaded = false
    private var isRecentRolesLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberRoleAssignBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("MemberRoleAssignFrag", "onViewCreated: Fragment view created")

        arguments?.let { args ->
            currentMeetingId = args.getString("meeting_id") ?: ""
        }

        if (currentMeetingId.isBlank()) {
            Log.e("MemberRoleAssignFrag", "No meeting ID provided")
            return@onViewCreated
        }

        // Show loading indicator, hide members and button initially
        binding.progressBar.visibility = View.VISIBLE
        binding.rvMembers.visibility = View.GONE

        setupRecyclerView()
        observeViewModel()
        viewModel.loadRoleAssignments(currentMeetingId)

    }

    private fun setupRecyclerView() {

        adapter = MemberRoleAssignAdapter().apply {
            setCallbacks(
                onRoleSelected = { userId, role ->
                    handleRoleSelected(userId, role)
                },
                onRoleRemoved = { userId, role ->
                    handleRoleRemoved(userId, role)
                },
                onBackupMemberSelected = { userId, backupMemberId ->
                    viewModel.setBackupMember(userId, backupMemberId)
                },
                onSaveRoles = { userId, _ ->
                    viewModel.saveRoleAssignments()
                },
                onToggleEditMode = { userId, isEditable ->
                    viewModel.toggleEditMode(userId, isEditable)
                },
                onEvaluatorSelected = { speakerId, evaluatorId ->
                    viewModel.assignEvaluator(speakerId, evaluatorId)
                }
            )
            // Initial setup with empty lists, will be updated in observeViewModel
            updateAssignableRoles(emptyList())
            updateAvailableMembers(viewModel.availableMembers.value ?: emptyList())
        }

        binding.rvMembers.adapter = adapter
        binding.rvMembers.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun handleRoleSelected(userId: String, role: String) {
        viewModel.assignRole(userId, role)
        // Refresh the role list to update availability
        val roleItems = viewModel.getAvailableRoles(userId)
        adapter.updateAssignableRoles(roleItems)
    }

    private fun handleRoleRemoved(userId: String, role: String) {
        viewModel.removeRole(userId, role)
        // Refresh the role list to update availability
        val roleItems = viewModel.getAvailableRoles(userId)
        adapter.updateAssignableRoles(roleItems)
    }

    private fun checkAllDataLoaded() {
        if (isRoleAssignmentsLoaded && isAssignableRolesLoaded && 
            isAvailableMembersLoaded && isRecentRolesLoaded) {
            // All data loaded, hide progress bar and show RecyclerView
            binding.progressBar.visibility = View.GONE
            binding.rvMembers.visibility = View.VISIBLE
        }
    }

    private fun observeViewModel() {
        viewModel.roleAssignments.observe(viewLifecycleOwner) { assignments ->
            isRoleAssignmentsLoaded = true
            adapter.submitList(assignments)
            // Update available roles for each user when assignments change
            assignments.forEach { assignment ->
                val roleItems = viewModel.getAvailableRoles(assignment.userId)
                adapter.updateAssignableRoles(roleItems)
            }
            checkAllDataLoaded()
        }

        viewModel.assignableRoles.observe(viewLifecycleOwner) { roles ->
            isAssignableRolesLoaded = true
            roles?.let { roleMap ->
                // Update roles for all users in the adapter
                viewModel.roleAssignments.value?.forEach { assignment ->
                    val availableRoles = viewModel.getAvailableRoles(assignment.userId)
                    adapter.updateAssignableRoles(availableRoles)
                }
            }
            checkAllDataLoaded()
        }

        viewModel.availableMembers.observe(viewLifecycleOwner) { members ->
            isAvailableMembersLoaded = true
            adapter.updateAvailableMembers(members)
            checkAllDataLoaded()
        }

        viewModel.recentRoles.observe(viewLifecycleOwner) { recentRoles ->
            isRecentRolesLoaded = true
            adapter.setRecentRoles(recentRoles)
            checkAllDataLoaded()
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                android.widget.Toast.makeText(
                    requireContext(),
                    it,
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }

        viewModel.evaluatorAssigned.observe(viewLifecycleOwner) { (speakerId, evaluatorId) ->
            if (speakerId.isNotBlank()) {
                // Update the UI to show the selected evaluator
                // The actual assignment is already handled in the adapter
                // No need to show the prompt here as the evaluator is already selected
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvMembers.adapter = null
    }
}