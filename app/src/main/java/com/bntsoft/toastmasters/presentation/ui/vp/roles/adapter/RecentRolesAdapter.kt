package com.bntsoft.toastmasters.presentation.ui.vp.roles.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecentRolesAdapter(
    private val roles: List<String>
) : RecyclerView.Adapter<RecentRolesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false) as TextView
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(roles[position])
    }

    override fun getItemCount() = roles.size

    class ViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(role: String) {
            textView.text = role
        }
    }
}
