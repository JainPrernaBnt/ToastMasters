package com.bntsoft.toastmasters.presentation.member.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.databinding.ItemMemberApprovalBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.*

class MemberApprovalAdapter(
    private val onApproveClick: (User) -> Unit,
    private val onRejectClick: (User) -> Unit,
    private val onAssignMentorClick: (User) -> Unit,
    private val onMentorClick: (User) -> Unit
) : ListAdapter<User, MemberApprovalAdapter.MemberApprovalViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberApprovalViewHolder {
        val binding = ItemMemberApprovalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberApprovalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberApprovalViewHolder, position: Int) {
        val member = getItem(position)
        holder.bind(member)
    }

    inner class MemberApprovalViewHolder(
        private val binding: ItemMemberApprovalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        private val timeAgoFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

        init {
            binding.approveButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onApproveClick(getItem(position))
                }
            }

            binding.rejectButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRejectClick(getItem(position))
                }
            }

            binding.assignMentorButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAssignMentorClick(getItem(position))
                }
            }

            binding.mentorChip.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMentorClick(getItem(position))
                }
            }
        }

        fun bind(member: User) {
            // Set member name
            binding.nameTextView.text = member.name

            // Set email
            binding.emailTextView.text = member.email

            // Set phone number (if available)
            binding.phoneTextView.text = member.phoneNumber.ifEmpty {
                itemView.context.getString(R.string.not_provided)
            }

            // Set joined date
            val joinedDate = member.joinedDate?.let { date ->
                itemView.context.getString(
                    R.string.joined_date,
                    dateFormat.format(date)
                )
            } ?: itemView.context.getString(R.string.not_available)
            binding.joinedDateTextView.text = joinedDate

            // Load member avatar (placeholder for now)
            Glide.with(itemView)
                .load(R.drawable.ic_person)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .placeholder(R.drawable.ic_person)
                .into(binding.avatarImageView)

            // Show/hide mentor assignment UI
            val hasMentor = member.mentorIds.isNotEmpty()
            binding.assignMentorButton.visibility = if (hasMentor) View.GONE else View.VISIBLE
            binding.mentorChip.visibility = if (hasMentor) View.VISIBLE else View.GONE

            // If member has a mentor, show mentor info
            if (hasMentor) {
                // TODO: Load mentor name from repository
                // For now, just show a placeholder
                binding.mentorChip.text = itemView.context.getString(R.string.mentor_assigned_placeholder)
            }

            // Show/hide buttons based on member approval status
            if (member.isApproved) {
                binding.approveButton.visibility = View.GONE
                binding.rejectButton.visibility = View.GONE
            } else {
                binding.approveButton.visibility = View.VISIBLE
                binding.rejectButton.visibility = View.VISIBLE
            }
        }
    }

    private class MemberDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
