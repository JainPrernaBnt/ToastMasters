package com.bntsoft.toastmasters.presentation.ui.vp.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentMemberResponseBinding
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter.AvailableMemberAdapter
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter.BackoutMemberAdapter
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter.NotAvailableMemberAdapter
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter.NotConfirmedMemberAdapter
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter.NotRespondedMemberAdapter
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.viewmodel.MemberResponseViewModel
import com.bntsoft.toastmasters.utils.Constants.EXTRA_MEETING_ID
import com.bntsoft.toastmasters.utils.UiUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MemberResponseFragment : Fragment() {

    private var _binding: FragmentMemberResponseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MemberResponseViewModel by viewModels()

    private lateinit var availableMemberAdapter: AvailableMemberAdapter
    private lateinit var notAvailableMemberAdapter: NotAvailableMemberAdapter
    private lateinit var notConfirmedMemberAdapter: NotConfirmedMemberAdapter
    private lateinit var notRespondedMemberAdapter: NotRespondedMemberAdapter
    private lateinit var backoutMemberAdapter: BackoutMemberAdapter

    private var meetingId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberResponseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        meetingId = arguments?.getString(EXTRA_MEETING_ID) ?: run {
            UiUtils.showSnackbar(requireView(), "Meeting ID not found")
            parentFragmentManager.popBackStack()
            return
        }

        setupAdapters()
        setupRecyclerViews()
        observeViewModel()

        viewModel.loadMemberResponses(meetingId)
    }

    private fun setupAdapters() {
        availableMemberAdapter = AvailableMemberAdapter()
        notAvailableMemberAdapter = NotAvailableMemberAdapter()
        notConfirmedMemberAdapter = NotConfirmedMemberAdapter()
        notRespondedMemberAdapter = NotRespondedMemberAdapter()
        backoutMemberAdapter = BackoutMemberAdapter()

        val memberClickListener: (User) -> Unit = { user ->
            UiUtils.showSnackbar(requireView(), "${user.name} clicked")
        }

        availableMemberAdapter.setOnMemberClickListener(memberClickListener)
        notAvailableMemberAdapter.setOnMemberClickListener(memberClickListener)
        notConfirmedMemberAdapter.setOnMemberClickListener(memberClickListener)
        notRespondedMemberAdapter.setOnMemberClickListener(memberClickListener)
        backoutMemberAdapter.setOnMemberClickListener(memberClickListener)
    }

    private fun setupRecyclerViews() {
        with(binding) {
            // Available Members
            rvAvailableMembers.layoutManager = LinearLayoutManager(requireContext())
            rvAvailableMembers.adapter = availableMemberAdapter

            // Not Available Members
            rvNotAvailableMembers.layoutManager = LinearLayoutManager(requireContext())
            rvNotAvailableMembers.adapter = notAvailableMemberAdapter

            // Not Confirmed Members
            rvNotConfirmedMembers.layoutManager = LinearLayoutManager(requireContext())
            rvNotConfirmedMembers.adapter = notConfirmedMemberAdapter

            // Not Responded Members
            rvNotRespondedMembers.layoutManager = LinearLayoutManager(requireContext())
            rvNotRespondedMembers.adapter = notRespondedMemberAdapter

            // Backout Members
            backoutMembers.layoutManager = LinearLayoutManager(requireContext())
            backoutMembers.adapter = backoutMemberAdapter
        }
    }

    private fun observeViewModel() {
        Log.d("MemberResponseFrag", "Starting to observe ViewModel")
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                Log.d("MemberResponseFrag", "New UI State: ${state.javaClass.simpleName}")
                when (state) {
                    is MemberResponseViewModel.MemberResponseUiState.Loading -> {
                        Log.d("MemberResponseFrag", "Loading state")
                        // Show loading state
                    }

                    is MemberResponseViewModel.MemberResponseUiState.Success -> {
                        Log.d(
                            "MemberResponseFrag", "Success state - " +
                                    "Available: ${state.availableMembers.size}, " +
                                    "Not Available: ${state.notAvailableMembers.size}, " +
                                    "Not Confirmed: ${state.notConfirmedMembers.size}, " +
                                    "Not Responded: ${state.notRespondedMembers.size}"
                        )
                        updateUi(state)
                    }

                    is MemberResponseViewModel.MemberResponseUiState.Error -> {
                        Log.e("MemberResponseFrag", "Error: ${state.message}")
                        UiUtils.showSnackbar(requireView(), state.message)
                    }

                }
            }
        }
    }


    private fun updateUi(state: MemberResponseViewModel.MemberResponseUiState.Success) {
        Log.d(
            "MemberResponseFrag", "Updating UI with ${state.availableMembers.size} available, " +
                    "${state.notAvailableMembers.size} not available, " +
                    "${state.notConfirmedMembers.size} not confirmed, " +
                    "${state.notRespondedMembers.size} not responded"
        )

        // Log first few items in each list for debugging
        state.availableMembers.take(3).forEachIndexed { index, item ->
            Log.d("MemberResponseFrag", "Available[$index]: ${item.name} (${if (item.isGuest) "Guest" else "Member"})")
        }
        state.notAvailableMembers.take(3).forEachIndexed { index, item ->
            Log.d(
                "MemberResponseFrag",
                "Not Available[$index]: ${item.name} (${if (item.isGuest) "Guest" else "Member"})"
            )
        }
        state.notConfirmedMembers.take(3).forEachIndexed { index, item ->
            Log.d(
                "MemberResponseFrag",
                "Not Confirmed[$index]: ${item.name} (${if (item.isGuest) "Guest" else "Member"})"
            )
        }
        state.notRespondedMembers.take(3).forEachIndexed { index, item ->
            Log.d(
                "MemberResponseFrag",
                "Not Responded[$index]: ${item.name} (${if (item.isGuest) "Guest" else "Member"})"
            )
        }
        state.backoutMembers.take(3).forEachIndexed { index, item ->
            Log.d("MemberResponseFrag", "Backout[$index]: ${item.name} (${if (item.isGuest) "Guest" else "Member"})")
        }

        // Update all member lists
        availableMemberAdapter.submitList(state.availableMembers)
        notAvailableMemberAdapter.submitList(state.notAvailableMembers)
        notConfirmedMemberAdapter.submitList(state.notConfirmedMembers)
        notRespondedMemberAdapter.submitList(state.notRespondedMembers)
        backoutMemberAdapter.submitList(state.backoutMembers)

        Log.d("MemberResponseFrag", "Adapters updated with new lists")

        // Update all counts
        binding.apply {
            tvAvailableCount.text =
                getString(R.string.available_members_count, state.availableMembers.size)
            tvNotAvailableCount.text =
                getString(R.string.not_available_members_count, state.notAvailableMembers.size)
            tvNotConfirmedCount.text =
                getString(R.string.not_confirmed_members_count, state.notConfirmedMembers.size)
            tvNotRespondedCount.text =
                getString(R.string.not_responded_members_count, state.notRespondedMembers.size)
            backoutCount.text =
                getString(R.string.backout_members_count, state.backoutMembers.size)

            Log.d(
                "MemberResponseFrag", "Counts updated - " +
                        "Available: ${tvAvailableCount.text}, " +
                        "Not Available: ${tvNotAvailableCount.text}, " +
                        "Not Confirmed: ${tvNotConfirmedCount.text}, " +
                        "Not Responded: ${tvNotRespondedCount.text}"
            )
        }

        // Show/hide sections based on data
        binding.apply {
            cvAvailableMembers.visibility =
                if (state.availableMembers.isEmpty()) View.GONE else View.VISIBLE
            cvNotAvailableMembers.visibility =
                if (state.notAvailableMembers.isEmpty()) View.GONE else View.VISIBLE
            cvNotConfirmedMembers.visibility =
                if (state.notConfirmedMembers.isEmpty()) View.GONE else View.VISIBLE
            cvNotRespondedMembers.visibility =
                if (state.notRespondedMembers.isEmpty()) View.GONE else View.VISIBLE
            backoutMembersCard.visibility =
                if (state.backoutMembers.isEmpty()) View.GONE else View.VISIBLE

            // Show empty state if no data
            val hasData = state.availableMembers.isNotEmpty() ||
                    state.notAvailableMembers.isNotEmpty() ||
                    state.notConfirmedMembers.isNotEmpty() ||
                    state.notRespondedMembers.isNotEmpty() ||
                    state.backoutMembers.isNotEmpty()

            groupEmpty.visibility = if (hasData) View.GONE else View.VISIBLE
            scrollView.visibility = if (hasData) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(meetingId: String): MemberResponseFragment {
            return MemberResponseFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_MEETING_ID, meetingId)
                }
            }
        }
    }
}