package com.bntsoft.toastmasters.presentation.ui.common.leaderboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.GemMemberData
import com.bntsoft.toastmasters.databinding.ItemGemMemberBinding
import com.bntsoft.toastmasters.domain.models.WinnerCategory
import com.google.android.material.chip.Chip

class GemMemberAdapter(
    private val onMemberClick: (GemMemberData) -> Unit
) : ListAdapter<GemMemberData, GemMemberAdapter.GemMemberViewHolder>(GemMemberDiffCallback()) {
    
    private var selectedGemId: String? = null
    
    fun setSelectedGem(selectedGem: GemMemberData?) {
        selectedGemId = selectedGem?.user?.id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GemMemberViewHolder {
        val binding = ItemGemMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GemMemberViewHolder(binding, onMemberClick)
    }

    override fun onBindViewHolder(holder: GemMemberViewHolder, position: Int) {
        val memberData = getItem(position)
        val isSelected = memberData.user.id == selectedGemId
        holder.bind(memberData, isSelected)
    }

    class GemMemberViewHolder(
        private val binding: ItemGemMemberBinding,
        private val onMemberClick: (GemMemberData) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(memberData: GemMemberData, isSelected: Boolean = false) {
            with(binding) {
                memberName.text = memberData.user.name
                memberRole.text = if (isSelected) "🏆 Gem of the Month" else "Active Member"

                attendanceText.text = "${memberData.attendanceData.attendedMeetings}/${memberData.attendanceData.totalMeetings} meetings"
                attendanceProgress.progress = memberData.attendanceData.attendancePercentage.toInt()
                
                speakerCount.text = memberData.roleData.speakerCount.toString()
                evaluatorCount.text = memberData.roleData.evaluatorCount.toString()
                otherRolesCount.text = memberData.roleData.otherRolesCount.toString()
                
                setupRecentRoles(memberData.roleData.recentRoles)
                setupAwards(memberData.awards)
                setupGemHistory(memberData.gemHistory)
                
                previousGemIcon.visibility = if (memberData.gemHistory.isNotEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                
                // Highlight selected gem
                if (isSelected) {
                    root.setBackgroundResource(R.drawable.bg_selected_gem_card)
                    memberName.setTextColor(root.context.getColor(R.color.primary))
                } else {
                    root.setBackgroundResource(R.drawable.bg_card)
                    memberName.setTextColor(root.context.getColor(R.color.on_surface))
                }
                
                root.setOnClickListener {
                    onMemberClick(memberData)
                }
            }
        }

        private fun setupRecentRoles(recentRoles: List<String>) {
            binding.recentRolesChipGroup.removeAllViews()
            
            recentRoles.take(3).forEach { role ->
                val chip = Chip(binding.root.context).apply {
                    text = role
                    textSize = 12f
                    chipStrokeWidth = 0F
                    isClickable = false
                    isFocusable = false
                    
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
                    
                    setChipBackgroundColorResource(bgColor)
                    setTextColor(context.getColor(textColor))
                }
                binding.recentRolesChipGroup.addView(chip)
            }
        }

        private fun setupAwards(awards: List<GemMemberData.Award>) {
            if (awards.isEmpty()) {
                binding.awardsSection.visibility = View.GONE
                return
            }
            
            binding.awardsSection.visibility = View.VISIBLE
            binding.awardsContainer.removeAllViews()
            
            awards.forEach { award ->
                val chip = Chip(binding.root.context).apply {
                    text = getAwardDisplayText(award.category)
                    textSize = 12f
                    chipStrokeWidth = 0F
                    isClickable = false
                    isFocusable = false
                    
                    chipBackgroundColor = when (award.category) {
                        WinnerCategory.BEST_SPEAKER -> 
                            binding.root.context.getColorStateList(R.color.yellow_500)
                        WinnerCategory.BEST_EVALUATOR -> 
                            binding.root.context.getColorStateList(R.color.success)
                        WinnerCategory.BEST_TABLE_TOPICS -> 
                            binding.root.context.getColorStateList(R.color.blue_500)
                        else -> 
                            binding.root.context.getColorStateList(R.color.orange_500)
                    }
                }
                binding.awardsContainer.addView(chip)
            }
        }

        private fun setupGemHistory(gemHistory: List<String>) {
            if (gemHistory.isEmpty()) {
                binding.gemHistorySection.visibility = View.GONE
                return
            }
            
            binding.gemHistorySection.visibility = View.VISIBLE
            val historyText = "Gem of the Month: ${gemHistory.joinToString(", ")}"
            binding.gemHistoryText.text = historyText
        }

        private fun getAwardDisplayText(category: WinnerCategory): String {
            return when (category) {
                WinnerCategory.BEST_SPEAKER -> "Best Speaker"
                WinnerCategory.BEST_EVALUATOR -> "Best Evaluator"
                WinnerCategory.BEST_TABLE_TOPICS -> "Best Table Topics"
                WinnerCategory.BEST_MAIN_ROLE -> "Best Main Role"
                WinnerCategory.BEST_AUX_ROLE -> "Best Aux Role"
            }
        }
    }

    private class GemMemberDiffCallback : DiffUtil.ItemCallback<GemMemberData>() {
        override fun areItemsTheSame(oldItem: GemMemberData, newItem: GemMemberData): Boolean {
            return oldItem.user.id == newItem.user.id
        }

        override fun areContentsTheSame(oldItem: GemMemberData, newItem: GemMemberData): Boolean {
            return oldItem == newItem
        }
    }
}
