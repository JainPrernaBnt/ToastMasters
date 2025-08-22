package com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemAvailableMemberBinding
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.MemberResponseUiModel

class AvailableMemberAdapter :
    BaseMemberResponseAdapter<AvailableMemberAdapter.AvailableMemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvailableMemberViewHolder {
        val binding = ItemAvailableMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AvailableMemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AvailableMemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AvailableMemberViewHolder(
        private val binding: ItemAvailableMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MemberResponseUiModel) {
            bindCommonViews(binding, item)

            itemView.setOnClickListener {
                onMemberClicked?.invoke(item.user)
            }
        }
    }
}
