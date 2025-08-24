package com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.ItemRecentRoleBinding
import com.bntsoft.toastmasters.domain.model.role.Role

class RecentRolesAdapter(
    private val onRoleSelected: (Role) -> Unit = {}
) : ListAdapter<Role, RecentRolesAdapter.RecentRoleViewHolder>(RecentRoleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentRoleViewHolder {
        val binding = ItemRecentRoleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentRoleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentRoleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecentRoleViewHolder(
        private val binding: ItemRecentRoleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(role: Role) {
            binding.chipRole.text = role.name
            
            // Set different colors based on role type
            val roleColorRes = when (role.name.lowercase()) {
                "toastmaster" -> R.color.role_toastmaster
                "speaker" -> R.color.role_speaker
                "evaluator" -> R.color.role_evaluator
                "grammarian" -> R.color.role_grammarian
                "timer" -> R.color.role_timer
                "ah counter" -> R.color.role_ah_counter
                else -> R.color.primary
            }
            
            binding.chipRole.apply {
                setChipBackgroundColorResource(roleColorRes)
                setTextColor(binding.root.context.getColor(android.R.color.white))
                setOnClickListener { onRoleSelected(role) }
            }
        }
    }
}

class RecentRoleDiffCallback : DiffUtil.ItemCallback<Role>() {
    override fun areItemsTheSame(oldItem: Role, newItem: Role): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Role, newItem: Role): Boolean {
        return oldItem == newItem
    }
}
