package com.bntsoft.toastmasters.presentation.ui.members.upcoming

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.ItemUpcomingMeetingBinding
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.format.DateTimeFormatter

class UpcomingMeetingsAdapter(
    private val currentUserId: String,
    private val onAvailabilityChanged: (meetingId: String, availability: MemberResponse.AvailabilityStatus) -> Unit,
    private val onRolesUpdated: (meetingId: String, selectedRoles: List<String>) -> Unit,
    private val memberResponseRepository: MemberResponseRepository
) : ListAdapter<Meeting, UpcomingMeetingsAdapter.UpcomingMeetingViewHolder>(
    UpcomingMeetingDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpcomingMeetingViewHolder {
        val binding = ItemUpcomingMeetingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UpcomingMeetingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpcomingMeetingViewHolder, position: Int) {
        val meeting = getItem(position)
        holder.bind(meeting)
    }

    inner class UpcomingMeetingViewHolder(
        private val binding: ItemUpcomingMeetingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentMeetingId: String = ""

        fun bind(meeting: Meeting) {
            currentMeetingId = meeting.id

            // If currentUserId is empty, don't try to load responses
            if (currentUserId.isEmpty()) {
                setupMeetingInfo(meeting, null)
                return
            }

            // Load member's current response for this meeting (using runBlocking for simplicity)
            try {
                val currentResponse = kotlinx.coroutines.runBlocking {
                    memberResponseRepository.getResponsesForMeeting(currentMeetingId)
                        .firstOrNull()
                        ?.find { it.memberId == currentUserId }
                }
                setupMeetingInfo(meeting, currentResponse)
            } catch (e: Exception) {
                android.util.Log.e("UpcomingMeetings", "Error loading response: ${e.message}")
                setupMeetingInfo(meeting, null)
            }

        }

        private fun setupMeetingInfo(meeting: Meeting, currentResponse: MemberResponse?) {
            binding.apply {
                // Always set meeting details
                tvMeetingTitle.text = meeting.theme
                tvMeetingDate.text =
                    meeting.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                tvMeetingTime.text =
                    "${meeting.dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${
                        meeting.endDateTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: ""
                    }"
                tvMeetingVenue.text = meeting.location

                // Handle response UI only if we have a valid user ID
                if (currentUserId.isNotEmpty()) {
                    // Set up availability chips
                    chipGroupAvailability.setOnCheckedChangeListener(null) // Clear previous listeners

                    // Set initial state based on current response
                    when (currentResponse?.availability) {
                        MemberResponse.AvailabilityStatus.AVAILABLE -> chipAvailable.isChecked =
                            true

                        MemberResponse.AvailabilityStatus.NOT_AVAILABLE -> chipNotAvailable.isChecked =
                            true

                        else -> chipNotConfirmed.isChecked = true // Default to not confirmed
                    }

                    // Show/hide preferred roles based on current availability
                    layoutPreferredRoles.isVisible =
                        currentResponse?.availability == MemberResponse.AvailabilityStatus.AVAILABLE

                    // Set up chip listeners
                    val chipMap = mapOf(
                        chipAvailable.id to MemberResponse.AvailabilityStatus.AVAILABLE,
                        chipNotAvailable.id to MemberResponse.AvailabilityStatus.NOT_AVAILABLE,
                        chipNotConfirmed.id to MemberResponse.AvailabilityStatus.NOT_CONFIRMED
                    )

                    chipGroupAvailability.setOnCheckedChangeListener { group, checkedId ->
                        val status = chipMap[checkedId] ?: return@setOnCheckedChangeListener
                        onAvailabilityChanged(meeting.id, status)

                        // Show/hide preferred roles section based on availability
                        layoutPreferredRoles.isVisible =
                            status == MemberResponse.AvailabilityStatus.AVAILABLE
                    }

                    // Enable/disable interaction based on user authentication
                    chipGroupAvailability.isEnabled = true
                } else {
                    // Disable interaction if user is not authenticated
                    chipGroupAvailability.isEnabled = false
                    layoutPreferredRoles.isVisible = false
                }

                // Set up preferred roles if available
                if (meeting.availableRoles.isNotEmpty()) {
                    setupPreferredRoles(
                        meeting.id,
                        meeting.availableRoles,
                        currentResponse?.preferredRoles ?: emptyList()
                    )
                } else {
                    binding.layoutPreferredRoles.visibility = View.GONE
                }
            }
        }

        private fun setupPreferredRoles(
            meetingId: String,
            availableRoles: List<String>,
            selectedRoles: List<String>
        ) {
            // Clear any existing chips
            binding.chipGroupPreferredRoles.removeAllViews()
            
            if (availableRoles.isEmpty()) {
                binding.layoutPreferredRoles.visibility = View.GONE
                return
            }
            
            // Show the roles section
            binding.layoutPreferredRoles.visibility = View.VISIBLE
            
            // Get resources
            val resources = itemView.context.resources
            
            // Create a chip for each available role
            availableRoles.forEach { role ->
                val chip = com.google.android.material.chip.Chip(binding.chipGroupPreferredRoles.context).apply {
                    text = role
                    isCheckable = true
                    isChecked = selectedRoles.contains(role)

                    // Set up chip styling
                    setChipBackgroundColorResource(R.color.selector_chip_background)
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body2)

                    // Set chip dimensions and padding
                    setEnsureMinTouchTargetSize(false)
                    chipMinHeight = resources.getDimensionPixelSize(R.dimen.chip_min_height).toFloat()
                    setPadding(
                        resources.getDimensionPixelSize(R.dimen.chip_horizontal_padding),
                        resources.getDimensionPixelSize(R.dimen.chip_vertical_padding),
                        resources.getDimensionPixelSize(R.dimen.chip_horizontal_padding),
                        resources.getDimensionPixelSize(R.dimen.chip_vertical_padding)
                    )

                    // Set up click listener for role selection
                    setOnCheckedChangeListener { buttonView, isChecked ->
                        val currentSelectedRoles = mutableListOf<String>()
                        for (i in 0 until binding.chipGroupPreferredRoles.childCount) {
                            val child = binding.chipGroupPreferredRoles.getChildAt(i) as? com.google.android.material.chip.Chip
                            child?.let { chip ->
                                if (chip.isChecked) {
                                    currentSelectedRoles.add(chip.text.toString())
                                }
                            }
                        }
                        onRolesUpdated(meetingId, currentSelectedRoles)
                    }
                }

                binding.chipGroupPreferredRoles.addView(chip)
            }
        }
    }

    class UpcomingMeetingDiffCallback : DiffUtil.ItemCallback<Meeting>() {
        override fun areItemsTheSame(oldItem: Meeting, newItem: Meeting): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Meeting, newItem: Meeting): Boolean {
            return oldItem == newItem
        }
    }
}
