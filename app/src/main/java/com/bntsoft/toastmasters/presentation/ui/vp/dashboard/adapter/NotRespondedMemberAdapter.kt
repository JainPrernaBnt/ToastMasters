package com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemNotRespondedMemberBinding
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.ParticipantResponseUiModel

class NotRespondedMemberAdapter :
    BaseMemberResponseAdapter<NotRespondedMemberAdapter.NotRespondedMemberViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotRespondedMemberViewHolder {
        val binding = ItemNotRespondedMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotRespondedMemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotRespondedMemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotRespondedMemberViewHolder(
        private val binding: ItemNotRespondedMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ParticipantResponseUiModel) {
            bindCommonViews(binding, item)

            itemView.setOnClickListener {
                onParticipantClicked?.invoke(item)
            }
        }
    }
}
