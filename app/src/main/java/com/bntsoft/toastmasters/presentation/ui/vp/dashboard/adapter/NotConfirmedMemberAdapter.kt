package com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemNotConfirmedMemberBinding
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.MemberResponseUiModel

class NotConfirmedMemberAdapter :
    BaseMemberResponseAdapter<NotConfirmedMemberAdapter.NotConfirmedMemberViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotConfirmedMemberViewHolder {
        val binding = ItemNotConfirmedMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotConfirmedMemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotConfirmedMemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotConfirmedMemberViewHolder(
        private val binding: ItemNotConfirmedMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MemberResponseUiModel) {
            bindCommonViews(binding, item)

            itemView.setOnClickListener {
                onMemberClicked?.invoke(item.user)
            }
        }
    }
}
