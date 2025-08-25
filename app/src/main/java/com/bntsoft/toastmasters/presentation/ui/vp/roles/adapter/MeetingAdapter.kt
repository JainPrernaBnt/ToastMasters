package com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.presentation.ui.vp.roles.model.MeetingListItem
import com.bntsoft.toastmasters.databinding.ItemMeetingAssignBinding

class MeetingAdapter(
    private val onItemClick: (MeetingListItem) -> Unit
) : ListAdapter<MeetingListItem, MeetingAdapter.MeetingViewHolder>(MeetingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingViewHolder {
        val binding = ItemMeetingAssignBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MeetingViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: MeetingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MeetingViewHolder(
        private val binding: ItemMeetingAssignBinding,
        private val onItemClick: (MeetingListItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meeting: MeetingListItem) {
            with(binding) {
                // Theme
                tvMeetingTheme.text = meeting.theme
                
                // Venue with icon
                tvMeetingVenue.text = meeting.venue
                
                // Date and time in separate fields
                tvMeetingDay.text = meeting.dayOfWeek
                tvMeetingDate.text = meeting.formattedDate
                tvMeetingTime.text = meeting.formattedTime
                
                // Set click listener
                root.setOnClickListener { onItemClick(meeting) }
            }
        }
    }
}

class MeetingDiffCallback : DiffUtil.ItemCallback<MeetingListItem>() {
    override fun areItemsTheSame(oldItem: MeetingListItem, newItem: MeetingListItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MeetingListItem, newItem: MeetingListItem): Boolean {
        return oldItem == newItem
    }
}
