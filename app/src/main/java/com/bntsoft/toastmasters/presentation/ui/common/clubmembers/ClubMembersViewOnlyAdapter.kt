package com.bntsoft.toastmasters.presentation.ui.common.clubmembers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.ItemClubMemberViewOnlyBinding
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.utils.GlideExtensions
import java.text.SimpleDateFormat
import java.util.Locale

class ClubMembersViewOnlyAdapter : ListAdapter<User, ClubMembersViewOnlyAdapter.MemberViewHolder>(MemberDiffCallback()) {

    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemClubMemberViewOnlyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(
        private val binding: ItemClubMemberViewOnlyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: User) {
            binding.apply {
                GlideExtensions.loadProfilePicture(
                    profileImageView,
                    member.profilePictureUrl,
                    R.drawable.ic_person
                )
                
                tvMemberName.text = member.name.ifEmpty { "N/A" }
                tvMemberEmail.text = member.email
                tvMemberPhone.text = member.phoneNumber.ifEmpty { "N/A" }
                tvMemberRole.text = when (member.role.name) {
                    "VP_EDUCATION" -> "VP Education"
                    "MEMBER" -> "Member"
                    else -> member.role.name.replace("_", " ")
                }
                tvMemberAddress.text = member.address.ifEmpty { "N/A" }
                tvToastmastersId.text = member.toastmastersId.ifEmpty { "N/A" }
                
                val formattedDate = try {
                    dateFormat.format(member.joinedDate)
                } catch (e: Exception) {
                    "N/A"
                }
                tvJoinedDate.text = formattedDate
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
}
