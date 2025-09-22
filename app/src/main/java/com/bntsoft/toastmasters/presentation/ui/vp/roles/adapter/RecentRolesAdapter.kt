package com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.ItemRecentRoleBinding

class RecentRolesAdapter(
    private var roles: List<String>
) : RecyclerView.Adapter<RecentRolesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentRoleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(roles[position])
    }

    override fun getItemCount() = roles.size

    fun updateRoles(newRoles: List<String>) {
        roles = newRoles.distinct()
        notifyDataSetChanged()
    }

    class ViewHolder(
        private val binding: ItemRecentRoleBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(role: String) {
            binding.chipRole.text = role
            
            // Set different background color based on role type
            val context = binding.root.context
            val (bgColor, textColor) = when (role.lowercase()) {
                "toastmaster of the day" -> R.color.toastmaster_bg to R.color.toastmaster_text
                "speaker" -> R.color.speaker_bg to R.color.speaker_text
                "evaluator" -> R.color.evaluator_bg to R.color.evaluator_text
                "timer" -> R.color.timer_bg to R.color.timer_text
                "ah-counter" -> R.color.ah_counter_bg to R.color.ah_counter_text
                "grammarian" -> R.color.grammarian_bg to R.color.grammarian_text
                "sergeant-at-arms" -> R.color.sergeant_bg to R.color.sergeant_text
                "presiding officer" -> R.color.presiding_bg to R.color.presiding_text
                "table topics master" -> R.color.ttm_bg to R.color.ttm_text
                "table topics speaker" -> R.color.tts_bg to R.color.tts_text
                "quiz master" -> R.color.quiz_master_bg to R.color.quiz_master_text
                else -> R.color.default_role_bg to R.color.default_role_text
            }

            binding.chipRole.setChipBackgroundColorResource(bgColor)
            binding.chipRole.setTextColor(context.getColor(textColor))
        }
    }
}
