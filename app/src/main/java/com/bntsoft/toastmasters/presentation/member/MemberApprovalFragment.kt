package com.bntsoft.toastmasters.presentation.member

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentMemberApprovalBinding
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.models.MemberApprovalFilter
import com.bntsoft.toastmasters.presentation.member.adapter.MemberApprovalAdapter
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

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Set up tabs for filtering
        setupTabs()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_member_approval, menu)

        // Set up search functionality
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filterMembers(newText.orEmpty())
                return true
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refresh()
                true
            }

            R.id.action_filter -> {
                showFilterDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupToolbar() {
        setHasOptionsMenu(true)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = MemberApprovalAdapter(
            onApproveClick = { member ->
                showApproveConfirmationDialog(member)
            },
            onRejectClick = { member ->
                showRejectConfirmationDialog(member)
            },
            onAssignMentorClick = { member ->
                showMentorSelectionDialog(member)
            },
            onMentorClick = { member ->
                // Show mentor details or edit mentor
                showMentorSelectionDialog(member)
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

    private fun setupClickListeners() {
        binding.approveAllButton.setOnClickListener {
            showApproveAllConfirmationDialog()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.setFilter(MemberApprovalFilter.ALL)
                    1 -> viewModel.setFilter(MemberApprovalFilter.NEW_MEMBERS)
                    2 -> viewModel.setFilter(MemberApprovalFilter.PENDING_APPROVAL)
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // Set default tab
        binding.tabLayout.getTabAt(1)?.select()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe members list
                viewModel.members.collectLatest { members ->
                    adapter.submitList(members)
                    updateEmptyState(members.isEmpty())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe loading state
                viewModel.isLoading.collectLatest { isLoading ->
                    binding.progressBar.isVisible = isLoading
                    binding.approveAllButton.isEnabled = !isLoading
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
        binding.approveAllButton.isVisible =
            !isEmpty && viewModel.currentFilter == MemberApprovalFilter.PENDING_APPROVAL
    }

    private fun showApproveConfirmationDialog(member: User) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.approve_member_title)
            .setMessage(R.string.approve_member_message)
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

    private fun showApproveAllConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.approve_all_title)
            .setMessage(R.string.approve_all_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.approveAll()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showMentorSelectionDialog(member: User) {
        // TODO: Implement mentor selection dialog
        // This should show a list of available mentors and allow selection
        // For now, we'll just show a placeholder message
        showSuccessMessage("Mentor selection will be implemented here")
    }

    private fun showFilterDialog() {
        val filters = arrayOf(
            getString(R.string.filter_all),
            getString(R.string.filter_new_members),
            getString(R.string.filter_pending_approval)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filter_members)
            .setSingleChoiceItems(filters, viewModel.currentFilter.ordinal) { dialog, which ->
                viewModel.setFilter(MemberApprovalFilter.values()[which])
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showErrorSnackbar(message: String) {
        UiUtils.showErrorWithRetry(
            view = requireView(),
            message = message,
            retryAction = { viewModel.refresh() }
        )
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