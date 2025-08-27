package com.bntsoft.toastmasters.presentation.ui.members.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemMemberAssignedRoleBinding
import com.bntsoft.toastmasters.domain.model.MeetingWithRole
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import android.util.Log

class MeetingWithRoleAdapter :
    ListAdapter<MeetingWithRole, MeetingWithRoleAdapter.MeetingWithRoleViewHolder>(
        MeetingWithRoleDiffCallback()
    ) {
    private val TAG = "MeetingRoleAdapter"

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingWithRoleViewHolder {
        Log.d(TAG, "Creating new view holder")
        return try {
            val binding = ItemMemberAssignedRoleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            MeetingWithRoleViewHolder(binding)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating view holder", e)
            throw e
        }
    }

    override fun onBindViewHolder(holder: MeetingWithRoleViewHolder, position: Int) {
        Log.d(TAG, "Binding view at position $position")
        try {
            val item = getItem(position)
            Log.d(TAG, "Binding item: ${item.meeting.theme} (ID: ${item.meeting.id})")
            holder.bind(item)
        } catch (e: Exception) {
            Log.e(TAG, "Error binding view at position $position", e)
        }
    }

    inner class MeetingWithRoleViewHolder(
        private val binding: ItemMemberAssignedRoleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MeetingWithRole) {
            with(binding) {
                // Set meeting theme/title
                tvMeetingTheme.text = item.meeting.theme.ifEmpty { "No Theme" }

                // Set venue/location
                tvMeetingVenue.text = item.meeting.location.ifEmpty { "Location not specified" }

                // Set day of week
                val dayOfWeek = item.meeting.dateTime.dayOfWeek
                    .getDisplayName(TextStyle.FULL, Locale.getDefault())
                tvMeetingDay.text = dayOfWeek

                // Set date
                tvMeetingDate.text = item.meeting.dateTime.format(dateFormatter)

                // Set time - handle both dateTime and endDateTime
                val startTime = item.meeting.dateTime.format(timeFormatter)
                val endTime = item.meeting.endDateTime?.format(timeFormatter)
                    ?: item.meeting.dateTime.plusHours(2).format(timeFormatter)
                tvMeetingTime.text = "$startTime - $endTime"

                // Set assigned roles with labels
                val rolesText = if (item.assignedRoles.isNotEmpty()) {
                    "Assigned roles: ${item.assignedRoles.joinToString(", ")}"
                } else {
                    "Assigned role: ${item.assignedRole}" // Fallback to single role if list is empty
                }
                tvMeetingRole.text = rolesText

                // Hide the assign role button as it's not needed in the dashboard
                btnAssignRole.visibility = View.GONE
            }
        }
    }

    class MeetingWithRoleDiffCallback : DiffUtil.ItemCallback<MeetingWithRole>() {
        private val TAG = "DiffCallback"
        
        override fun areItemsTheSame(oldItem: MeetingWithRole, newItem: MeetingWithRole): Boolean {
            val isSame = oldItem.meeting.id == newItem.meeting.id
            Log.d(TAG, "areItemsTheSame: $isSame (${oldItem.meeting.id} vs ${newItem.meeting.id})")
            return isSame
        }

        override fun areContentsTheSame(
            oldItem: MeetingWithRole,
            newItem: MeetingWithRole
        ): Boolean {
            val isSame = oldItem.meeting.id == newItem.meeting.id &&
                    oldItem.assignedRoles == newItem.assignedRoles &&
                    oldItem.assignedRole == newItem.assignedRole
            if (!isSame) {
                Log.d(TAG, "Contents changed for item ${oldItem.meeting.id}")
            }
            return isSame
        }
    }
}
