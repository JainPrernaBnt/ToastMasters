package com.bntsoft.toastmasters.presentation.ui.members.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.MemberRole
import com.bntsoft.toastmasters.databinding.ItemMeetingsRolesBinding

class MeetingsRoleAdapter : ListAdapter<MemberRole, MeetingsRoleAdapter.MeetingsRoleViewHolder>(MemberRoleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingsRoleViewHolder {
        val binding = ItemMeetingsRolesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MeetingsRoleViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MeetingsRoleAdapter.MeetingsRoleViewHolder,
        position: Int
    ) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class MeetingsRoleViewHolder(
        private val binding: ItemMeetingsRolesBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(memberRole: MemberRole) {
            binding.apply {
                tvMemberName.text = memberRole.memberName
                tvMeetingRole.text = if (memberRole.roles.isNotEmpty()) {
                    "Assigned Roles: ${memberRole.roles.joinToString(", ")}"
                } else {
                    itemView.context.getString(R.string.error_loading_roles)
                }

                if (!memberRole.evaluator.isNullOrBlank()) {
                    tvEvaluator.visibility = View.VISIBLE
                    tvEvaluator.text =
                        "Evaluator: ${memberRole.evaluator} (${memberRole.evaluatorRole ?: ""})"
                } else {
                    tvEvaluator.visibility = View.GONE
                }
            }
        }
    }
}

class MemberRoleDiffCallback : DiffUtil.ItemCallback<MemberRole>() {
    override fun areItemsTheSame(oldItem: MemberRole, newItem: MemberRole): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MemberRole, newItem: MemberRole): Boolean {
        return oldItem == newItem
    }
}
