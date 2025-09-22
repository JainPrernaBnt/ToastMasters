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

    var onAvailabilitySubmitted: ((meetingId: String, status: AvailabilityStatus, preferredRoles: List<String>, isBackout: Boolean, backoutReason: String?) -> Unit)? = null
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
                
                // Setup chips
                chipAvailable.chipBackgroundColor = itemView.context.getColorStateList(R.color.available_chip_background)
                chipNotAvailable.chipBackgroundColor = itemView.context.getColorStateList(R.color.unavailable_chip_background)
                chipNotConfirmed.chipBackgroundColor = itemView.context.getColorStateList(R.color.not_confirmed_chip_background)

                // --- Decide Form vs Read-only ---
                val showForm = canEdit && (userAvailability == null || meeting.isEditMode)

                if (showForm) {
                    // Show form (chips + submit)
                    cgAvailability.visibility = View.VISIBLE
                    btnSubmit.visibility = View.VISIBLE
                    tvRoleInstructions.visibility = View.VISIBLE
                    tvRoleInstructions.text =
                        "Select your preferred roles in order of priority.\nThe first role you select will be your highest priority."

                    // Set initial selection
                    cgAvailability.clearCheck()
                    when (userAvailability?.status) {
                        AvailabilityStatus.AVAILABLE -> chipAvailable.isChecked = true
                        AvailabilityStatus.NOT_CONFIRMED -> chipNotConfirmed.isChecked = true
                        AvailabilityStatus.NOT_AVAILABLE, null -> chipNotAvailable.isChecked = true
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
                    cgAvailability.visibility = View.GONE
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
                            // Toggle backout reason visibility
                            val isReasonVisible = binding.tilBackoutReason.visibility == View.VISIBLE
                            
                            if (!isReasonVisible) {
                                // Show backout reason input
                                binding.tilBackoutReason.visibility = View.VISIBLE
                                binding.btnSubmitBackout.visibility = View.VISIBLE
                                binding.btnBackout.text = "Cancel Backout"
                            } else {
                                // Hide backout reason input
                                binding.tilBackoutReason.visibility = View.GONE
                                binding.btnSubmitBackout.visibility = View.GONE
                                binding.btnBackout.text = "Backout"
                                binding.etBackoutReason.text?.clear()
                            }
                        }
                        
                        // Handle backout submission
                        binding.btnSubmitBackout.setOnClickListener {
                            val reason = binding.etBackoutReason.text?.toString()?.trim()
                            
                            if (reason.isNullOrEmpty()) {
                                // If no reason provided, show confirmation dialog
                                MaterialAlertDialogBuilder(itemView.context)
                                    .setTitle("Backout without reason?")
                                    .setMessage("Are you sure you want to back out without providing a reason?")
                                    .setPositiveButton("Yes, Backout") { _, _ ->
                                        submitBackout(meeting.id, reason)
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            } else {
                                submitBackout(meeting.id, reason)
                            }
                        }
                    } else {
                        btnEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
                        btnEdit.setOnClickListener { onEditClicked?.invoke(meeting) }
                    }
                }

                // --- ChipGroup Listeners ---
                chipAvailable.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        currentStatus = AvailabilityStatus.AVAILABLE
                        updatePreferredRolesVisibility()
                    }
                }
                
                chipNotAvailable.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        currentStatus = AvailabilityStatus.NOT_AVAILABLE
                        selectedRoles.clear()
                        cgRoles.clearCheck()
                        updatePreferredRolesVisibility()
                    }
                }
                
                chipNotConfirmed.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        currentStatus = AvailabilityStatus.NOT_CONFIRMED
                        selectedRoles.clear()
                        cgRoles.clearCheck()
                        updatePreferredRolesVisibility()
                    }
                }

                // --- Submit Button ---
                btnSubmit.setOnClickListener {
                    onAvailabilitySubmitted?.invoke(meeting.id, currentStatus, selectedRoles.keys.toList(), false, null)
                }
            }
        }

        private fun setupInitialState() {
            binding.apply {
                // Reset form state
                cgRoles.visibility = View.GONE
                tvNoRoles.visibility = View.GONE
                selectedRoles.clear()
                
                // Reset backout UI
                tilBackoutReason.visibility = View.GONE
                btnSubmitBackout.visibility = View.GONE
                btnBackout.text = "Backout"
                etBackoutReason.text?.clear()

                // Set default selection to Not Available if nothing is selected
                if (!chipAvailable.isChecked && !chipNotAvailable.isChecked && !chipNotConfirmed.isChecked) {
                    chipNotAvailable.isChecked = true
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
            // First, update the priority numbers based on the current selection order
            val sortedEntries = selectedRoles.entries.sortedBy { it.value }
            sortedEntries.forEachIndexed { index, entry ->
                selectedRoles[entry.key] = index + 1
            }
            nextPriority = selectedRoles.size + 1
            
            // Then update the chip text and appearance
            for (i in 0 until binding.cgRoles.childCount) {
                val chip = binding.cgRoles.getChildAt(i) as Chip
                val currentText = chip.text.toString()

                // Strip any existing priority like "1. " to get the clean role text
                val roleText = currentText.substringAfter(". ", currentText)
                
                // Extract the base role name, e.g., "Speaker" from "Speaker (1/2)"
                val baseRole = roleText.substringBefore(" (")

                val priority = selectedRoles[baseRole]
                
                // Update the chip text with the correct priority
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

        private fun submitBackout(meetingId: String, reason: String?) {
            onAvailabilitySubmitted?.invoke(
                meetingId,
                AvailabilityStatus.NOT_AVAILABLE,
                emptyList(),
                true, // backout mode
                reason
            )

            // Reset backout UI
            binding.apply {
                tilBackoutReason.visibility = View.GONE
                btnSubmitBackout.visibility = View.GONE
                btnBackout.text = "Backout"
                etBackoutReason.text?.clear()
            }
        }

    }
}