package com.bntsoft.toastmasters.presentation.ui.members.upcoming

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.ItemUpcomingMeetingBinding
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingAvailability
import com.bntsoft.toastmasters.domain.models.AvailabilityStatus
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private class MeetingDiffCallback : DiffUtil.ItemCallback<Meeting>() {
    override fun areItemsTheSame(oldItem: Meeting, newItem: Meeting): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Meeting, newItem: Meeting): Boolean {
        return oldItem == newItem
    }
}

class UpcomingMeetingsAdapter :
    ListAdapter<Meeting, UpcomingMeetingsAdapter.MeetingViewHolder>(MeetingDiffCallback()) {

    var onAvailabilitySubmitted: ((meetingId: String, status: AvailabilityStatus, preferredRoles: List<String>, isBackout: Boolean) -> Unit)? = null
    var onEditClicked: ((Meeting) -> Unit)? = null

    // Add current user ID property
    var currentUserId: String = ""

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    private fun getPreviousMonday(meetingTime: java.time.LocalDateTime): java.time.LocalDateTime {
        var date = meetingTime.toLocalDate()
        while (date.dayOfWeek != java.time.DayOfWeek.MONDAY) {
            date = date.minusDays(1)
        }
        return date.atTime(meetingTime.toLocalTime())
    }
    
    private fun canEditAvailability(meetingTime: java.time.LocalDateTime): Boolean {
        val currentTime = java.time.LocalDateTime.now()
        val mondayBeforeMeeting = getPreviousMonday(meetingTime)
        return currentTime.isBefore(mondayBeforeMeeting)
    }
    
    private fun isAfterCutoff(meetingTime: java.time.LocalDateTime): Boolean {
        val currentTime = java.time.LocalDateTime.now()
        val mondayBeforeMeeting = getPreviousMonday(meetingTime)
        return currentTime.isAfter(mondayBeforeMeeting) && currentTime.isBefore(meetingTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingViewHolder {
        val binding = ItemUpcomingMeetingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MeetingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MeetingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MeetingViewHolder(
        private val binding: ItemUpcomingMeetingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val btnBackout = binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBackout)

        private var isAvailable = false
        private val selectedRoles = linkedMapOf<String, Int>() // Role to priority map
        private var currentStatus: AvailabilityStatus = AvailabilityStatus.NOT_AVAILABLE
        private var nextPriority = 1

        fun bind(meeting: Meeting, availability: MeetingAvailability? = null) {
            setupInitialState()
            itemView.tag = meeting

            val canEdit = canEditAvailability(meeting.dateTime)
            val isCutoff = isAfterCutoff(meeting.dateTime)

            // Use the availability passed as param (from ViewModel)
            val userAvailability = availability ?: meeting.availability?.takeIf { it.userId == currentUserId }

            binding.apply {
                // --- Basic Info ---
                tvMeetingTitle.text = meeting.theme.ifEmpty { itemView.context.getString(R.string.meeting_title) }
                tvMeetingDate.text = meeting.dateTime.format(dateFormatter)
                tvMeetingTime.text = "${meeting.dateTime.format(timeFormatter)} - ${meeting.endDateTime?.format(timeFormatter) ?: ""}"
                tvMeetingLocation.text = meeting.location.ifEmpty { itemView.context.getString(R.string.venue_not_specified) }

                // --- Decide Form vs Read-only ---
                val showForm = canEdit && (userAvailability == null || meeting.isEditMode)

                if (showForm) {
                    // Show form (radio buttons + submit)
                    rgAvailability.visibility = View.VISIBLE
                    btnSubmit.visibility = View.VISIBLE
                    tvRoleInstructions.visibility = View.VISIBLE
                    tvRoleInstructions.text =
                        "Select your preferred roles in order of priority.\nThe first role you select will be your highest priority."

                    rgAvailability.clearCheck()
                    when (userAvailability?.status) {
                        AvailabilityStatus.AVAILABLE -> rgAvailability.check(R.id.rbAvailable)
                        AvailabilityStatus.NOT_CONFIRMED -> rgAvailability.check(R.id.rbNotConfirmed)
                        AvailabilityStatus.NOT_AVAILABLE, null -> rgAvailability.check(R.id.rbNotAvailable)
                    }

                    if (meeting.preferredRoles.isNotEmpty()) {
                        setupPreferredRoles(meeting.preferredRoles)
                        userAvailability?.preferredRoles?.let { roles ->
                            selectedRoles.clear()
                            roles.forEachIndexed { index, role -> selectedRoles[role] = index + 1 }
                            nextPriority = selectedRoles.size + 1
                            cgRoles.visibility = View.VISIBLE
                            updateRoleChips()
                            updateSelectedRolesText()
                        }
                    }
                } else {
                    // Read-only view
                    displayRoleStatus(meeting)
                    tvAvailabilityStatus.visibility = View.VISIBLE
                    tvYourAvailability.visibility = View.GONE
                    tvPreferredRoles.visibility = View.GONE
                    rgAvailability.visibility = View.GONE
                    btnSubmit.visibility = View.GONE
                    val statusText = when (userAvailability?.status) {
                        AvailabilityStatus.AVAILABLE -> "Available"
                        AvailabilityStatus.NOT_CONFIRMED -> "Not Confirmed"
                        AvailabilityStatus.NOT_AVAILABLE, null -> "Not Available"
                    }
                    tvAvailabilityStatus.text = "Your Availability: $statusText"

                    // Preferred roles
                    if (userAvailability?.status == AvailabilityStatus.AVAILABLE &&
                        userAvailability.preferredRoles.isNotEmpty()
                    ) {
                        tvSelectedRoles.visibility = View.VISIBLE
                        tvSelectedRoles.text = "Preferred Roles: ${userAvailability.preferredRoles.joinToString(", ")}"
                    }

                    // Edit/backout buttons
                    if (isCutoff) {
                        btnBackout.visibility = View.VISIBLE
                        btnBackout.setOnClickListener {
                            MaterialAlertDialogBuilder(itemView.context)
                                .setTitle("Backout from Meeting")
                                .setMessage("Are you sure you want to back out from this meeting? This will remove you from any assigned roles.")
                                .setPositiveButton("Yes, Backout") { _, _ ->
                                    onAvailabilitySubmitted?.invoke(
                                        meeting.id,
                                        AvailabilityStatus.NOT_AVAILABLE,
                                        emptyList(),
                                        true // backout mode
                                    )
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    } else {
                        btnEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
                        btnEdit.setOnClickListener { onEditClicked?.invoke(meeting) }
                    }
                }

                // --- RadioGroup Listener ---
                rgAvailability.setOnCheckedChangeListener { _, checkedId ->
                    currentStatus = when (checkedId) {
                        R.id.rbAvailable -> AvailabilityStatus.AVAILABLE
                        R.id.rbNotConfirmed -> AvailabilityStatus.NOT_CONFIRMED
                        else -> AvailabilityStatus.NOT_AVAILABLE
                    }
                    if (checkedId != R.id.rbAvailable) {
                        selectedRoles.clear()
                        cgRoles.clearCheck()
                    }
                    updatePreferredRolesVisibility()
                }

                // --- Submit Button ---
                btnSubmit.setOnClickListener {
                    val status = when (rgAvailability.checkedRadioButtonId) {
                        R.id.rbAvailable -> AvailabilityStatus.AVAILABLE
                        R.id.rbNotAvailable -> AvailabilityStatus.NOT_AVAILABLE
                        R.id.rbNotConfirmed -> AvailabilityStatus.NOT_CONFIRMED
                        else -> AvailabilityStatus.NOT_AVAILABLE
                    }
                    onAvailabilitySubmitted?.invoke(meeting.id, status, selectedRoles.keys.toList(), false)
                }
            }
        }

        private fun setupInitialState() {
            binding.apply {
                // Reset form state
                cgRoles.visibility = View.GONE
                tvNoRoles.visibility = View.GONE
                selectedRoles.clear()

                // Set default selection to Not Available if nothing is selected
                if (rgAvailability.checkedRadioButtonId == -1) {
                    rgAvailability.check(R.id.rbNotAvailable)
                    currentStatus = AvailabilityStatus.NOT_AVAILABLE
                }

                // Clear any existing chips
                cgRoles.removeAllViews()
            }
        }

        private fun updatePreferredRolesVisibility() {
            binding.apply {
                val showRoles = currentStatus == AvailabilityStatus.AVAILABLE

                if (showRoles) {
                    // Show preferred roles only when Available is selected
                    if (cgRoles.childCount > 0) {
                        cgRoles.visibility = View.VISIBLE
                        tvNoRoles.visibility = View.GONE
                    } else {
                        tvNoRoles.visibility = View.VISIBLE
                        cgRoles.visibility = View.GONE
                    }
                } else {
                    // Clear selection and hide for Not Available or Not Confirmed
                    cgRoles.visibility = View.GONE
                    tvNoRoles.visibility = View.GONE

                    // Clear any selected roles when not in Available state
                    selectedRoles.clear()
                    cgRoles.clearCheck()
                }
            }
        }

        private fun updateChipAppearance(chip: Chip, isSelected: Boolean) {
            chip.apply {
                if (isSelected) {
                    setChipBackgroundColorResource(R.color.purple_200)
                    setTextColor(resources.getColor(R.color.white, null))
                } else {
                    setChipBackgroundColorResource(R.color.chip_background)
                    setTextAppearance(R.style.ChipTextAppearance)
                }
            }
        }

        private fun setupPreferredRoles(roles: List<String>) {
            binding.apply {
                cgRoles.removeAllViews()

                if (roles.isEmpty()) {
                    updatePreferredRolesVisibility()
                    return@apply
                }

                val meeting = (itemView.tag as? Meeting) ?: return@apply
                val roleCounts = meeting.roleCounts ?: return
                val assignedCounts = meeting.assignedCounts ?: emptyMap()

                roleCounts.forEach { (baseRole, totalCount) ->
                    val assigned = assignedCounts.filterKeys { it.startsWith(baseRole) }.values.sum()

                    val isFullyAssigned = assigned >= totalCount

                    val chip = Chip(itemView.context).apply {
                        text = "$baseRole ($assigned/$totalCount)"
                        isCheckable = !isFullyAssigned
                        isClickable = !isFullyAssigned
                        isEnabled = !isFullyAssigned

                        if (!isFullyAssigned) {
                            setOnClickListener {
                                if (selectedRoles.containsKey(baseRole)) {
                                    selectedRoles.remove(baseRole)
                                } else {
                                    selectedRoles[baseRole] = nextPriority++
                                }
                                updateRoleChips()
                                updateSelectedRolesText()
                            }
                        }
                    }
                    cgRoles.addView(chip)
                }
                updateRoleChips()
                updateSelectedRolesText()
            }
        }

        private fun updateRoleChips() {
            for (i in 0 until binding.cgRoles.childCount) {
                val chip = binding.cgRoles.getChildAt(i) as Chip
                val currentText = chip.text.toString()

                // Strip any existing priority like "1. " to get the clean role text
                val roleText = currentText.substringAfter(". ")
                
                // Extract the base role name, e.g., "Speaker" from "Speaker (1/2)"
                val baseRole = roleText.substringBefore(" (")

                val priority = selectedRoles[baseRole]

                chip.text = if (priority != null) "$priority. $roleText" else roleText
                updateChipAppearance(chip, priority != null)
            }
        }

        private fun updateSelectedRolesText() {
            binding.tvSelectedRoles.visibility =
                if (selectedRoles.isNotEmpty()) View.VISIBLE else View.GONE
            if (selectedRoles.isNotEmpty()) {
                val sortedRoles = selectedRoles.entries
                    .sortedBy { it.value }
                    .joinToString("\n") { "${it.value}. ${it.key}" }
                binding.tvSelectedRoles.text = "Your role preferences:\n$sortedRoles"
            }
        }

        private fun displayRoleStatus(meeting: Meeting) {
            binding.cgRoles.removeAllViews()

            val roleCounts = meeting.roleCounts ?: return
            val assignedCounts = meeting.assignedCounts ?: emptyMap()

            roleCounts.forEach { (baseRole, totalCount) ->
                val assigned = assignedCounts.filterKeys { it.startsWith(baseRole) }.values.sum()
                val isFullyAssigned = assigned >= totalCount

                val chip = Chip(itemView.context).apply {
                    text = "$baseRole ($assigned/$totalCount)"
                    isCheckable = false
                    isClickable = false
                    isEnabled = !isFullyAssigned
                }
                binding.cgRoles.addView(chip)
            }
        }

    }
}