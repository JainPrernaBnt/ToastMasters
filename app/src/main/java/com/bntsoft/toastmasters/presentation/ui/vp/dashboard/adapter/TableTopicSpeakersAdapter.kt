package com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.data.model.TableTopicSpeaker
import com.bntsoft.toastmasters.databinding.ItemTableTopicSpeakerBinding

class TableTopicSpeakersAdapter(
    private val onDeleteClick: (TableTopicSpeaker) -> Unit
) : RecyclerView.Adapter<TableTopicSpeakersAdapter.ViewHolder>() {

    private val speakers = mutableListOf<TableTopicSpeaker>()

    fun submitList(newList: List<TableTopicSpeaker>) {
        speakers.clear()
        speakers.addAll(newList)
        notifyDataSetChanged()
    }

    fun addSpeaker(speaker: TableTopicSpeaker) {
        speakers.add(speaker)
        notifyItemInserted(speakers.size - 1)
    }

    fun getSpeakers(): List<TableTopicSpeaker> = speakers.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTableTopicSpeakerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(speakers[position])
    }

    override fun getItemCount(): Int = speakers.size

    inner class ViewHolder(
        private val binding: ItemTableTopicSpeakerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(speaker: TableTopicSpeaker) {
            binding.tvSpeakerName.text = speaker.speakerName
            binding.tvTopic.text = speaker.topic
            binding.btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(speakers[position])
                    speakers.removeAt(position)
                    notifyItemRemoved(position)
                }
            }
        }
    }
}
