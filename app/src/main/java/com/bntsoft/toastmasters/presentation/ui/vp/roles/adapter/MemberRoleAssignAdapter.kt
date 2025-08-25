package com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemMemberRoleAssignmentBinding
import com.bntsoft.toastmasters.domain.model.RoleAssignmentItem
import com.google.android.material.chip.Chip

class MemberRoleAssignAdapter(
    private var assignableRoles: List<String>,
    private val onRoleSelected: (String, String) -> Unit,
    private val onBackupMemberSelected: (String, String) -> Unit,
    private val availableMembers: List<Pair<String, String>>
) : ListAdapter<RoleAssignmentItem, MemberRoleAssignAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMemberRoleAssignmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, assignableRoles, onRoleSelected, onBackupMemberSelected, availableMembers)
    }

    fun updateAssignableRoles(newRoles: List<String>) {
        assignableRoles = newRoles
        notifyDataSetChanged()
    }

    class ViewHolder(
        private val binding: ItemMemberRoleAssignmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private fun createChip(text: String, isCloseable: Boolean, onClose: (() -> Unit)? = null): Chip {
            return Chip(binding.root.context).apply {
                this.text = text
                isCheckable = false
                setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Chip)
                
                if (isCloseable && onClose != null) {
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        onClose()
                    }
                }
            }
        }

        fun bind(
            item: RoleAssignmentItem,
            roles: List<String>,
            onRoleSelected: (String, String) -> Unit,
            onBackupMemberSelected: (String, String) -> Unit,
            availableMembers: List<Pair<String, String>>
        ) {
            // Filter out current member from available members
            val otherMembers = availableMembers.filter { it.first != item.userId }
            binding.tvMemberName.text = item.memberName

            // Set up preferred roles chips
            binding.chipGroupPreferredRoles.removeAllViews()
            item.preferredRoles.forEach { role ->
                val chip = createChip(role, false) {}
                binding.chipGroupPreferredRoles.addView(chip)
            }

            // Set up selected roles chips
            binding.chipGroupSelectedRoles.removeAllViews()
            item.selectedRoles.forEach { role ->
                val chip = createChip(role, true) {
                    onRoleSelected(item.userId, role)
                }
                binding.chipGroupSelectedRoles.addView(chip)
            }

            // Set up recent roles
            if (item.recentRoles.isNotEmpty()) {
                binding.rvRecentRoles.visibility = View.VISIBLE
                binding.tvNoRecentRoles.visibility = View.GONE
                binding.rvRecentRoles.adapter = RecentRolesAdapter(item.recentRoles)
            } else {
                binding.rvRecentRoles.visibility = View.GONE
                binding.tvNoRecentRoles.visibility = View.VISIBLE
            }

            // Set up role selection
            val availableRoles = roles.filter { !item.selectedRoles.contains(it) }
            val roleAdapter = ArrayAdapter(
                binding.root.context,
                android.R.layout.simple_dropdown_item_1line,
                availableRoles
            )
            binding.actvRole.setAdapter(roleAdapter)
            binding.actvRole.setText("", false)

            binding.actvRole.setOnItemClickListener { _, _, position, _ ->
                val selectedRole = roleAdapter.getItem(position) ?: return@setOnItemClickListener
                onRoleSelected(item.userId, selectedRole.toString())
                binding.actvRole.setText("", false)
            }
            
            // Set up backup member selection
            val memberAdapter = ArrayAdapter(
                binding.root.context,
                android.R.layout.simple_dropdown_item_1line,
                otherMembers.map { it.second }
            )
            binding.actvBackupMember.setAdapter(memberAdapter)
            binding.actvBackupMember.setText(item.backupMemberName, false)
            
            binding.actvBackupMember.onItemClickListener = null
            binding.actvBackupMember.setOnItemClickListener { _, _, position, _ ->
                if (position < otherMembers.size) {
                    val selectedMember = otherMembers[position]
                    onBackupMemberSelected(item.userId, selectedMember.first)
                }
            }
            
            // Handle backup checkbox
            binding.cbAssignBackup.setOnCheckedChangeListener(null) // Clear previous listeners
            binding.cbAssignBackup.isChecked = item.backupMemberId.isNotEmpty()
            binding.tilBackupMember.visibility = if (binding.cbAssignBackup.isChecked) View.VISIBLE else View.GONE
            
            binding.cbAssignBackup.setOnCheckedChangeListener { _, isChecked ->
                binding.tilBackupMember.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (!isChecked) {
                    onBackupMemberSelected(item.userId, "")
                    binding.actvBackupMember.setText("", false)
                }
            }

            
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RoleAssignmentItem>() {
        override fun areItemsTheSame(oldItem: RoleAssignmentItem, newItem: RoleAssignmentItem) =
            oldItem.userId == newItem.userId

        override fun areContentsTheSame(oldItem: RoleAssignmentItem, newItem: RoleAssignmentItem) =
            oldItem == newItem
    }
}