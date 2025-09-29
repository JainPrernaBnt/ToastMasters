package com.bntsoft.toastmasters.presentation.ui.common.leaderboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.data.model.MeetingWithWinners
import com.bntsoft.toastmasters.databinding.ItemMeetingWinnersBinding

class LeaderboardMeetingAdapter : RecyclerView.Adapter<LeaderboardMeetingAdapter.MeetingViewHolder>() {

    private val items = mutableListOf<MeetingWithWinners>()

    fun submitList(meetings: List<MeetingWithWinners>) {
        items.clear()
        items.addAll(meetings)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingViewHolder {
        val binding = ItemMeetingWinnersBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MeetingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MeetingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class MeetingViewHolder(
        private val binding: ItemMeetingWinnersBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val winnerAdapter = WinnerAdapter()

        init {
            binding.winnersRecyclerView.adapter = winnerAdapter
        }

        fun bind(meeting: MeetingWithWinners) {
            binding.apply {
                dateTextView.text = meeting.date
                themeTextView.text = meeting.theme
                winnerAdapter.submitList(meeting.winners)
            }
        }
    }
}
