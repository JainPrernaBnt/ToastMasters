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
    private var availableMembers: List<Pair<String, String>>,
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
        android.util.Log.d("MemberRoleAssignAdapter", "[onBindViewHolder] Binding item at position $position: " +
                "member=${item.memberName}, assignedRole=${item.assignedRole}, " +
                "selectedRoles=${item.selectedRoles}, isEditable=${item.isEditable}")
        
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
    
    fun updateAvailableMembers(newMembers: List<Pair<String, String>>) {
        availableMembers = newMembers
        notifyDataSetChanged()
    }

    class ViewHolder(
        private val binding: ItemMemberRoleAssignmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var isSettingChecked = false

        private fun createChip(
            text: String,
            isCloseable: Boolean,
            onClose: (() -> Unit)? = null,
            isCheckable: Boolean = false
        ): Chip {
            return Chip(binding.root.context, null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice).apply {
                this.text = text
                this.isCheckable = isCheckable
                this.isChecked = false
                this.chipBackgroundColor = context.getColorStateList(com.google.android.material.R.color.mtrl_chip_background_color)
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
            android.util.Log.d("MemberRoleAssignAdapter", "[bind] Binding member: ${item.memberName}")
            android.util.Log.d("MemberRoleAssignAdapter", "[bind] Current state - assignedRole: ${item.assignedRole}, " +
                    "selectedRoles: ${item.selectedRoles}, isEditable: ${item.isEditable}")
            android.util.Log.d("MemberRoleAssignAdapter", "[bind] Available roles: $roles")
            // Filter out current member from available members
            val otherMembers = availableMembers.filter { it.first != item.userId }
            binding.apply {
                tvMemberName.text = item.memberName

                // Set up preferred roles chips
                binding.chipGroupPreferredRoles.removeAllViews()
                android.util.Log.d("MemberRoleAssignAdapter", "[bind] Adding ${item.preferredRoles.size} preferred roles")
                item.preferredRoles.forEach { role ->
                    val chip = createChip(role, false)
                    binding.chipGroupPreferredRoles.addView(chip)
                }

                // Set up selected roles chips
                binding.chipGroupSelectedRoles.removeAllViews()
                android.util.Log.d("MemberRoleAssignAdapter", "[bind] Adding ${item.selectedRoles.size} selected roles, isEditable=${item.isEditable}")
                
                item.selectedRoles.forEach { role ->
                    val chip = createChip(
                        text = role,
                        isCloseable = item.isEditable,
                        onClose = {
                            // On close icon click, remove the role
                            android.util.Log.d("MemberRoleAssignAdapter", "[onClose] Removing role: $role from ${item.memberName}")
                            onRoleSelected(item.userId, role)
                        },
                        isCheckable = true
                    )
                    
                    // Set the chip as checked if it's the assigned role
                    chip.isChecked = role == item.assignedRole
                    
                    // Handle chip selection
                    chip.setOnClickListener {
                        if (!isSettingChecked) {
                            isSettingChecked = true
                            // Uncheck all other chips
                            for (i in 0 until binding.chipGroupSelectedRoles.childCount) {
                                val otherChip = binding.chipGroupSelectedRoles.getChildAt(i) as? Chip
                                if (otherChip != chip) {
                                    otherChip?.isChecked = false
                                } else {
                                    // Toggle the current chip's checked state
                                    chip.isChecked = !chip.isChecked
                                }
                            }
                            isSettingChecked = false
                        }
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

                // Set up AutoCompleteTextView for roles
                val roleAdapter = ArrayAdapter(
                    binding.root.context,
                    android.R.layout.simple_dropdown_item_1line,
                    roles
                )
                binding.actvRole.setAdapter(roleAdapter)
                binding.actvRole.isEnabled = item.isEditable

                // Set up role selection from AutoCompleteTextView
                binding.actvRole.setOnItemClickListener { _, _, position, _ ->
                    if (item.isEditable) {
                        val selectedRole = roles[position]
                        
                        // Check if this role is already selected
                        if (item.selectedRoles.contains(selectedRole)) {
                            Toast.makeText(
                                binding.root.context,
                                "This role is already selected",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnItemClickListener
                        }
                        
                        // Notify the ViewModel about the role selection
                        onRoleSelected(item.userId, selectedRole)
                        
                        // Clear the AutoCompleteTextView
                        binding.actvRole.text.clear()
                    } else {
                        // Show message that role is read-only
                        Toast.makeText(
                            binding.root.context,
                            "Role is already assigned and cannot be modified",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // Set up backup member AutoCompleteTextView
                val memberAdapter = ArrayAdapter(
                    binding.root.context,
                    android.R.layout.simple_dropdown_item_1line,
                    otherMembers.map { it.second } // Show member names
                )
                binding.actvBackupMember.setAdapter(memberAdapter)
                binding.actvBackupMember.isEnabled = item.isEditable

                // Only set up click listener if editable
                if (item.isEditable) {
                    binding.actvBackupMember.setOnItemClickListener { _, _, position, _ ->
                        val selectedMember = otherMembers[position]
                        onBackupMemberSelected(item.userId, selectedMember.first)
                    }
                } else {
                    binding.actvBackupMember.setOnItemClickListener(null)
                }

                // Set current backup member if exists
                if (item.backupMemberId.isNotBlank()) {
                    binding.cbAssignBackup.isChecked = true
                    binding.tilBackupMember.visibility = View.VISIBLE
                    binding.actvBackupMember.setText(item.backupMemberName, false)
                } else {
                    binding.cbAssignBackup.isChecked = false
                    binding.tilBackupMember.visibility = View.GONE
                }

                // Handle backup member selection
                binding.actvBackupMember.setOnItemClickListener { _, _, position, _ ->
                    val selectedMember = otherMembers[position]
                    onBackupMemberSelected(item.userId, selectedMember.first)
                }

                // Toggle backup member visibility
                binding.tilBackupMember.visibility =
                    if (binding.cbAssignBackup.isChecked) View.VISIBLE else View.GONE

                binding.cbAssignBackup.setOnCheckedChangeListener { _, isChecked ->
                    binding.tilBackupMember.visibility = if (isChecked) View.VISIBLE else View.GONE
                    if (!isChecked) {
                        onBackupMemberSelected(item.userId, "")
                        binding.actvBackupMember.setText("", false)
                    }
                }

                // Set up edit button - only show if there's an assigned role and it's not already editable
                binding.btnEdit.visibility = if (item.assignedRole.isNotBlank() && !item.isEditable) View.VISIBLE else View.GONE
                binding.btnEdit.setOnClickListener {
                    onToggleEditMode(item.userId, true)
                }
                
                // Set up assign role button - only show if editable
                binding.btnAssignRole.visibility = if (item.isEditable) View.VISIBLE else View.GONE
                binding.actvRole.isEnabled = item.isEditable

                // Set initial edit state based on whether role is assigned
                val isEditMode = item.assignedRole.isBlank()

                // Disable/enable input fields based on edit mode
                binding.actvRole.isEnabled = item.isEditable
                binding.actvBackupMember.isEnabled = item.isEditable
                binding.cbAssignBackup.isEnabled = item.isEditable

                // Handle Assign Role button click
                binding.btnAssignRole.setOnClickListener {
                    if (!item.isEditable) {
                        Toast.makeText(
                            binding.root.context,
                            "Role is already assigned and cannot be modified",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    // Get all selected roles from the chip group
                    val selectedRoles = mutableListOf<String>()
                    for (i in 0 until binding.chipGroupSelectedRoles.childCount) {
                        val chip = binding.chipGroupSelectedRoles.getChildAt(i) as? Chip
                        chip?.text?.toString()?.let { role ->
                            selectedRoles.add(role)
                        }
                    }

                    if (selectedRoles.isEmpty()) {
                        Toast.makeText(
                            binding.root.context,
                            "Please select at least one role",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    // Call onSaveRole with the first selected role
                    onSaveRole(item.userId, selectedRoles.first())
                    
                    // Clear the input and chip group
                    binding.actvRole.text.clear()
                    binding.chipGroupSelectedRoles.removeAllViews()
                }

                // Handle Edit button click
                binding.btnEdit.setOnClickListener {
                    // Make fields editable
                    binding.actvRole.isEnabled = true
                    binding.actvBackupMember.isEnabled = true
                    binding.cbAssignBackup.isEnabled = true
                    binding.btnEdit.visibility = View.GONE
                    binding.btnAssignRole.visibility = View.VISIBLE
                }


                // Make the AutoCompleteTextView clickable to show dropdown
                binding.actvRole.setOnClickListener {
                    if (item.isEditable) {
                        binding.actvRole.showDropDown()
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