package com.bntsoft.toastmasters.presentation.ui.vp.dashboard

import android.view.LayoutInflater
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
    private val onItemClick: (meetingId: String) -> Unit = {}
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
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            binding.tvMeetingDate.text = meeting.dateTime.format(formatter)
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
            val time = "${meeting.dateTime.format(timeFormatter)} - ${
                meeting.endDateTime?.format(timeFormatter)
            }"
            binding.tvMeetingTime.text = time
            binding.tvMeetingVenue.text = meeting.location

            binding.tvAvailableCount.text = meetingWithCounts.availableCount.toString()
            binding.tvNotAvailableCount.text = meetingWithCounts.notAvailableCount.toString()
            binding.tvNotConfirmedCount.text = meetingWithCounts.notConfirmedCount.toString()

            // Overflow menu for edit/delete
            binding.btnOverflow.setOnClickListener { view ->
                PopupMenu(view.context, view).apply {
                    inflate(R.menu.menu_meeting_item)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.action_edit -> {
                                onEdit(meeting.id)
                                true
                            }

                            R.id.action_delete -> {
                                onDelete(meeting.id)
                                true
                            }

                            else -> false
                        }
                    }
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