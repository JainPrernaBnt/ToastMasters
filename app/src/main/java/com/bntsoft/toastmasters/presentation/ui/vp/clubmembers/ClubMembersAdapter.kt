package com.bntsoft.toastmasters.presentation.ui.vp.clubmembers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemClubMemberBinding
import com.bntsoft.toastmasters.domain.model.User
import java.text.SimpleDateFormat
import java.util.Locale

class ClubMembersAdapter(
    private val onMemberClick: (User) -> Unit
) : ListAdapter<User, ClubMembersAdapter.MemberViewHolder>(MemberDiffCallback()) {

    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemClubMemberBinding.inflate(
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
        private val binding: ItemClubMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: User) {
            binding.apply {
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

                root.setOnClickListener {
                    onMemberClick(member)
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
}
