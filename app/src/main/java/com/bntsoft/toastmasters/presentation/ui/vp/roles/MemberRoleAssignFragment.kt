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

        // Log the current state
        Log.d("MemberRoleAssignFrag", "View binding root visibility: ${binding.root.visibility}")
        Log.d("MemberRoleAssignFrag", "RecyclerView visibility: ${binding.rvMembers.visibility}")
    }

    private fun setupRecyclerView() {
        Log.d("MemberRoleAssignFrag", "Setting up RecyclerView")

        adapter = MemberRoleAssignAdapter().apply {
            setCallbacks(
                onRoleSelected = { userId, role ->
                    Log.d("MemberRoleAssignFrag", "Role selected - UserId: $userId, Role: $role")
                    handleRoleSelected(userId, role)
                },
                onRoleRemoved = { userId, role ->
                    Log.d("MemberRoleAssignFrag", "Role removed - UserId: $userId, Role: $role")
                    handleRoleRemoved(userId, role)
                },
                onBackupMemberSelected = { userId, backupMemberId ->
                    Log.d(
                        "MemberRoleAssignFrag",
                        "Backup member selected - UserId: $userId, BackupId: $backupMemberId"
                    )
                    viewModel.setBackupMember(userId, backupMemberId)
                },
                onSaveRoles = { userId, _ ->
                    Log.d("MemberRoleAssignFrag", "Saving role for user: $userId")
                    viewModel.saveRoleAssignments()
                },
                onToggleEditMode = { userId, isEditable ->
                    Log.d(
                        "MemberRoleAssignFrag",
                        "Toggling edit mode - UserId: $userId, isEditable: $isEditable"
                    )
                    viewModel.toggleEditMode(userId, isEditable)
                },
                onEvaluatorSelected = { speakerId, evaluatorId ->
                    Log.d(
                        "MemberRoleAssignFrag",
                        "Evaluator selected - SpeakerId: $speakerId, EvaluatorId: $evaluatorId"
                    )
                    viewModel.assignEvaluator(speakerId, evaluatorId)
                }
            )
            // Initial setup with empty lists, will be updated in observeViewModel
            updateAssignableRoles(emptyList())
            updateAvailableMembers(viewModel.availableMembers.value ?: emptyList())
        }

        binding.rvMembers.adapter = adapter
        binding.rvMembers.layoutManager = LinearLayoutManager(requireContext())

        Log.d("MemberRoleAssignFrag", "RecyclerView setup complete. Adapter: $adapter")
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

    private fun observeViewModel() {
        viewModel.roleAssignments.observe(viewLifecycleOwner) { assignments ->
            adapter.submitList(assignments)
            // Update available roles for each user when assignments change
            assignments.forEach { assignment ->
                val roleItems = viewModel.getAvailableRoles(assignment.userId)
                adapter.updateAssignableRoles(roleItems)
            }
        }

        viewModel.assignableRoles.observe(viewLifecycleOwner) { roles ->
            roles?.let { roleMap ->
                Log.d("MemberRoleAssignFrag", "Assignable roles updated. Count: ${roleMap.size}")
                // Update roles for all users in the adapter
                viewModel.roleAssignments.value?.forEach { assignment ->
                    val availableRoles = viewModel.getAvailableRoles(assignment.userId)
                    adapter.updateAssignableRoles(availableRoles)
                }
            }
        }

        viewModel.availableMembers.observe(viewLifecycleOwner) { members ->
            Log.d("MemberRoleAssignFrag", "Available members updated. Count: ${members.size}")
            adapter.updateAvailableMembers(members)
            // Hide loading, show members and button when loaded
            binding.progressBar.visibility = View.GONE
            binding.rvMembers.visibility = View.VISIBLE
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