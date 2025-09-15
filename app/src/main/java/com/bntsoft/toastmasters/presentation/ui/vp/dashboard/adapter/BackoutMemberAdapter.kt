package com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter

import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.MemberResponseUiModel

class BackoutMemberAdapter: BaseMemberResponseAdapter<BackoutMemberAdapter.BackoutMemberViewHolder>() {

    override fun onCreateViewHolder(
        parent: android.view.ViewGroup,
        viewType: Int
    ): BackoutMemberViewHolder {
        val binding = com.bntsoft.toastmasters.databinding.ItemBackoutBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BackoutMemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BackoutMemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BackoutMemberViewHolder(
        private val binding: com.bntsoft.toastmasters.databinding.ItemBackoutBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MemberResponseUiModel) {
            bindCommonViews(binding, item)

            itemView.setOnClickListener {
                onMemberClicked?.invoke(item.user)
            }
        }
    }
}