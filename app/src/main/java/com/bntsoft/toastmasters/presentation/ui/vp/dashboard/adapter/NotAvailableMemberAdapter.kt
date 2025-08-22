package com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemNotAvailableMemberBinding
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.MemberResponseUiModel

class NotAvailableMemberAdapter :
    BaseMemberResponseAdapter<NotAvailableMemberAdapter.NotAvailableMemberViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotAvailableMemberViewHolder {
        val binding = ItemNotAvailableMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotAvailableMemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotAvailableMemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotAvailableMemberViewHolder(
        private val binding: ItemNotAvailableMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MemberResponseUiModel) {
            bindCommonViews(binding, item)

            itemView.setOnClickListener {
                onMemberClicked?.invoke(item.user)
            }
        }
    }
}
