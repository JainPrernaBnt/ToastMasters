package com.bntsoft.toastmasters.presentation.member.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.databinding.ItemMemberApprovalBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.*

class MemberApprovalAdapter(
    private val onApproveClick: (User) -> Unit,
    private val onRejectClick: (User) -> Unit,
    private val onApplyMentors: (member: User, mentorNames: List<String>) -> Unit
) : ListAdapter<User, MemberApprovalAdapter.MemberApprovalViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberApprovalViewHolder {
        val binding = ItemMemberApprovalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberApprovalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberApprovalViewHolder, position: Int) {
        val member = getItem(position)
        holder.bind(member)
    }

    inner class MemberApprovalViewHolder(
        private val binding: ItemMemberApprovalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        private var isMentorInputVisible = false
        
        // Store the current member
        private var currentMember: User? = null

        init {
            binding.apply {
                // Toggle mentor input visibility
                assignMentorButton.setOnClickListener {
                    isMentorInputVisible = !isMentorInputVisible
                    updateMentorInputVisibility()

                    if (isMentorInputVisible) {
                        mentorNamesEditText.requestFocus()
                    }
                }

                // Handle apply mentors
                applyMentorsButton.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val member = getItem(position)
                        val raw = mentorNamesEditText.text?.toString().orEmpty()
                        val names = raw.split(',')
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        if (names.isNotEmpty()) {
                            onApplyMentors(member, names)
                            isMentorInputVisible = false
                            updateMentorInputVisibility()
                        }
                    }
                }

                // Handle approve button
                approveButton.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onApproveClick(getItem(position))
                    }
                }

                // Handle reject button
                rejectButton.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onRejectClick(getItem(position))
                    }
                }

                // Handle keyboard done action
                mentorNamesEditText.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                        applyMentorsButton.performClick()
                        true
                    } else {
                        false
                    }
                }
            }
        }

        private fun updateMentorInputVisibility() {
            with(binding) {
                if (isMentorInputVisible) {
                    mentorInputLayout.visibility = View.VISIBLE
                    assignMentorButton.text = itemView.context.getString(R.string.cancel)
                    mentorNamesEditText.requestFocus()
                } else {
                    mentorInputLayout.visibility = View.GONE
                    assignMentorButton.text = itemView.context.getString(R.string.assign_mentor)
                    mentorNamesEditText.text?.clear()
                }
            }
        }

        fun bind(member: User) {
            currentMember = member
            
            // Reset input visibility when binding new data
            isMentorInputVisible = false
            updateMentorInputVisibility()

            with(binding) {
                // Set member name
                nameTextView.text = member.name

                // Set email
                emailTextView.text = member.email

                // Set phone number (if available)
                phoneTextView.text = member.phoneNumber.ifEmpty {
                    itemView.context.getString(R.string.not_provided)
                }

                // Set joined date
                val joinedDate = member.joinedDate?.let { date ->
                    itemView.context.getString(
                        R.string.joined_date,
                        dateFormat.format(date)
                    )
                } ?: itemView.context.getString(R.string.not_available)
                joinedDateTextView.text = joinedDate

                // Load member avatar
                Glide.with(itemView)
                    .load(R.drawable.ic_person)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .placeholder(R.drawable.ic_person)
                    .into(avatarImageView)

                // Populate mentor chips
                mentorsChipGroup.removeAllViews()
                if (member.mentorNames.isNotEmpty()) {
                    mentorsChipGroup.visibility = View.VISIBLE
                    member.mentorNames.forEach { name ->
                        val chip = com.google.android.material.chip.Chip(itemView.context).apply {
                            text = name
                            isCheckable = false
                            isClickable = false
                            isCloseIconVisible = true
                            setChipBackgroundColorResource(com.google.android.material.R.color.m3_chip_assist_text_color)
                            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                            chipStrokeWidth = 0f
                            setOnCloseIconClickListener {
                                // Remove mentor when close icon is clicked
                                val updatedMentors = member.mentorNames.toMutableList().apply { remove(name) }
                                currentMember?.let { user ->
                                    onApplyMentors(user, updatedMentors)
                                }
                            }
                        }
                        mentorsChipGroup.addView(chip)
                    }
                    mentorNamesEditText.setText(member.mentorNames.joinToString(", "))
                } else {
                    mentorsChipGroup.visibility = View.GONE
                    mentorNamesEditText.setText("")
                }

                // Show/hide approve/reject buttons based on member approval status
                if (member.isApproved) {
                    actionButtonsContainer.visibility = View.GONE
                } else {
                    actionButtonsContainer.visibility = View.VISIBLE
                }

                // Mentor section is always visible
                mentorsSection.visibility = View.VISIBLE
            }
        }
        }
    }

    private class MemberDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }