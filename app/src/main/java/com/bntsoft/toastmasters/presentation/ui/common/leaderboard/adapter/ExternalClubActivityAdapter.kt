package com.bntsoft.toastmasters.presentation.ui.common.leaderboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemExternalClubActivityBinding
import com.bntsoft.toastmasters.domain.model.ExternalClubActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import java.text.SimpleDateFormat
import java.util.Locale

class ExternalClubActivityAdapter(
    private val onItemClick: (ExternalClubActivity) -> Unit = {}
) : ListAdapter<ExternalClubActivity, ExternalClubActivityAdapter.ActivityViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemExternalClubActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ActivityViewHolder(
        private val binding: ItemExternalClubActivityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(activity: ExternalClubActivity) {
            binding.apply {
                tvMemberName.text = activity.userName
                tvClubName.text = activity.clubName
                chipRole.text = activity.rolePlayed
                
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                tvDate.text = "${dateFormat.format(activity.meetingDate)}"
                
                if (!activity.clubLocation.isNullOrBlank()) {
                    tvLocation.text = "${activity.clubLocation}"
                    tvLocation.visibility = View.VISIBLE
                } else {
                    tvLocation.visibility = View.GONE
                }
                
                if (!activity.notes.isNullOrBlank()) {
                    tvNotes.text = "${activity.notes}"
                    tvNotes.visibility = View.VISIBLE
                } else {
                    tvNotes.visibility = View.GONE
                }
                
                if (!activity.userProfilePicture.isNullOrBlank()) {
                    try {
                        val imageBytes = android.util.Base64.decode(activity.userProfilePicture, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        Glide.with(itemView.context)
                            .load(bitmap)
                            .transform(CircleCrop())
                            .into(ivMemberProfile)
                    } catch (e: Exception) {
                        Glide.with(itemView.context)
                            .load(com.bntsoft.toastmasters.R.drawable.ic_person)
                            .transform(CircleCrop())
                            .into(ivMemberProfile)
                    }
                } else {
                    Glide.with(itemView.context)
                        .load(com.bntsoft.toastmasters.R.drawable.ic_person)
                        .transform(CircleCrop())
                        .into(ivMemberProfile)
                }
                
                root.setOnClickListener {
                    onItemClick(activity)
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ExternalClubActivity>() {
        override fun areItemsTheSame(oldItem: ExternalClubActivity, newItem: ExternalClubActivity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExternalClubActivity, newItem: ExternalClubActivity): Boolean {
            return oldItem == newItem
        }
    }
}
