package com.bntsoft.toastmasters.presentation.ui.vp.reports

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bntsoft.toastmasters.databinding.FragmentReportsBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportsViewModel by viewModels()

    // Meeting Reports Group
    private val meetingReportsViews by lazy {
        listOf(
            binding.cardMeetingSummary,
            binding.cardRoleAssigned,
            binding.cardAverageParticipation,
            binding.cardMeetingAttendance
        )
    }

    // Member Reports Group
    private val memberReportsViews by lazy {
        listOf(
            binding.cardUserWise,
            binding.cardMembersRoleFrequency,
            binding.cardNonParticipatingMembers,
            binding.cardTopActiveMembers,
            binding.cardBackoutMembers,
            binding.cardMembershipStatus
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryButtons()
        setupClickListeners()

        selectCategory(viewModel.isMeetingSelected)
    }

    private fun setupCategoryButtons() {
        binding.btnMeetingsReports.setOnClickListener {
            viewModel.isMeetingSelected = true
            selectCategory(true)
        }
        binding.btnMembersReports.setOnClickListener {
            viewModel.isMeetingSelected = false
            selectCategory(false)
        }
    }

    private fun selectCategory(isMeetingSelected: Boolean) {
        // Save state
        viewModel.isMeetingSelected = isMeetingSelected

        meetingReportsViews.forEach { it.isVisible = false }
        memberReportsViews.forEach { it.isVisible = false }

        if (isMeetingSelected) {
            switchToMeetingsReports()
            updateCategoryUI(binding.btnMeetingsReports, binding.btnMembersReports)
        } else {
            switchToMembersReports()
            updateCategoryUI(binding.btnMembersReports, binding.btnMeetingsReports)
        }
    }

    private fun updateCategoryUI(selected: MaterialButton, unselected: MaterialButton) {
        val colorPrimaryContainer = MaterialColors.getColor(
            selected,
            com.google.android.material.R.attr.colorPrimaryContainer
        )
        val colorOnSurfaceVariant = MaterialColors.getColor(
            selected,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )

        // Selected button
        selected.backgroundTintList = ColorStateList.valueOf(colorPrimaryContainer)
        selected.setTextColor(colorOnSurfaceVariant)

        // Unselected button
        unselected.backgroundTintList =
            ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        unselected.setTextColor(colorOnSurfaceVariant)
    }

    private fun switchToMeetingsReports() {
        binding.cardMeetingSummary.isVisible = true
        binding.cardRoleAssigned.isVisible = true
        binding.cardAverageParticipation.isVisible = true
        binding.cardMeetingAttendance.isVisible = true

        binding.cardUserWise.isVisible = false
        binding.cardMembersRoleFrequency.isVisible = false
        binding.cardNonParticipatingMembers.isVisible = false
        binding.cardTopActiveMembers.isVisible = false
        binding.cardBackoutMembers.isVisible = false
        binding.cardMembershipStatus.isVisible = false
    }

    private fun switchToMembersReports() {
        binding.cardMeetingSummary.isVisible = false
        binding.cardRoleAssigned.isVisible = false
        binding.cardAverageParticipation.isVisible = false
        binding.cardMeetingAttendance.isVisible = false

        binding.cardUserWise.isVisible = true
        binding.cardMembersRoleFrequency.isVisible = true
        binding.cardNonParticipatingMembers.isVisible = true
        binding.cardTopActiveMembers.isVisible = true
        binding.cardBackoutMembers.isVisible = true
        binding.cardMembershipStatus.isVisible = true
    }


    private fun setupClickListeners() {
        // Meeting reports
        binding.cardMeetingSummary.setOnClickListener { navigateToReport("MEETING_SUMMARY") }
        binding.cardRoleAssigned.setOnClickListener { navigateToReport("ROLE_ASSIGNED") }
        binding.cardAverageParticipation.setOnClickListener { navigateToReport("AVERAGE_PARTICIPATION") }
        binding.cardMeetingAttendance.setOnClickListener { navigateToReport("MEETING_ATTENDANCE") }

        // Member reports
        binding.cardUserWise.setOnClickListener { navigateToReport("USER_WISE") }
        binding.cardMembersRoleFrequency.setOnClickListener { navigateToReport("MEMBERS_ROLE_FREQUENCY") }
        binding.cardNonParticipatingMembers.setOnClickListener { navigateToReport("NON_PARTICIPATING_MEMBERS") }
        binding.cardTopActiveMembers.setOnClickListener { navigateToReport("TOP_ACTIVE_MEMBERS") }
        binding.cardBackoutMembers.setOnClickListener { navigateToReport("BACKOUT_MEMBERS") }
        binding.cardMembershipStatus.setOnClickListener { navigateToReport("MEMBERSHIP_STATUS") }
    }

    private fun navigateToReport(reportId: String) {
        val action =
            ReportsFragmentDirections.actionReportsFragmentToReportsDataFragment(reportId = reportId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
