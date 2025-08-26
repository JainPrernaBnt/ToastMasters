package com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
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
    private val availableMembers: List<Pair<String, String>>,
    private val onSaveRole: (userId: String, role: String) -> Unit,
    private val onToggleEditMode: (String, Boolean) -> Unit
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
        holder.bind(
            item,
            assignableRoles,
            onRoleSelected,
            onBackupMemberSelected,
            availableMembers,
            onSaveRole,
            onToggleEditMode
        )
    }


    fun updateAssignableRoles(newRoles: List<String>) {
        assignableRoles = newRoles
        notifyDataSetChanged()
    }

    class ViewHolder(
        private val binding: ItemMemberRoleAssignmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private fun createChip(
            text: String,
            isCloseable: Boolean,
            onClose: (() -> Unit)? = null
        ): Chip {
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
            availableMembers: List<Pair<String, String>>,
            onSaveRole: (String, String) -> Unit,
            onToggleEditMode: (String, Boolean) -> Unit
        ) {
            // Filter out current member from available members
            val otherMembers = availableMembers.filter { it.first != item.userId }
            binding.apply {
                tvMemberName.text = item.memberName

                // Set up preferred roles chips
                chipGroupPreferredRoles.removeAllViews()
                item.preferredRoles.forEach { role ->
                    val chip = createChip(role, false) {}
                    chipGroupPreferredRoles.addView(chip)
                }

                // Set up selected roles chips
                chipGroupSelectedRoles.removeAllViews()
                item.selectedRoles.forEach { role ->
                    val chip = createChip(role, item.isEditable) {
                        onRoleSelected(item.userId, role)
                    }
                    chipGroupSelectedRoles.addView(chip)
                }

                // Set up recent roles
                if (item.recentRoles.isNotEmpty()) {
                    rvRecentRoles.visibility = View.VISIBLE
                    tvNoRecentRoles.visibility = View.GONE
                    rvRecentRoles.adapter = RecentRolesAdapter(item.recentRoles)
                } else {
                    rvRecentRoles.visibility = View.GONE
                    tvNoRecentRoles.visibility = View.VISIBLE
                }

                // Set up AutoCompleteTextView for roles
                val roleAdapter = ArrayAdapter(
                    root.context,
                    android.R.layout.simple_dropdown_item_1line,
                    roles.filter { !item.selectedRoles.contains(it) } // Only show roles that are not already selected
                )
                actvRole.setAdapter(roleAdapter)
                
                // Handle role selection from dropdown
                actvRole.setOnItemClickListener { _, _, position, _ ->
                    val selectedRole = roleAdapter.getItem(position) ?: return@setOnItemClickListener
                    if (!item.selectedRoles.contains(selectedRole)) {
                        onRoleSelected(item.userId, selectedRole)
                        actvRole.text.clear()
                    }
                }

                // Set up AutoCompleteTextView for backup members
                val memberAdapter = ArrayAdapter(
                    root.context,
                    android.R.layout.simple_dropdown_item_1line,
                    otherMembers.map { it.second } // Show member names in dropdown
                )
                actvBackupMember.setAdapter(memberAdapter)

                // Set current backup member if exists
                if (item.backupMemberId.isNotBlank()) {
                    cbAssignBackup.isChecked = true
                    tilBackupMember.visibility = View.VISIBLE
                    actvBackupMember.setText(item.backupMemberName, false)
                } else {
                    cbAssignBackup.isChecked = false
                    tilBackupMember.visibility = View.GONE
                }

                // Handle backup member selection
                actvBackupMember.setOnItemClickListener { _, _, position, _ ->
                    val selectedMember = otherMembers[position]
                    onBackupMemberSelected(item.userId, selectedMember.first)
                }

                // Toggle backup member visibility
                tilBackupMember.visibility = if (cbAssignBackup.isChecked) View.VISIBLE else View.GONE

                cbAssignBackup.setOnCheckedChangeListener { _, isChecked ->
                    tilBackupMember.visibility = if (isChecked) View.VISIBLE else View.GONE
                    if (!isChecked) {
                        onBackupMemberSelected(item.userId, "")
                        actvBackupMember.setText("", false)
                    }
                }

                // Set up edit mode UI
                btnEdit.visibility = if (item.selectedRoles.isNotEmpty() && !item.isEditable) View.VISIBLE else View.GONE
                btnAssignRole.visibility = if (item.isEditable) View.VISIBLE else View.GONE
                
                // Disable/enable input fields based on edit mode
                actvRole.isEnabled = item.isEditable
                actvBackupMember.isEnabled = item.isEditable
                cbAssignBackup.isEnabled = item.isEditable
                
                // Handle Edit button click
                btnEdit.setOnClickListener {
                    onToggleEditMode(item.userId, true)
                }

                // Handle Assign Role button click
                btnAssignRole.setOnClickListener {
                    val selectedRoles = item.selectedRoles.toList()
                    if (selectedRoles.isNotEmpty()) {
                        try {
                            // Save each selected role
                            selectedRoles.forEach { role ->
                                onSaveRole(item.userId, role)
                            }
                            
                            // Disable edit mode after saving
                            onToggleEditMode(item.userId, false)

                            // Show success message
                            val roleText = selectedRoles.joinToString(", ")
                            Toast.makeText(
                                root.context,
                                "$roleText assigned to ${item.memberName}",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                        } catch (e: Exception) {
                            Toast.makeText(
                                root.context,
                                "Error assigning roles: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Show error if no role is selected
                        Toast.makeText(
                            root.context,
                            "Please select at least one role to assign",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // Make the AutoCompleteTextView clickable to show dropdown
                actvRole.setOnClickListener {
                    if (item.isEditable) {
                        actvRole.showDropDown()
                    }
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