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
import com.google.android.material.chip.Chip
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class UpcomingMeetingsAdapter :
    ListAdapter<Meeting, UpcomingMeetingsAdapter.MeetingViewHolder>(MeetingDiffCallback()) {

    var onAvailabilitySubmitted: ((String, Boolean, List<String>) -> Unit)? = null

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

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

        private var isAvailable = false
        private val selectedRoles = mutableSetOf<String>()

        fun bind(meeting: Meeting, availability: MeetingAvailability? = null) {
            binding.apply {
                // Set meeting title
                tvMeetingTitle.text = meeting.theme.ifEmpty {
                    itemView.context.getString(R.string.meeting_title)
                }

                val date = "${meeting.dateTime.format(dateFormatter)} "
                tvMeetingDate.text = date

                val time = "${meeting.dateTime.format(timeFormatter)} - ${meeting.endDateTime?.format(timeFormatter) ?: ""}"
                tvMeetingTime.text = time

                // Set location
                tvMeetingLocation.text = meeting.location.ifEmpty {
                    itemView.context.getString(R.string.venue_not_specified)
                }
                if (availability != null) {
                    // View mode - show submitted availability
                    rgAvailability.visibility = View.GONE
                    btnSubmit.visibility = View.GONE
                    cgRoles.visibility = View.GONE

                    // Show availability status
                    val availabilityText = if (availability.isAvailable) {
                        "Available" // You can move this to strings.xml
                    } else {
                        "Not Available"
                    }

                    binding.tvAvailabilityStatus.visibility = View.VISIBLE
                    binding.tvAvailabilityStatus.text = "Your Availability: $availabilityText"

                    // Show selected roles if available
                    if (availability.isAvailable && availability.preferredRoles.isNotEmpty()) {
                        binding.tvSelectedRoles.visibility = View.VISIBLE
                        binding.tvSelectedRoles.text = "Preferred Roles: ${
                            availability.preferredRoles.joinToString(", ")
                        }"
                    } else {
                        binding.tvSelectedRoles.visibility = View.GONE
                    }
                } else {
                    // Edit mode
                    setupInitialState()

                    // Setup radio group listener
                    rgAvailability.setOnCheckedChangeListener { _, checkedId ->
                        isAvailable = checkedId == R.id.rbAvailable
                        updatePreferredRolesVisibility()
                    }

                    // Show availability options
                    rgAvailability.visibility = View.VISIBLE

                    // Setup preferred roles (initially hidden)
                    setupPreferredRoles(meeting.preferredRoles)

                    // Set initial visibility
                    updatePreferredRolesVisibility()

                    // Setup submit button
                    btnSubmit.visibility = View.VISIBLE
                    btnSubmit.setOnClickListener {
                        onAvailabilitySubmitted?.invoke(
                            meeting.id,
                            isAvailable,
                            selectedRoles.toList()
                        )
                    }
                }
                // Initialize UI state


            }
        }

        private fun setupInitialState() {
            binding.apply {
                // Initially hide preferred roles section
                cgRoles.visibility = View.GONE
                tvNoRoles.visibility = View.GONE

                // Set default selection to not available
                rgAvailability.clearCheck()
            }
        }

        private fun updatePreferredRolesVisibility() {
            binding.apply {
                if (isAvailable) {
                    // Show preferred roles when available is selected
                    if (cgRoles.childCount > 0) {
                        cgRoles.visibility = View.VISIBLE
                        tvNoRoles.visibility = View.GONE
                    } else {
                        tvNoRoles.visibility = View.VISIBLE
                        cgRoles.visibility = View.GONE
                    }
                } else {
                    // Hide preferred roles for not available/not selected
                    cgRoles.visibility = View.GONE
                    tvNoRoles.visibility = View.GONE
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
                selectedRoles.clear()

                if (roles.isEmpty()) {
                    updatePreferredRolesVisibility()
                    return@apply
                }

                roles.forEach { role ->
                    val chip = Chip(itemView.context).apply {
                        text = role
                        isCheckable = true
                        isClickable = true
                        isCheckable = false // We'll handle the selection manually

                        // Initial state
                        updateChipAppearance(this, false)

                        layoutParams = ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginEnd = resources.getDimensionPixelSize(R.dimen.chip_spacing)
                        }

                        setOnClickListener {
                            if (selectedRoles.contains(role)) {
                                selectedRoles.remove(role)
                                updateChipAppearance(this, false)
                            } else {
                                selectedRoles.add(role)
                                updateChipAppearance(this, true)
                            }
                        }
                    }

                    cgRoles.addView(chip)
                }

                updatePreferredRolesVisibility()
            }
        }
    }


    class MeetingDiffCallback : DiffUtil.ItemCallback<Meeting>() {
        override fun areItemsTheSame(oldItem: Meeting, newItem: Meeting): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Meeting, newItem: Meeting): Boolean {
            return oldItem == newItem
        }
    }
}
