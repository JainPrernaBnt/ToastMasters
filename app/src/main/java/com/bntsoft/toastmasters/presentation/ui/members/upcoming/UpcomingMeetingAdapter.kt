package com.bntsoft.toastmasters.presentation.ui.members.upcoming

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemUpcomingMeetingBinding
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import java.time.format.DateTimeFormatter

class UpcomingMeetingAdapter :
    ListAdapter<MeetingWithCounts, UpcomingMeetingAdapter.UpcomingMeetingViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpcomingMeetingViewHolder {
        val binding =
            ItemUpcomingMeetingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UpcomingMeetingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpcomingMeetingViewHolder, position: Int) {
        val currentMeeting = getItem(position)
        holder.bind(currentMeeting)
    }

    inner class UpcomingMeetingViewHolder(private val binding: ItemUpcomingMeetingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(meetingWithCounts: MeetingWithCounts) {
            val meeting = meetingWithCounts.meeting
            binding.apply {
                tvMeetingTitle.text = "Weekly ToastMasters Meeting"
                tvMeetingDate.text = meeting.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                tvMeetingTime.text = 
                    "${meeting.dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${meeting.endDateTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: ""}"
                tvMeetingVenue.text = meeting.location
                tvMeetingTheme.text = "Theme: ${meeting.theme}"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MeetingWithCounts>() {
        override fun areItemsTheSame(oldItem: MeetingWithCounts, newItem: MeetingWithCounts) =
            oldItem.meeting.id == newItem.meeting.id

        override fun areContentsTheSame(oldItem: MeetingWithCounts, newItem: MeetingWithCounts) =
            oldItem == newItem
    }
}
