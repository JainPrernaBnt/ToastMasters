package com.bntsoft.toastmasters.presentation.ui.vp.memberapproval.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.ItemMemberApprovalBinding
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.models.UserRole
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.chip.Chip

class MemberApprovalAdapter(
    private val onApproveClick: (User) -> Unit,
    private val onRejectClick: (User) -> Unit,
    private val onApplyMentors: (User, List<String>) -> Unit,
    private val onAssignVp: (User) -> Unit
) : ListAdapter<User, MemberApprovalAdapter.MemberViewHolder>(MemberDiffCallback()) {

    // Track mentor input visibility state per user
    private val mentorInputVisibility = mutableMapOf<String, Boolean>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding =
            ItemMemberApprovalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(private val binding: ItemMemberApprovalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val avatarImageView = binding.avatarImageView
        private val nameTextView = binding.nameTextView
        private val emailTextView = binding.emailTextView
        private val approveButton = binding.approveButton
        private val rejectButton = binding.rejectButton
        private val mentorsChipGroup = binding.mentorsChipGroup
        private val addMentorEditText = binding.mentorNamesEditText
        private val applyMentorsButton = binding.applyMentorsButton
        private val assignMentor = binding.assignMentorButton
        private val assignVP = binding.vpMembershipSwitch

        fun bind(member: User) {
            // Load avatar with fallback
            Glide.with(itemView)
                .load(R.drawable.ic_person)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .placeholder(R.drawable.ic_person)
                .into(avatarImageView)

            nameTextView.text = member.name
            emailTextView.text = member.email

            assignMentor.setOnClickListener {
                mentorInputVisibility[member.id] = true
                setMentorInputVisibility(true)
            }
            // Restore visibility state
            val isMentorVisible = mentorInputVisibility[member.id] ?: false
            setMentorInputVisibility(isMentorVisible)

            // Populate mentor chips
            mentorsChipGroup.removeAllViews()
            member.mentorNames.forEach { mentorName ->
                val chip = Chip(itemView.context).apply {
                    text = mentorName
                    isCloseIconVisible = true
                    setChipBackgroundColorResource(R.color.chip_background)
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))

                    setOnCloseIconClickListener {
                        val updatedMentors =
                            member.mentorNames.toMutableList().apply { remove(mentorName) }
                        onApplyMentors(member.copy(mentorNames = updatedMentors), updatedMentors)

                        // Update UI immediately
                        mentorsChipGroup.removeView(this)
                    }
                }
                mentorsChipGroup.addView(chip)
            }

            // Add mentor
            addMentorEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    applyMentorsButton.performClick()
                    true
                } else false
            }

            applyMentorsButton.setOnClickListener {
                val newMentor = addMentorEditText.text.toString().trim()
                if (newMentor.isNotEmpty()) {
                    val updatedMentors = member.mentorNames.toMutableList().apply { add(newMentor) }
                    val updatedMember = member.copy(mentorNames = updatedMentors)

                    onApplyMentors(updatedMember, updatedMentors)

                    // Update UI immediately
                    val chip = Chip(itemView.context).apply {
                        text = newMentor
                        isCloseIconVisible = true
                        setChipBackgroundColorResource(R.color.chip_background)
                        setTextColor(ContextCompat.getColor(context, android.R.color.white))

                        setOnCloseIconClickListener {
                            val refreshed = updatedMember.mentorNames.toMutableList()
                                .apply { remove(newMentor) }
                            onApplyMentors(updatedMember.copy(mentorNames = refreshed), refreshed)
                            mentorsChipGroup.removeView(this)
                        }
                    }
                    mentorsChipGroup.addView(chip)

                    addMentorEditText.text?.clear()
                }
            }

            // Approve
            approveButton.setOnClickListener {
                val updatedMember = member.copy(mentorNames = member.mentorNames)
                onApproveClick(updatedMember)
                refreshMember(updatedMember)
            }

            // Reject
            rejectButton.setOnClickListener {
                onRejectClick(member)
            }

            assignVP.setOnCheckedChangeListener(null)
            assignVP.isChecked = member.isVpEducation

            assignVP.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && member.role != UserRole.VP_EDUCATION) {
                    // Promote to VP Education
                    AlertDialog.Builder(itemView.context)
                        .setTitle("Assign VP Education")
                        .setMessage("Do you want to give ${member.name} the same permissions as VP Education?")
                        .setPositiveButton("Yes") { _, _ ->
                            onAssignVp(member)
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                            assignVP.isChecked = false
                        }
                        .show()

                } else if (!isChecked && member.role == UserRole.VP_EDUCATION) {
                    // Demote back to MEMBER
                    AlertDialog.Builder(itemView.context)
                        .setTitle("Remove VP Education Role")
                        .setMessage("Do you want to remove VP Education role from ${member.name}?")
                        .setPositiveButton("Yes") { _, _ ->
                            val updatedMember = member.copy(role = UserRole.MEMBER)
                            onAssignVp(updatedMember)
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                            assignVP.isChecked = true
                        }
                        .show()
                }
            }
        }

        private fun setMentorInputVisibility(visible: Boolean) {
            binding.mentorInputLayout.visibility = if (visible) View.VISIBLE else View.GONE
            if (!visible) {
                addMentorEditText.text?.clear()
            }
        }

        private fun refreshMember(updatedMember: User) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                submitList(currentList.toMutableList().apply {
                    set(position, updatedMember)
                })
            }
        }
    }

    class MemberDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }
}
