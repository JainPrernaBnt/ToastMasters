package com.bntsoft.toastmasters.presentation.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.NotificationData
import com.bntsoft.toastmasters.databinding.ItemNotificationBinding
import com.bntsoft.toastmasters.utils.DateTimeUtils.formatDateTime
import com.bumptech.glide.Glide


class NotificationAdapter(
    private val onNotificationClick: (NotificationData) -> Unit,
    private val onDeleteClick: (NotificationData) -> Unit
) : ListAdapter<NotificationData, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification)
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val notification = getItem(position)
                    onNotificationClick(notification)
                }
            }

            binding.btnDelete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val notification = getItem(position)
                    onDeleteClick(notification)
                }
            }
        }

        fun bind(notification: NotificationData) {
            with(binding) {
                // Set notification title and message
                textTitle.text = notification.title
                textMessage.text = notification.message
                
                // Format and set the time
                textTime.text = formatDateTime(notification.createdAt.time)
                
                // Set read state
                if (notification.isRead) {
                    // Read notification styling
                    root.setBackgroundResource(R.drawable.bg_notification_read)
                    textTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                } else {
                    // Unread notification styling
                    root.setBackgroundResource(R.drawable.bg_notification_unread)
                    textTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                    textTitle.paint.isFakeBoldText = true
                    
                    // Show an indicator for unread notifications
                    viewUnreadIndicator.visibility = View.VISIBLE
                }
                
                // Set notification icon based on type
                val iconRes = when (notification.type) {
                    "meeting_created" -> R.drawable.ic_notification_meeting
                    "mentor_assignment" -> R.drawable.ic_notification_mentor
                    else -> R.drawable.ic_notification_default
                }
                
                Glide.with(imageIcon)
                    .load(iconRes)
                    .into(imageIcon)
            }
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationData>() {
        override fun areItemsTheSame(oldItem: NotificationData, newItem: NotificationData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationData, newItem: NotificationData): Boolean {
            return oldItem == newItem
        }
    }
}
