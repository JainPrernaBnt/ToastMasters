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
        onRoleSelected: (String, String) -> Unit = { _, _ -> },
        onRoleRemoved: (String, String) -> Unit = { _, _ -> },
        onBackupMemberSelected: (String, String) -> Unit = { _, _ -> },
        onSaveRoles: (String, List<String>) -> Unit = { _, _ -> },
        onToggleEditMode: (String, Boolean) -> Unit = { _, _ -> }
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
            val chip = Chip(
                binding.root.context,
                null,
                com.google.android.material.R.style.Widget_Material3_Chip_Assist
            )
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
                chipGroupPreferredRoles.removeAllViews()
                android.util.Log.d(
                    "MemberRoleAssignAdapter",
                    "[bind] Adding ${item.preferredRoles.size} preferred roles"
                )

                // Show only unique base role names from preferred roles
                val uniqueBaseRoles = item.preferredRoles.map {
                    it.split(" (")[0] // Get base role name
                }.distinct()

                uniqueBaseRoles.forEach { baseRole ->
                    val chip = createChip(
                        text = baseRole,
                        isCloseable = false,
                        isEnabled = true
                    )
                    chipGroupPreferredRoles.addView(chip)
                }

                // Set up selected roles chips
                chipGroupSelectedRoles.removeAllViews()
                android.util.Log.d(
                    "MemberRoleAssignAdapter",
                    "[bind] Adding ${item.selectedRoles.size} selected roles, isEditable=${item.isEditable}"
                )

                // Add all selected roles as chips with their full numbered names
                item.selectedRoles.forEach { role ->
                    val displayName = role // Use the full role name as is
                    val chip = createChip(
                        text = displayName,
                        isCloseable = item.isEditable,
                        onClose = {
                            onRoleRemoved?.invoke(item.userId, role)
                        },
                        isCheckable = true
                    )

                    // Set the chip as checked if it's the assigned role
                    chip.isChecked = role == item.assignedRole

                    // Handle chip selection
                    chip.setOnClickListener {
                        if (!isSettingChecked && item.isEditable) {
                            isSettingChecked = true
                            // Toggle selection
                            if (chip.isChecked) {
                                // Uncheck all other chips
                                for (i in 0 until chipGroupSelectedRoles.childCount) {
                                    val otherChip = chipGroupSelectedRoles.getChildAt(i) as? Chip
                                    if (otherChip != chip) {
                                        otherChip?.isChecked = false
                                    }
                                }
                                onRoleSelected?.invoke(item.userId, role)
                            } else {
                                onRoleRemoved?.invoke(item.userId, role)
                            }
                            isSettingChecked = false
                        }
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

                // Set up AutoCompleteTextView for roles with counts
                val availableRoles = roles.filter { role ->
                    // Show all roles that are available (isAvailable = true) or assigned to this user
                    role.isAvailable || role.isAssignedToUser
                }

                // Generate all possible role slots from roleCounts
                val allRoleSlots = item.roleCounts.flatMap { (roleName, count) ->
                    if (count > 1) {
                        (1..count).map { "$roleName $it" }
                    } else {
                        listOf(roleName)
                    }
                }

                // Filter out roles that are already assigned to someone else
                val unassignedRoles = allRoleSlots.filter { role ->
                    !item.allAssignedRoles.containsKey(role) || item.allAssignedRoles[role] == item.userId
                }

                val roleAdapter = ArrayAdapter<String>(
                    binding.root.context,
                    android.R.layout.simple_dropdown_item_1line,
                    unassignedRoles
                )
                actvRole.setAdapter(roleAdapter)
                actvRole.isEnabled = item.isEditable && availableRoles.isNotEmpty()
                actvRole.hint =
                    if (availableRoles.isEmpty()) "All roles assigned" else "Select a role"

                // Set up backup member AutoCompleteTextView
                val memberAdapter = ArrayAdapter(
                    binding.root.context,
                    android.R.layout.simple_dropdown_item_1line,
                    otherMembers.map { it.second } // Show member names
                )
                actvBackupMember.setAdapter(memberAdapter)
                actvBackupMember.isEnabled = item.isEditable

                // Set current backup member if exists
                if (item.backupMemberId.isNotBlank()) {
                    cbAssignBackup.isChecked = true
                    tilBackupMember.visibility = View.VISIBLE
                    actvBackupMember.setText(item.backupMemberName, false)
                } else {
                    cbAssignBackup.isChecked = false
                    tilBackupMember.visibility = View.GONE
                }

                // Toggle backup member visibility
                tilBackupMember.visibility = if (cbAssignBackup.isChecked) View.VISIBLE else View.GONE

                cbAssignBackup.setOnCheckedChangeListener { _, isChecked ->
                    tilBackupMember.visibility = if (isChecked) View.VISIBLE else View.GONE
                    if (!isChecked) {
                        onBackupMemberSelected?.invoke(item.userId, "")
                        actvBackupMember.setText("", false)
                    }
                }

                // Only set up click listener if editable
                if (item.isEditable) {
                    actvBackupMember.setOnItemClickListener { _, _, position, _ ->
                        val selectedMember = otherMembers[position]
                        onBackupMemberSelected?.invoke(item.userId, selectedMember.first)
                    }
                } else {
                    actvBackupMember.setOnItemClickListener(null)
                }

                // Set up edit/cancel button
                btnEdit.visibility = View.VISIBLE
                btnEdit.text = if (item.isEditable) "Cancel" else "Edit"
                btnEdit.setOnClickListener {
                    if (item.isEditable) {
                        // Cancel edit mode - revert changes
                        onToggleEditMode?.invoke(item.userId, false)
                    } else {
                        // Enter edit mode
                        onToggleEditMode?.invoke(item.userId, true)
                    }
                }

                // Set up assign role button - only show if in edit mode
                btnAssignRole.visibility = if (item.isEditable) View.VISIBLE else View.GONE

                // Set up assign roles button click
                btnAssignRole.setOnClickListener {
                    if (!item.isEditable) return@setOnClickListener

                    // Get all selected roles from chips
                    val selectedRoles = mutableListOf<String>()
                    for (i in 0 until chipGroupSelectedRoles.childCount) {
                        val chip = chipGroupSelectedRoles.getChildAt(i) as? Chip
                        chip?.text?.toString()?.let { roleName ->
                            if (!selectedRoles.contains(roleName)) {
                                selectedRoles.add(roleName)
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

                    Toast.makeText(
                        binding.root.context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Handle dropdown item selection
                actvRole.setOnItemClickListener { _, _, position, _ ->
                    if (item.isEditable) {
                        val fullRoleName = actvRole.adapter?.getItem(position) as? String
                        fullRoleName?.let { roleName ->
                            if (!item.selectedRoles.contains(roleName)) {
                                addRoleToChipGroup(binding, roleName, item)
                                actvRole.text.clear()
                                onRoleSelected?.invoke(item.userId, roleName)
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
                actvRole.setOnClickListener {
                    if (item.isEditable) {
                        actvRole.showDropDown()
                    }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RoleAssignmentItem>() {
        override fun areItemsTheSame(
            oldItem: RoleAssignmentItem,
            newItem: RoleAssignmentItem
        ) = oldItem.userId == newItem.userId

        override fun areContentsTheSame(
            oldItem: RoleAssignmentItem,
            newItem: RoleAssignmentItem
        ) = oldItem == newItem
    }
}