package com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemAvailableMemberBinding
import com.bntsoft.toastmasters.databinding.ItemBackoutBinding
import com.bntsoft.toastmasters.databinding.ItemNotAvailableMemberBinding
import com.bntsoft.toastmasters.databinding.ItemNotConfirmedMemberBinding
import com.bntsoft.toastmasters.databinding.ItemNotRespondedMemberBinding
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.MemberResponseUiModel

abstract class BaseMemberResponseAdapter<T : RecyclerView.ViewHolder> :
    ListAdapter<MemberResponseUiModel, T>(MemberResponseDiffCallback()) {

    protected var onMemberClicked: ((User) -> Unit)? = null

    fun setOnMemberClickListener(listener: (User) -> Unit) {
        onMemberClicked = listener
    }

    class MemberResponseDiffCallback : DiffUtil.ItemCallback<MemberResponseUiModel>() {
        override fun areItemsTheSame(
            oldItem: MemberResponseUiModel,
            newItem: MemberResponseUiModel
        ): Boolean = oldItem.user.id == newItem.user.id

        override fun areContentsTheSame(
            oldItem: MemberResponseUiModel,
            newItem: MemberResponseUiModel
        ): Boolean = oldItem == newItem
    }

    protected fun bindCommonViews(
        binding: ItemAvailableMemberBinding,
        item: MemberResponseUiModel
    ) {
        binding.tvMemberName.text = item.user.name

        // Clear any existing chips
        binding.chipGroupPreferredRoles.removeAllViews()

        // Show or hide the preferred roles section based on availability of roles
        if (item.response?.preferredRoles?.isNotEmpty() == true) {
            var hasValidRoles = false

            // Add a chip for each preferred role
            item.response.preferredRoles.forEach { role ->
                if (role.isNotBlank()) {
                    hasValidRoles = true
                    val chip = com.google.android.material.chip.Chip(binding.root.context).apply {
                        text = role.trim()
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
                        setTextColor(binding.root.context.getColor(android.R.color.white))
                        chipBackgroundColor = binding.root.context.getColorStateList(
                            com.bntsoft.toastmasters.R.color.purple_500
                        )
                        chipMinHeight = binding.root.context.resources.getDimensionPixelSize(
                            com.bntsoft.toastmasters.R.dimen.chip_min_height
                        ).toFloat()
                        isClickable = false
                        setPaddingRelative(
                            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp),
                            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp),
                            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._4sdp)
                        )
                    }
                    binding.chipGroupPreferredRoles.addView(chip)
                }
            }

            // Update visibility based on whether we have any valid roles
            if (hasValidRoles) {
                binding.tvPreferredRolesLabel.visibility = View.VISIBLE
                binding.chipGroupPreferredRoles.visibility = View.VISIBLE
            } else {
                binding.tvPreferredRolesLabel.visibility = View.GONE
                binding.chipGroupPreferredRoles.visibility = View.GONE
            }
        } else {
            binding.tvPreferredRolesLabel.visibility = View.GONE
            binding.chipGroupPreferredRoles.visibility = View.GONE
        }
    }

    protected fun bindCommonViews(
        binding: ItemNotAvailableMemberBinding,
        item: MemberResponseUiModel
    ) {
        binding.tvMemberName.text = item.user.name
    }

    protected fun bindCommonViews(
        binding: ItemNotConfirmedMemberBinding,
        item: MemberResponseUiModel
    ) {
        binding.tvMemberName.text = item.user.name
    }

    protected fun bindCommonViews(
        binding: ItemNotRespondedMemberBinding,
        item: MemberResponseUiModel
    ) {
        binding.tvMemberName.text = item.user.name
    }

    protected fun bindCommonViews(
        binding: ItemBackoutBinding,
        item: MemberResponseUiModel
    ) {
        binding.tvMemberName.text = item.user.name
    }
}
