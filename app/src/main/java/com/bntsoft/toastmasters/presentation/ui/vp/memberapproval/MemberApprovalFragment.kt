package com.bntsoft.toastmasters.presentation.ui.vp.memberapproval

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentMemberApprovalBinding
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.presentation.ui.vp.memberapproval.adapter.MemberApprovalAdapter
import com.bntsoft.toastmasters.utils.UiUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MemberApprovalFragment : Fragment() {

    private var _binding: FragmentMemberApprovalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MemberApprovalViewModel by viewModels()
    private lateinit var adapter: MemberApprovalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberApprovalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use Activity toolbar; hide Activity menu icons on this screen
        setHasOptionsMenu(true)
        setupRecyclerView()
        observeViewModel()

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Hide activity menu items for this screen
        menu.findItem(R.id.action_reports)?.isVisible = false
        menu.findItem(R.id.action_settings)?.isVisible = false
        menu.findItem(R.id.action_notifications)?.isVisible = false
    }

    private fun setupRecyclerView() {
        adapter = MemberApprovalAdapter(
            onApproveClick = { member ->
                showApproveConfirmationDialog(member)
            },
            onRejectClick = { member ->
                showRejectConfirmationDialog(member)
            },
            onApplyMentors = { member, names ->
                viewModel.assignMentor(member, names)
            }
        )

        binding.membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            this.adapter = this@MemberApprovalFragment.adapter

            // Add divider between items
            val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            addItemDecoration(divider)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe filtered members list
                viewModel.filteredMembers.collectLatest { members ->
                    // Filter out approved members
                    val pendingMembers = members.filter { !it.isApproved }
                    adapter.submitList(pendingMembers)
                    updateEmptyState(pendingMembers.isEmpty())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe loading state
                viewModel.isLoading.collectLatest { isLoading ->
                    binding.progressBar.isVisible = isLoading

                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe error messages
                viewModel.errorMessages.collectLatest { message ->
                    if (message.isNotBlank()) {
                        showErrorSnackbar(binding.root, message)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe success messages
                viewModel.successMessages.collectLatest { message ->
                    if (message.isNotBlank()) {
                        showSuccessMessage(message)
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateView.isVisible = isEmpty
        binding.membersRecyclerView.isVisible = !isEmpty
    }

    private fun showApproveConfirmationDialog(member: User) {
        val mentorNamesInput = member.mentorNames.joinToString(", ")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.approve_member_title)
            .setMessage("Approve ${member.name} with mentors: $mentorNamesInput ?")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.approveMember(member)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }


    private fun showRejectConfirmationDialog(member: User) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reject_member_title)
            .setMessage(R.string.reject_member_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.rejectMember(member)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }


    private fun showErrorSnackbar(view: View, message: String) {
        UiUtils.showErrorWithRetry(
            view = view,
            message = message,
            retryAction = { viewModel.refresh() }
        )
    }

    private fun showSuccessMessage(message: String) {
        UiUtils.showSuccessMessage(requireView(), message)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}