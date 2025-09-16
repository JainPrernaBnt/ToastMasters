package com.bntsoft.toastmasters.presentation.ui.members.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.MeetingRoleItem
import com.bntsoft.toastmasters.data.model.MemberRole
import com.bntsoft.toastmasters.databinding.ItemGrammarianDetailsBinding
import com.bntsoft.toastmasters.databinding.ItemMeetingsRolesBinding
import com.bntsoft.toastmasters.databinding.ItemSpeakerDetailsBinding

class MeetingsRoleAdapter : ListAdapter<MeetingRoleItem, RecyclerView.ViewHolder>(MeetingRoleDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_MEMBER = 0
        private const val VIEW_TYPE_GRAMMARIAN = 1
        private const val VIEW_TYPE_SPEAKER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MeetingRoleItem.MemberRoleItem -> VIEW_TYPE_MEMBER
            is MeetingRoleItem.GrammarianDetails -> VIEW_TYPE_GRAMMARIAN
            is MeetingRoleItem.SpeakerDetails -> VIEW_TYPE_SPEAKER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_MEMBER -> {
                val binding = ItemMeetingsRolesBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                MemberRoleViewHolder(binding)
            }
            VIEW_TYPE_GRAMMARIAN -> {
                val binding = ItemGrammarianDetailsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                GrammarianViewHolder(binding)
            }
            VIEW_TYPE_SPEAKER -> {
                val binding = ItemSpeakerDetailsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SpeakerViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MeetingRoleItem.MemberRoleItem -> (holder as MemberRoleViewHolder).bind(item.memberRole)
            is MeetingRoleItem.GrammarianDetails -> (holder as GrammarianViewHolder).bind(item)
            is MeetingRoleItem.SpeakerDetails -> (holder as SpeakerViewHolder).bind(item)
        }
    }

    inner class MemberRoleViewHolder(
        private val binding: ItemMeetingsRolesBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(memberRole: MemberRole) {
            binding.apply {
                tvMemberName.text = memberRole.memberName
                tvMeetingRole.text = if (memberRole.roles.isNotEmpty()) {
                    "Assigned Roles: ${memberRole.roles.joinToString(", ")}"
                } else {
                    itemView.context.getString(R.string.error_loading_roles)
                }

                if (!memberRole.evaluator.isNullOrBlank()) {
                    tvEvaluator.visibility = View.VISIBLE
                    tvEvaluator.text =
                        "Evaluator: ${memberRole.evaluator} (${memberRole.evaluatorRole ?: ""})"
                } else {
                    tvEvaluator.visibility = View.GONE
                }
            }
        }
    }

    inner class GrammarianViewHolder(
        private val binding: ItemGrammarianDetailsBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(grammarian: MeetingRoleItem.GrammarianDetails) {
            binding.apply {
                tvWordOfTheDay.text = grammarian.wordOfTheDay
                tvWordMeaning.text = grammarian.wordMeaning.joinToString("\n")
                tvWordExamples.text = grammarian.wordExamples.joinToString("\n\n") { "\"$it\"" }
                tvIdiomOfTheDay.text = grammarian.idiomOfTheDay
                tvIdiomMeaning.text = grammarian.idiomMeaning
                tvIdiomExamples.text = grammarian.idiomExamples.joinToString("\n\n") { "\"$it\"" }
            }
        }
    }

    inner class SpeakerViewHolder(
        private val binding: ItemSpeakerDetailsBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(speaker: MeetingRoleItem.SpeakerDetails) {
            binding.apply {
                tvSpeechTitle.text = speaker.speechTitle
                tvLevel.text = speaker.level.toString()
                tvPathwaysTrack.text = speaker.pathwaysTrack
                tvProjectNumber.text = speaker.projectNumber.toString()
                tvProjectTitle.text = speaker.projectTitle
                tvSpeechObjectives.text = speaker.speechObjectives
                tvSpeechTime.text = "${speaker.speechTime} minutes"
            }
        }
    }
}

class MeetingRoleDiffCallback : DiffUtil.ItemCallback<MeetingRoleItem>() {
    override fun areItemsTheSame(oldItem: MeetingRoleItem, newItem: MeetingRoleItem): Boolean {
        return when {
            oldItem is MeetingRoleItem.MemberRoleItem && newItem is MeetingRoleItem.MemberRoleItem -> 
                oldItem.memberRole.id == newItem.memberRole.id
            oldItem is MeetingRoleItem.GrammarianDetails && newItem is MeetingRoleItem.GrammarianDetails -> 
                oldItem.wordOfTheDay == newItem.wordOfTheDay && oldItem.idiomOfTheDay == newItem.idiomOfTheDay
            oldItem is MeetingRoleItem.SpeakerDetails && newItem is MeetingRoleItem.SpeakerDetails -> 
                oldItem.speechTitle == newItem.speechTitle && oldItem.speechTime == newItem.speechTime
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: MeetingRoleItem, newItem: MeetingRoleItem): Boolean {
        return oldItem == newItem
    }
}
