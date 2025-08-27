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

        setupRecyclerView()
        observeViewModel()
        viewModel.loadRoleAssignments()

        // Log the current state
        Log.d("MemberRoleAssignFrag", "View binding root visibility: ${binding.root.visibility}")
        Log.d("MemberRoleAssignFrag", "RecyclerView visibility: ${binding.rvMembers.visibility}")
    }

    private fun setupRecyclerView() {
        Log.d("MemberRoleAssignFrag", "Setting up RecyclerView")

        adapter = MemberRoleAssignAdapter(
            assignableRoles = viewModel.assignableRoles.value ?: emptyList(),
            onRoleSelected = { userId, role ->
                Log.d("MemberRoleAssignFrag", "Role selected - UserId: $userId, Role: $role")
                viewModel.assignRole(userId, role)
            },
            onBackupMemberSelected = { userId, backupMemberId ->
                Log.d("MemberRoleAssignFrag", "Backup member selected - UserId: $userId, BackupId: $backupMemberId")
                viewModel.setBackupMember(userId, backupMemberId)
            },
            availableMembers = viewModel.availableMembers.value ?: emptyList(),
            onSaveRole = { userId, _ ->
                Log.d("MemberRoleAssignFrag", "Saving role for user: $userId")
                viewModel.saveRoleAssignments()
            },
            onToggleEditMode = { userId, isEditable ->
                Log.d("MemberRoleAssignFrag", "Toggling edit mode - UserId: $userId, isEditable: $isEditable")
                viewModel.toggleEditMode(userId, isEditable)
            }
        )

        binding.rvMembers.adapter = adapter
        binding.rvMembers.layoutManager = LinearLayoutManager(requireContext())

        Log.d("MemberRoleAssignFrag", "RecyclerView setup complete. Adapter: $adapter")
    }

    private fun observeViewModel() {
        Log.d("MemberRoleAssignFrag", "Setting up ViewModel observers")

        viewModel.roleAssignments.observe(viewLifecycleOwner) { assignments ->
            if (assignments == null) {
                Log.d("MemberRoleAssignFrag", "Role assignments are null. Waiting for data...")
                return@observe
            }

            Log.d("MemberRoleAssignFrag", "Role assignments updated. Count: ${assignments.size}")
            if (assignments.isEmpty()) {
                Log.d("MemberRoleAssignFrag", "No role assignments received. Check if data is loading or if there was an error.")
            }
            adapter.submitList(assignments) {
                Log.d("MemberRoleAssignFrag", "Adapter list updated. New item count: ${adapter.itemCount}")
                if (adapter.itemCount == 0) {
                    Log.d("MemberRoleAssignFrag", "WARNING: Adapter list is empty after submission")
                }
            }
        }

        viewModel.assignableRoles.observe(viewLifecycleOwner) { roles ->
            Log.d("MemberRoleAssignFrag", "Assignable roles updated. Count: ${roles.size}")
            if (roles.isEmpty()) {
                Log.d("MemberRoleAssignFrag", "WARNING: No assignable roles received")
            }
            adapter.updateAssignableRoles(roles)
        }

        viewModel.availableMembers.observe(viewLifecycleOwner) { members ->
            Log.d("MemberRoleAssignFrag", "Available members updated. Count: ${members.size}")
            // Update the adapter with the new list of available members
            adapter.updateAvailableMembers(members)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Show error message to user
                android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvMembers.adapter = null
    }
}