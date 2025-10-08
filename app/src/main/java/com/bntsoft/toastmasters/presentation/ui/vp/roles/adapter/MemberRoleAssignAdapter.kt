package com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
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
    private var onEvaluatorSelected: ((String, String) -> Unit) = { _, _ -> }
    private var recentRoles: Map<String, List<String>> = emptyMap()

    fun setCallbacks(
        onRoleSelected: (String, String) -> Unit = { _, _ -> },
        onRoleRemoved: (String, String) -> Unit = { _, _ -> },
        onBackupMemberSelected: (String, String) -> Unit = { _, _ -> },
        onSaveRoles: (String, List<String>) -> Unit = { _, _ -> },
        onToggleEditMode: (String, Boolean) -> Unit = { _, _ -> },
        onEvaluatorSelected: (String, String) -> Unit = { _, _ -> }
    ) {
        this.onRoleSelected = onRoleSelected
        this.onRoleRemoved = onRoleRemoved
        this.onBackupMemberSelected = onBackupMemberSelected
        this.onSaveRoles = onSaveRoles
        this.onToggleEditMode = onToggleEditMode
        this.onEvaluatorSelected = onEvaluatorSelected
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

        holder.bind(
            item,
            assignableRoles,
            onRoleSelected,
            onRoleRemoved,
            onBackupMemberSelected,
            availableMembers,
            onSaveRoles,
            onToggleEditMode,
            onEvaluatorSelected
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
    
    fun setRecentRoles(roles: Map<String, List<String>>) {
        recentRoles = roles
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemMemberRoleAssignmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val evaluatorBinding = binding.evaluatorPrompt
        private val recentRolesAdapter = RecentRolesAdapter(emptyList())

        init {
            binding.rvRecentRoles.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = recentRolesAdapter
            }
            
            // Initially hide recent roles section
            binding.tvRecentRolesLabel.visibility = View.GONE
            binding.rvRecentRoles.visibility = View.GONE
        }


        private fun setupEvaluatorSelection(
            item: RoleAssignmentItem,
            onEvaluatorSelected: ((String, String) -> Unit)?
        ) {
            evaluatorBinding.apply {
                // Get the current list of members with their roles
                val currentMembers = this@MemberRoleAssignAdapter.currentList
                val allMembers = this@MemberRoleAssignAdapter.availableMembers

                // Only show actual members in evaluator dropdown (no guests)
                val membersOnly = allMembers.filter { member ->
                    !member.second.startsWith("Guest:") && !member.first.startsWith("guest_")
                }

                // Get all members who are already evaluators (for highlighting)
                val evaluators = currentMembers
                    .filter { assignment ->
                        assignment.selectedRoles.any { it.startsWith("Evaluator") } &&
                                assignment.userId != item.userId && // Exclude current speaker
                                !assignment.memberName.startsWith("Guest:") && // Exclude guests
                                !assignment.userId.startsWith("guest_") // Exclude guest IDs
                    }
                    .mapNotNull { assignment ->
                        membersOnly.find { it.first == assignment.userId }
                    }

                // Get members who have "Evaluator" in their preferred roles
                val preferredEvaluators = currentMembers
                    .filter { assignment ->
                        assignment.preferredRoles.any { it.equals("Evaluator", ignoreCase = true) } &&
                                assignment.userId != item.userId && // Exclude current speaker
                                !assignment.memberName.startsWith("Guest:") && // Exclude guests
                                !assignment.userId.startsWith("guest_") && // Exclude guest IDs
                                !evaluators.any { it.first == assignment.userId } // Don't duplicate evaluators
                    }
                    .mapNotNull { assignment ->
                        membersOnly.find { it.first == assignment.userId }
                    }

                // Get all other available members (excluding current speaker, guests, and already listed members)
                val otherMembers = membersOnly.filter { member ->
                    member.first != item.userId && // Don't allow self-evaluation
                            !evaluators.any { it.first == member.first } &&
                            !preferredEvaluators.any { it.first == member.first }
                }
                // Combine all members in priority order: evaluators > preferred evaluators > other members
                val allMembersInOrder = evaluators + preferredEvaluators + otherMembers

                // Create a list of member names with evaluator status
                val memberNames = allMembersInOrder.map { member ->
                    when {
                        evaluators.any { it.first == member.first } -> "${member.second} (Evaluator)"
                        preferredEvaluators.any { it.first == member.first } -> "${member.second} (Prefers to Evaluate)"
                        else -> member.second
                    }
                }

                val memberAdapter = ArrayAdapter(
                    root.context,
                    android.R.layout.simple_dropdown_item_1line,
                    memberNames
                )

                actvEvaluator.setAdapter(memberAdapter)
                actvEvaluator.setText("", false) // Clear any existing text

                // Clear any existing click listeners to prevent duplicates
                actvEvaluator.onItemClickListener = null

                actvEvaluator.setOnItemClickListener { _, _, position, _ ->
                    val selectedMember = allMembersInOrder[position]
                    val selectedMemberId = selectedMember.first
                    
                    // Notify the parent about the new evaluator
                    onEvaluatorSelected?.invoke(item.userId, selectedMemberId)
                    
                    // Clear the input
                    actvEvaluator.setText("", false)
                }
            }
        }
        
        private fun isEvaluatorAlreadyAdded(evaluatorId: String): Boolean {
            val count = evaluatorBinding.chipGroupEvaluators.childCount

            if (count == 0) {
                return false
            }
            
            for (i in 0 until count) {
                val chip = evaluatorBinding.chipGroupEvaluators.getChildAt(i) as? Chip
                val chipTag = chip?.tag?.toString()
                if (chipTag == evaluatorId) {
                    return true
                }
            }
            return false
        }
        
        private fun addEvaluatorChip(
            evaluatorId: String,
            evaluatorName: String,
            item: RoleAssignmentItem,
            onEvaluatorSelected: ((String, String) -> Unit)?
        ) {

            // Only proceed if we don't already have this evaluator chip
            if (!isEvaluatorAlreadyAdded(evaluatorId)) {

                val chip = createChip(
                    text = evaluatorName,
                    isCloseable = true,
                    onClose = {
                        evaluatorBinding.chipGroupEvaluators.removeView(it)
                        onEvaluatorSelected?.invoke(item.userId, "$evaluatorId:remove")
                    }
                )
                
                // Store the evaluator ID in the chip's tag for easy reference
                chip.tag = evaluatorId

                // Style the chip
                try {
                    chip.setChipBackgroundColorResource(R.color.evaluator_bg)
                    chip.setTextColor(evaluatorBinding.root.context.getColor(R.color.evaluator_text))
                } catch (e: Exception) {
                }
                
                // Add the chip to the group
                try {
                    evaluatorBinding.chipGroupEvaluators.addView(chip)
                } catch (e: Exception) {
                }
            } else {
            }
        }

        private var currentItem: RoleAssignmentItem? = null
        private var currentRoles: List<MemberRoleAssignViewModel.RoleDisplayItem> = emptyList()
        private var availableMembers: List<Pair<String, String>> = emptyList()
        private var isSettingChecked = false

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
            onToggleEditMode: ((String, Boolean) -> Unit)?,
            onEvaluatorSelected: ((String, String) -> Unit)?
        ) {

            this.currentItem = item
            this.currentRoles = roles
            this.availableMembers = availableMembers

            // Only show actual members in backup dropdown (no guests, no current user)
            val otherMembers = availableMembers.filter { 
                it.first != item.userId && !it.second.startsWith("Guest:") && !it.first.startsWith("guest_")
            }

            binding.apply {
                tvMemberName.text = item.memberName
                
                // Check if this is a guest
                val isGuest = item.memberName.startsWith("Guest:") || item.userId.startsWith("guest_")
                
                // Hide recent roles for guests
                if (isGuest) {
                    tvRecentRolesLabel.visibility = View.GONE
                    rvRecentRoles.visibility = View.GONE
                } else {
                    // Update recent roles for members only
                    recentRoles[item.userId]?.let { roles ->
                        if (roles.isNotEmpty()) {
                            tvRecentRolesLabel.visibility = View.VISIBLE
                            rvRecentRoles.visibility = View.VISIBLE
                            recentRolesAdapter.updateRoles(roles)
                        } else {
                            tvRecentRolesLabel.visibility = View.GONE
                            rvRecentRoles.visibility = View.GONE
                        }
                    } ?: run {
                        tvRecentRolesLabel.visibility = View.GONE
                        rvRecentRoles.visibility = View.GONE
                    }
                }

                // Handle evaluator selection UI
                val isSpeaker = item.selectedRoles.any { it.startsWith("Speaker") }
                evaluatorPrompt.root.visibility = if (isSpeaker) View.VISIBLE else View.GONE
                
                // Always setup evaluator selection to refresh the list
                if (isSpeaker) {
                    setupEvaluatorSelection(item, onEvaluatorSelected)
                    
                    // Clear existing evaluator chips
                    evaluatorBinding.chipGroupEvaluators.removeAllViews()

                    item.evaluatorIds.forEachIndexed { index, evaluatorId ->
                        val evaluator = availableMembers.find { it.first == evaluatorId }
                        if (evaluator != null) {
                            if (!isEvaluatorAlreadyAdded(evaluator.first)) {
                                addEvaluatorChip(
                                    evaluatorId = evaluator.first,
                                    evaluatorName = evaluator.second,
                                    item = item,
                                    onEvaluatorSelected = onEvaluatorSelected
                                )
                            } else {
                                Log.d("EvaluatorDebug", "Evaluator already added: ${evaluator.second}")
                            }
                        } else {
                            Log.d("EvaluatorDebug", "Evaluator not found in available members: $evaluatorId")
                            Log.d("EvaluatorDebug", "Available members: $availableMembers")
                        }
                    }
                }
                // Set up preferred roles chips - hide for guests
                chipGroupPreferredRoles.removeAllViews()
                
                if (isGuest) {
                    tvPreferredRolesLabel.visibility = View.GONE
                    chipGroupPreferredRoles.visibility = View.GONE
                } else {
                    tvPreferredRolesLabel.visibility = View.VISIBLE
                    chipGroupPreferredRoles.visibility = View.VISIBLE
                    
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
                        val context = binding.root.context
                        val (bgColor, textColor) = when (baseRole.lowercase()) {
                            "toastmaster of the day" -> R.color.toastmaster_bg to R.color.toastmaster_text
                            "speaker" -> R.color.speaker_bg to R.color.speaker_text
                            "evaluator" -> R.color.evaluator_bg to R.color.evaluator_text
                            "timer" -> R.color.timer_bg to R.color.timer_text
                            "ah-counter" -> R.color.ah_counter_bg to R.color.ah_counter_text
                            "grammarian" -> R.color.grammarian_bg to R.color.grammarian_text
                            "sergeant-at-arms" -> R.color.sergeant_bg to R.color.sergeant_text
                            "presiding officer" -> R.color.presiding_bg to R.color.presiding_text
                            "table topics master" -> R.color.ttm_bg to R.color.ttm_text
                            "table topics speaker" -> R.color.tts_bg to R.color.tts_text
                            "quiz master" -> R.color.quiz_master_bg to R.color.quiz_master_text
                            else -> R.color.default_role_bg to R.color.default_role_text
                        }

                        chip.setChipBackgroundColorResource(bgColor)
                        chip.setTextColor(context.getColor(textColor))

                        chipGroupPreferredRoles.addView(chip)
                    }
                }

                // Set up selected roles chips
                chipGroupSelectedRoles.removeAllViews()

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

                // Evaluator UI visibility is already handled above

                // Set up recent roles
                // Use the roles from adapter's recentRoles map
                val rolesToShow = recentRoles[item.userId] ?: emptyList()

                if (rolesToShow.isNotEmpty()) {
                    rvRecentRoles.visibility = View.VISIBLE
                    tvNoRecentRoles.visibility = View.GONE
                    recentRolesAdapter.updateRoles(rolesToShow)
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

                val unassignedRoles = allRoleSlots.filter { role ->
                    val assignedUsers = item.allAssignedRoles[role] ?: emptyList()
                    assignedUsers.isEmpty() || assignedUsers.contains(item.userId)
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

                // Set up backup member AutoCompleteTextView - hide for guests
                if (isGuest) {
                    cbAssignBackup.visibility = View.GONE
                    tilBackupMember.visibility = View.GONE
                } else {
                    cbAssignBackup.visibility = View.VISIBLE
                    
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
                            onEvaluatorSelected?.invoke(item.userId, selectedMember.first)
                        }
                    } else {
                        actvBackupMember.setOnItemClickListener(null)
                    }
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