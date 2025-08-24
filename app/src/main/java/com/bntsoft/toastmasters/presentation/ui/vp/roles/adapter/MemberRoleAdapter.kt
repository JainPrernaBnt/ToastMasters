package com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.ItemMemberRoleAssignmentBinding
import com.bntsoft.toastmasters.domain.model.role.Role
import com.bntsoft.toastmasters.presentation.ui.vp.roles.MemberRoleAssignmentViewModel.MemberRoleItem

class MemberRoleAdapter(
    private val onRoleSelected: (MemberRoleItem, Role?) -> Unit
) : ListAdapter<MemberRoleItem, MemberRoleAdapter.MemberRoleViewHolder>(MemberRoleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberRoleViewHolder {
        val binding = ItemMemberRoleAssignmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberRoleViewHolder(binding, onRoleSelected)
    }

    override fun onBindViewHolder(holder: MemberRoleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MemberRoleViewHolder(
        private val binding: ItemMemberRoleAssignmentBinding,
        private val onRoleSelected: (MemberRoleItem, Role?) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var currentItem: MemberRoleItem
        private val roleAdapter by lazy {
            ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_dropdown_item,
                mutableListOf<String>()
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        fun bind(item: MemberRoleItem) {
            currentItem = item
            
            with(binding) {
                // Set member name
                tvMemberName.text = item.userName
                
                // Setup role dropdown
                setupRoleDropdown(item)
                
                // Setup preferred roles
                setupPreferredRoles(item)
                
                // Setup recent roles
                setupRecentRoles(item)
            }
        }
        
        private fun setupRoleDropdown(item: MemberRoleItem) {
            // Add empty option for no role
            val roleNames = listOf("Select Role") + item.availableRoles.map { it.name }
            roleAdapter.clear()
            roleAdapter.addAll(roleNames)
            
            binding.tilRole.editText?.let { editText ->
                if (editText is android.widget.AutoCompleteTextView) {
                    editText.setAdapter(roleAdapter)
                    
                    // Set current role if exists
                    item.currentRole?.let { role ->
                        val position = roleAdapter.getPosition(role.name)
                        if (position >= 0) {
                            editText.setText(role.name, false)
                        }
                    } ?: run {
                        editText.setText("", false)
                    }
                    
                    editText.setOnItemClickListener { _, _, position, _ ->
                        val selectedRole = if (position > 0) {
                            item.availableRoles[position - 1]
                        } else {
                            null // No role selected
                        }
                        onRoleSelected(currentItem, selectedRole)
                    }
                }
            }
        }
        
        private fun setupPreferredRoles(item: MemberRoleItem) {
            binding.chipGroupPreferredRoles.removeAllViews()
            if (item.preferredRoles.isNotEmpty()) {
                binding.tvPreferredRolesLabel.visibility = View.VISIBLE
                item.preferredRoles.forEach { role ->
                    val chip = com.google.android.material.chip.Chip(itemView.context).apply {
                        text = role.name
                        isCheckable = false
                        isClickable = true
                        setOnClickListener {
                            onRoleSelected(currentItem, role)
                        }
                        setChipBackgroundColorResource(R.color.chip_background_selector)
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Caption)
                    }
                    binding.chipGroupPreferredRoles.addView(chip)
                }
            } else {
                binding.tvPreferredRolesLabel.visibility = View.GONE
            }
        }
        
        private fun setupRecentRoles(item: MemberRoleItem) {
            if (item.pastRoles.isNotEmpty()) {
                binding.tvRecentRolesLabel.visibility = View.VISIBLE
                binding.rvRecentRoles.visibility = View.VISIBLE
                val recentRolesAdapter = RecentRolesAdapter { role ->
                    onRoleSelected(currentItem, role)
                }
                binding.rvRecentRoles.adapter = recentRolesAdapter
                recentRolesAdapter.submitList(item.pastRoles)
            } else {
                binding.tvRecentRolesLabel.visibility = View.GONE
                binding.rvRecentRoles.visibility = View.GONE
            }
        }
    }
}

class MemberRoleDiffCallback : DiffUtil.ItemCallback<MemberRoleItem>() {
    override fun areItemsTheSame(oldItem: MemberRoleItem, newItem: MemberRoleItem): Boolean {
        return oldItem.userId == newItem.userId
    }

    override fun areContentsTheSame(oldItem: MemberRoleItem, newItem: MemberRoleItem): Boolean {
        return oldItem.userId == newItem.userId &&
               oldItem.currentRole?.id == newItem.currentRole?.id
    }
}
