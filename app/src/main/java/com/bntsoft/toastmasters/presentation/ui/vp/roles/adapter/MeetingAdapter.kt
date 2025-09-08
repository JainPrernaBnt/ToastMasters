package com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemMeetingAssignBinding
import com.bntsoft.toastmasters.presentation.ui.vp.roles.model.MeetingListItem

class MeetingAdapter(
    private val onItemClick: (MeetingListItem) -> Unit,
    private val onCreateAgendaClick: (MeetingListItem) -> Unit
) : ListAdapter<MeetingListItem, MeetingAdapter.MeetingViewHolder>(MeetingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingViewHolder {
        val binding = ItemMeetingAssignBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MeetingViewHolder(binding, onItemClick, onCreateAgendaClick)
    }

    override fun onBindViewHolder(holder: MeetingViewHolder, position: Int) {
        val item = getItem(position)
        Log.d(
            "MeetingAdapter",
            "Binding item at position $position: ${item.theme} (ID: ${item.id})"
        )
        holder.bind(item)
    }

    override fun onViewRecycled(holder: MeetingViewHolder) {
        super.onViewRecycled(holder)
        Log.d("MeetingAdapter", "View recycled for position ${holder.bindingAdapterPosition}")
    }

    class MeetingViewHolder(
        private val binding: ItemMeetingAssignBinding,
        private val onItemClick: (MeetingListItem) -> Unit,
        private val onCreateAgendaClick: (MeetingListItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meeting: MeetingListItem) {
            Log.d("MeetingViewHolder", "Binding meeting: ${meeting.theme} (ID: ${meeting.id})")
            with(binding) {
                // Bind UI
                tvMeetingTheme.text = meeting.theme
                tvMeetingVenue.text = meeting.venue
                tvMeetingDay.text = meeting.dayOfWeek
                tvMeetingDate.text = meeting.formattedDate
                tvMeetingTime.text = meeting.formattedTime

                // Click listeners
                root.setOnClickListener { onItemClick(meeting) }
                btnCreateAgenda.setOnClickListener { onCreateAgendaClick(meeting) }
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
