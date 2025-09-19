package com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.ItemMeetingCardBinding
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import java.time.format.DateTimeFormatter

class MeetingAdapter(
    private val onEdit: (meetingId: String) -> Unit = {},
    private val onDelete: (meetingId: String) -> Unit = {},
    private val onComplete: (meetingId: String) -> Unit = {},
    private val onItemClick: (meetingId: String) -> Unit = {},
    private val showOverflowMenu: Boolean = true
) :
    ListAdapter<MeetingWithCounts, MeetingAdapter.MeetingViewHolder>(MeetingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingViewHolder {
        val binding =
            ItemMeetingCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MeetingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MeetingViewHolder, position: Int) {
        val meeting = getItem(position)
        holder.bind(meeting)
    }

    inner class MeetingViewHolder(private val binding: ItemMeetingCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val meeting = getItem(position).meeting
                    onItemClick(meeting.id)
                }
            }
        }

        fun bind(meetingWithCounts: MeetingWithCounts) {
            val meeting = meetingWithCounts.meeting
            binding.tvMeetingTitle.text = meeting.theme

            // Format date (e.g., "Aug 25, 2024")
            val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            binding.tvMeetingDate.text = meeting.dateTime.format(dateFormatter)

            // Format times in 12-hour format with AM/PM
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
            val startTime = meeting.dateTime.format(timeFormatter).replace(" ", " ").lowercase()
            val endTime =
                meeting.endDateTime?.format(timeFormatter)?.replace(" ", " ")?.lowercase() ?: ""

            // Ensure we show the time range only if end time is available
            val timeDisplay = if (endTime.isNotEmpty()) "$startTime - $endTime" else startTime
            binding.tvMeetingTime.text = timeDisplay
            binding.tvMeetingVenue.text = meeting.location

            binding.tvAvailableCount.text = meetingWithCounts.availableCount.toString()
            binding.tvNotAvailableCount.text = meetingWithCounts.notAvailableCount.toString()
            binding.tvNotConfirmedCount.text = meetingWithCounts.notConfirmedCount.toString()
            binding.tvNotResponded.text = meetingWithCounts.notResponded.toString()

            // Overflow menu for edit/delete
            binding.btnOverflow.visibility = if (showOverflowMenu) View.VISIBLE else View.GONE

            if (showOverflowMenu) {
                binding.btnOverflow.setOnClickListener { view ->
                    val popupMenu = PopupMenu(view.context, view)
                    popupMenu.menuInflater.inflate(R.menu.menu_meeting_item, popupMenu.menu)
                    popupMenu.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.action_edit -> {
                                onEdit(meeting.id)
                                true
                            }

                            R.id.action_delete -> {
                                onDelete(meeting.id)
                                true
                            }

                            R.id.action_complete -> {
                                onComplete(meeting.id)
                                true
                            }

                            else -> false
                        }
                    }
                    popupMenu.show()
                }
            }
        }
    }
}

class MeetingDiffCallback : DiffUtil.ItemCallback<MeetingWithCounts>() {
    override fun areItemsTheSame(
        oldItem: MeetingWithCounts,
        newItem: MeetingWithCounts
    ): Boolean {
        return oldItem.meeting.id == newItem.meeting.id
    }

    override fun areContentsTheSame(
        oldItem: MeetingWithCounts,
        newItem: MeetingWithCounts
    ): Boolean {
        return oldItem == newItem
    }
}