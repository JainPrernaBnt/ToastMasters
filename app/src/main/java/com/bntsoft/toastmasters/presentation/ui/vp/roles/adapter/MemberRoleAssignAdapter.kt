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
import com.bntsoft.toastmasters.presentation.ui.vp.roles.MemberRoleAssignViewModel
import com.google.android.material.chip.Chip

class MemberRoleAssignAdapter :
    ListAdapter<RoleAssignmentItem, MemberRoleAssignAdapter.ViewHolder>(DiffCallback()) {
    private var assignableRoles: List<MemberRoleAssignViewModel.RoleDisplayItem> = emptyList()
    private var availableMembers: List<Pair<String, String>> = emptyList()
    private var onRoleSelected: ((String, String) -> Unit)? = null
    private var onRoleRemoved: ((String, String) -> Unit)? = null
    private var onBackupMemberSelected: ((String, String) -> Unit)? = null
    private var onSaveRoles: ((String, List<String>) -> Unit)? = null
    private var onToggleEditMode: ((String, Boolean) -> Unit)? = null
    private var currentUserId: String = ""


    fun setCallbacks(
        onRoleSelected: (String, String) -> Unit,
        onRoleRemoved: (String, String) -> Unit,
        onBackupMemberSelected: (String, String) -> Unit,
        onSaveRoles: (String, List<String>) -> Unit,
        onToggleEditMode: (String, Boolean) -> Unit
    ) {
        this.onRoleSelected = onRoleSelected
        this.onRoleRemoved = onRoleRemoved
        this.onBackupMemberSelected = onBackupMemberSelected
        this.onSaveRoles = onSaveRoles
        this.onToggleEditMode = onToggleEditMode
    }

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
        android.util.Log.d(
            "MemberRoleAssignAdapter", "[onBindViewHolder] Binding item at position $position: " +
                    "member=${item.memberName}, assignedRole=${item.assignedRole}, " +
                    "selectedRoles=${item.selectedRoles}, isEditable=${item.isEditable}"
        )

        holder.bind(
            item,
            assignableRoles,
            onRoleSelected,
            onRoleRemoved,
            onBackupMemberSelected,
            availableMembers,
            onSaveRoles,
            onToggleEditMode
        )
    }

    fun updateAssignableRoles(roleItems: List<MemberRoleAssignViewModel.RoleDisplayItem>) {
        assignableRoles = roleItems
        notifyDataSetChanged()
    }

    fun updateAvailableMembers(newMembers: List<Pair<String, String>>) {
        availableMembers = newMembers
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemMemberRoleAssignmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentItem: RoleAssignmentItem? = null
        private var currentRoles: List<MemberRoleAssignViewModel.RoleDisplayItem> = emptyList()
        private var availableMembers: List<Pair<String, String>> = emptyList()
        private var isSettingChecked = false

        private fun addRoleFromInput(
            binding: ItemMemberRoleAssignmentBinding,
            item: RoleAssignmentItem
        ) {
            val role = binding.actvRole.text.toString().trim()
            if (role.isNotEmpty()) {
                addRoleToChipGroup(binding, role, item)
                binding.actvRole.text.clear()
            }
        }

        private fun addRoleToChipGroup(
            binding: ItemMemberRoleAssignmentBinding,
            role: String,
            item: RoleAssignmentItem
        ) {
            // Check if role already exists in chip group
            for (i in 0 until binding.chipGroupSelectedRoles.childCount) {
                val chip = binding.chipGroupSelectedRoles.getChildAt(i) as? Chip
                if (chip?.text.toString().startsWith(role)) {
                    // Role already exists, don't add again
                    return
                }
            }

            // Get role display name with count
            val displayName = item.getRoleDisplayName(role)
            
            // Add new role chip
            val chip = createChip(
                displayName,
                isCloseable = item.isEditable,
                onClose = {
                    binding.chipGroupSelectedRoles.removeView(it)
                    this@MemberRoleAssignAdapter.onRoleRemoved?.invoke(item.userId, role)
                }
            )
            binding.chipGroupSelectedRoles.addView(chip)

            // Notify ViewModel about the new role
            this@MemberRoleAssignAdapter.onRoleSelected?.invoke(item.userId, role)
        }

        private fun createChip(
            text: String,
            isCloseable: Boolean = true,
            onClose: ((View) -> Unit)? = null,
            isCheckable: Boolean = false,
            isEnabled: Boolean = true
        ): Chip {
            val chip = Chip(binding.root.context, null, com.google.android.material.R.style.Widget_Material3_Chip_Assist)
            chip.text = text
            chip.isCloseIconVisible = isCloseable && isEnabled
            chip.isCheckable = isCheckable
            chip.isEnabled = isEnabled
            
            // Set visual appearance for disabled chips
            if (!isEnabled) {
                chip.alpha = 0.5f
            }

            if (isCloseable && isEnabled && onClose != null) {
                chip.setOnCloseIconClickListener { onClose(chip) }
            }
            return chip
        }

        fun bind(
            item: RoleAssignmentItem,
            roles: List<MemberRoleAssignViewModel.RoleDisplayItem>,
            onRoleSelected: ((String, String) -> Unit)?,
            onRoleRemoved: ((String, String) -> Unit)?,
            onBackupMemberSelected: ((String, String) -> Unit)?,
            availableMembers: List<Pair<String, String>>,
            onSaveRoles: ((String, List<String>) -> Unit)?,
            onToggleEditMode: ((String, Boolean) -> Unit)?
        ) {
            this.currentItem = item
            this.currentRoles = roles
            this.availableMembers = availableMembers
            // Initialize bindings
            android.util.Log.d(
                "MemberRoleAssignAdapter",
                "[bind] Binding member: ${item.memberName}"
            )
            android.util.Log.d(
                "MemberRoleAssignAdapter",
                "[bind] Current state - assignedRole: ${item.assignedRole}, " +
                        "selectedRoles: ${item.selectedRoles}, isEditable: ${item.isEditable}"
            )
            android.util.Log.d("MemberRoleAssignAdapter", "[bind] Available roles: $roles")
            // Filter out current member from available members
            val otherMembers = availableMembers.filter { it.first != item.userId }
            binding.apply {
                tvMemberName.text = item.memberName

                // Set up preferred roles chips
                binding.chipGroupPreferredRoles.removeAllViews()
                android.util.Log.d(
                    "MemberRoleAssignAdapter",
                    "[bind] Adding ${item.preferredRoles.size} preferred roles"
                )
                
                // Group preferred roles by base name (without count)
                val preferredRoleGroups = item.preferredRoles.groupBy { 
                    it.split(" (")[0] // Get base role name
                }
                
                preferredRoleGroups.forEach { (baseRole, roles) ->
                    val remainingSlots = item.getRemainingSlots(baseRole)
                    val isRoleAvailable = remainingSlots > 0 || 
                                        item.selectedRoles.any { it.startsWith(baseRole) }
                    
                    // Show count if there are multiple slots
                    val displayText = if (item.roleCounts[baseRole] ?: 1 > 1) {
                        "$baseRole ($remainingSlots/${item.roleCounts[baseRole]})"
                    } else {
                        baseRole
                    }
                    
                    val chip = createChip(
                        text = displayText,
                        isCloseable = false,
                        isEnabled = isRoleAvailable
                    )
                    
                    if (isRoleAvailable) {
                        chip.setOnClickListener {
                            if (item.isEditable) {
                                addRoleToChipGroup(binding, baseRole, item)
                            }
                        }
                    }
                    
                    binding.chipGroupPreferredRoles.addView(chip)
                }

                // Set up selected roles chips
                binding.chipGroupSelectedRoles.removeAllViews()
                android.util.Log.d(
                    "MemberRoleAssignAdapter",
                    "[bind] Adding ${item.selectedRoles.size} selected roles, isEditable=${item.isEditable}"
                )
                
                // Add all selected roles as chips with proper display names
                item.selectedRoles.forEach { role ->
                    val displayName = item.getRoleDisplayName(role)
                    val chip = createChip(
                        text = displayName,
                        isCloseable = item.isEditable,
                        onClose = {
                            onRoleRemoved?.invoke(item.userId, role)
                        }
                    )
                    binding.chipGroupSelectedRoles.addView(chip)
                }

                // Set up edit/cancel button
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnEdit.text = if (item.isEditable) "Cancel" else "Edit"
                binding.btnEdit.setOnClickListener {
                    if (item.isEditable) {
                        // Cancel edit mode - revert changes
                        onToggleEditMode?.invoke(item.userId, false)
                    } else {
                        // Enter edit mode
                        onToggleEditMode?.invoke(item.userId, true)
                    }
                }

                item.selectedRoles.forEach { role ->
                    val chip = createChip(
                        text = role,
                        isCloseable = item.isEditable, // Show close button only in edit mode
                        onClose = {
                            // On close icon click, remove the role
                            android.util.Log.d(
                                "MemberRoleAssignAdapter",
                                "[onClose] Removing role: $role from ${item.memberName}"
                            )
                            onRoleRemoved?.invoke(item.userId, role)
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
                                val otherChip =
                                    binding.chipGroupSelectedRoles.getChildAt(i) as? Chip
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
                val availableRoles = roles.filter { role ->
                    // Show all roles that are available (isAvailable = true) or assigned to this user
                    role.isAvailable || role.isAssignedToUser
                }
                
                val roleNames = availableRoles.map { it.displayName }
                val roleAdapter = ArrayAdapter<String>(
                    binding.root.context,
                    android.R.layout.simple_dropdown_item_1line,
                    roleNames
                )
                binding.actvRole.setAdapter(roleAdapter)
                binding.actvRole.isEnabled = item.isEditable && availableRoles.isNotEmpty()
                binding.actvRole.hint = if (availableRoles.isEmpty()) "All roles assigned" else "Select a role"

                // Set up role selection from AutoCompleteTextView
                binding.actvRole.setOnItemClickListener { _, _, position, _ ->
                    if (item.isEditable) {
                        val selectedRoleDisplayName = binding.actvRole.adapter?.getItem(position) as? String
                        selectedRoleDisplayName?.let { displayName ->
                            // Find the role that matches both the display name and is available
                            val selectedRole = availableRoles.find { it.displayName == displayName }?.role
                            selectedRole?.let { role ->
                                // Check if this role is already selected (shouldn't happen due to filtering)
                                if (item.selectedRoles.any { it == role }) {
                                    Toast.makeText(
                                        binding.root.context,
                                        "This role is already selected",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@setOnItemClickListener
                                }

                                // Notify the ViewModel about the role selection
                                onRoleSelected?.invoke(item.userId, role)

                                // Clear the AutoCompleteTextView
                                binding.actvRole.text.clear()
                                
                                // Add the role to the selected roles chip group
                                val displayName = roles.find { it.role == role }?.displayName ?: role
                                val chip = createChip(
                                    text = displayName,
                                    isCloseable = item.isEditable,
                                    onClose = {
                                        onRoleRemoved?.invoke(item.userId, role)
                                    },
                                    isCheckable = true,
                                    isEnabled = item.isEditable
                                )
                                binding.chipGroupSelectedRoles.addView(chip)
                                
                                // Set checked if this is the assigned role
                                if (role == item.assignedRole) {
                                    (chip as? Chip)?.isChecked = true
                                }
                                
                                // Handle role selection
                                chip.setOnClickListener {
                                    if (item.isEditable) {
                                        onRoleSelected?.invoke(item.userId, role)
                                    }
                                }
                            }
                        }
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
                        onBackupMemberSelected?.invoke(item.userId, selectedMember.first)
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
                    onBackupMemberSelected?.invoke(item.userId, selectedMember.first)
                }

                // Toggle backup member visibility
                binding.tilBackupMember.visibility =
                    if (binding.cbAssignBackup.isChecked) View.VISIBLE else View.GONE

                binding.cbAssignBackup.setOnCheckedChangeListener { _, isChecked ->
                    binding.tilBackupMember.visibility = if (isChecked) View.VISIBLE else View.GONE
                    if (!isChecked) {
                        onBackupMemberSelected?.invoke(item.userId, "")
                        binding.actvBackupMember.setText("", false)
                    }
                }

                // Set up assign role button - only show if in edit mode
                binding.btnAssignRole.visibility = if (item.isEditable) View.VISIBLE else View.GONE
                binding.actvRole.isEnabled = item.isEditable

                // Set up assign roles button click
                binding.btnAssignRole.setOnClickListener {
                    if (!item.isEditable) return@setOnClickListener

                    // Get all selected roles from chips
                    val selectedRoles = mutableListOf<String>()
                    for (i in 0 until binding.chipGroupSelectedRoles.childCount) {
                        val chip = binding.chipGroupSelectedRoles.getChildAt(i) as? Chip
                        chip?.text?.toString()?.let { displayName ->
                            // Find the actual role name from display name
                            roles.find { it.displayName == displayName }?.role?.let { role ->
                                if (!selectedRoles.contains(role)) {
                                    selectedRoles.add(role)
                                }
                            }
                        }
                    }

                    if (selectedRoles.isEmpty()) {
                        Toast.makeText(
                            binding.root.context,
                            "Please add at least one role",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    // Save all selected roles
                    onSaveRoles?.invoke(item.userId, selectedRoles)

                    // Exit edit mode
                    onToggleEditMode?.invoke(item.userId, false)

                    val message = if (selectedRoles.size == 1) {
                        "1 role assigned successfully"
                    } else {
                        "${selectedRoles.size} roles assigned successfully"
                    }
                    
                    // Update the UI to show assigned roles in the selected roles chip group
                    binding.chipGroupSelectedRoles.removeAllViews()
                    item.selectedRoles.forEach { role ->
                        val displayName = item.getRoleDisplayName(role)
                        val chip = createChip(
                            text = displayName,
                            isCloseable = item.isEditable,
                            onClose = {
                                onRoleRemoved?.invoke(item.userId, role)
                            },
                            isCheckable = true,
                            isEnabled = item.isEditable
                        )
                        binding.chipGroupSelectedRoles.addView(chip)
                        
                        // Set checked if this is the assigned role
                        if (role == item.assignedRole) {
                            (chip as? Chip)?.isChecked = true
                        }
                        
                        // Handle role selection
                        chip.setOnClickListener {
                            if (item.isEditable) {
                                onRoleSelected?.invoke(item.userId, role)
                            }
                        }
                    }

                    Toast.makeText(
                        binding.root.context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Handle dropdown item selection
                binding.actvRole.setOnItemClickListener { _, _, position, _ ->
                    val selectedRoleName = binding.actvRole.adapter?.getItem(position) as? String
                    selectedRoleName?.let { roleName ->
                        val selectedRole = roles.find { it.displayName == roleName }?.role
                        selectedRole?.let { role ->
                            if (!item.selectedRoles.contains(role)) {
                                addRoleToChipGroup(binding, role, item)
                                binding.actvRole.text.clear()
                                
                                // Notify role selection
                                onRoleSelected?.invoke(item.userId, role)
                            } else {
                                Toast.makeText(
                                    binding.root.context,
                                    "This role is already selected",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                // Make the AutoCompleteTextView clickable to show dropdown
                binding.actvRole.setOnClickListener {
                    if (item.isEditable) {
                        binding.actvRole.showDropDown()
                    }
                }

                // Update chip group based on selected roles
                binding.chipGroupSelectedRoles.removeAllViews()
                item.selectedRoles.forEach { role ->
                    val chip = createChip(
                        text = role,
                        isCloseable = item.isEditable,
                        onClose = { onRoleRemoved?.invoke(item.userId, role) },
                        isCheckable = true
                    )
                    chip.isChecked = role == item.assignedRole
                    binding.chipGroupSelectedRoles.addView(chip)
                }
            }
        }
    }
}


class DiffCallback : DiffUtil.ItemCallback<RoleAssignmentItem>() {
    override fun areItemsTheSame(
        oldItem: RoleAssignmentItem,
        newItem: RoleAssignmentItem
    ) =
        oldItem.userId == newItem.userId

    override fun areContentsTheSame(
        oldItem: RoleAssignmentItem,
        newItem: RoleAssignmentItem
    ) =
        oldItem == newItem
}


