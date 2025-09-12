package com.bntsoft.toastmasters.presentation.ui.common.leaderboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.data.model.Winner
import com.bntsoft.toastmasters.databinding.ItemWinnerBinding

class WinnerAdapter : RecyclerView.Adapter<WinnerAdapter.WinnerViewHolder>() {

    private val items = mutableListOf<Winner>()

    fun submitList(winners: List<Winner>) {
        items.clear()
        items.addAll(winners)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WinnerViewHolder {
        val binding = ItemWinnerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WinnerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WinnerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class WinnerViewHolder(
        private val binding: ItemWinnerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(winner: Winner) {
            binding.apply {
                categoryEmoji.text = "🏆"
                categoryNameTextView.text = winner.category.displayName
                winnerNameTextView.text = winner.memberName ?: winner.guestName ?: ""
            }
        }
    }
}
