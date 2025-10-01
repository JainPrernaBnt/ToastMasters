package com.bntsoft.toastmasters.presentation.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.google.android.material.chip.Chip

class SelectedMentorsAdapter(
    private val onMentorRemoved: (String) -> Unit
) : RecyclerView.Adapter<SelectedMentorsAdapter.MentorViewHolder>() {

    private val selectedMentors = mutableListOf<String>()

    fun updateMentors(mentors: List<String>) {
        selectedMentors.clear()
        selectedMentors.addAll(mentors)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MentorViewHolder {
        val chip = Chip(parent.context).apply {
            isCloseIconVisible = true
            setOnCloseIconClickListener { 
                val position = tag as Int
                if (position < selectedMentors.size) {
                    onMentorRemoved(selectedMentors[position])
                }
            }
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 4, 8, 4)
            }
            layoutParams = params
        }
        return MentorViewHolder(chip)
    }

    override fun onBindViewHolder(holder: MentorViewHolder, position: Int) {
        val mentor = selectedMentors[position]
        holder.chip.text = mentor
        holder.chip.tag = position
    }

    override fun getItemCount(): Int = selectedMentors.size

    class MentorViewHolder(val chip: Chip) : RecyclerView.ViewHolder(chip)
}
