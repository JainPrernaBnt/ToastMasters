package com.bntsoft.toastmasters.presentation.ui.vp.agenda.front

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.data.model.AbbreviationItem
import com.bntsoft.toastmasters.databinding.ItemAbbreviationBinding

class AbbreviationAdapter(
    private val onItemRemoved: ((Int) -> Unit)? = null
) : ListAdapter<AbbreviationItem, AbbreviationAdapter.AbbreviationViewHolder>(
    AbbreviationDiffCallback()
) {

    init {
        // Submit an empty list to avoid NPE in DiffUtil
        submitList(emptyList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbbreviationViewHolder {
        val binding = ItemAbbreviationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AbbreviationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AbbreviationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setEditable(isEditable: Boolean) {
        val updatedList = currentList.map { it.copy(isEditable = isEditable) }
        submitList(updatedList) {
            // Scroll to the bottom when adding a new item in edit mode
            if (isEditable && updatedList.isNotEmpty()) {
                notifyItemInserted(updatedList.size - 1)
            }
        }
    }

    fun getAbbreviationsMap(): Map<String, String> {
        return currentList
            .filter { it.abbreviation.isNotBlank() && it.meaning.isNotBlank() }
            .associate { it.abbreviation to it.meaning }
    }
    
    fun addNewItem() {
        val newList = currentList.toMutableList()
        newList.add(AbbreviationItem(id = System.currentTimeMillis().toString()))
        submitList(newList)
    }

    inner class AbbreviationViewHolder(
        private val binding: ItemAbbreviationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AbbreviationItem) {
            with(binding) {
                abbreviationEditText.setText(item.abbreviation)
                meaningEditText.setText(item.meaning)

                // Update UI based on edit mode
                abbreviationEditText.isEnabled = item.isEditable
                meaningEditText.isEnabled = item.isEditable
                removeButton.visibility = if (item.isEditable) android.view.View.VISIBLE else android.view.View.GONE

                // Update data when text changes
                abbreviationEditText.doOnTextChanged { text, _, _, _ ->
                    item.abbreviation = text?.toString() ?: ""
                }

                meaningEditText.doOnTextChanged { text, _, _, _ ->
                    item.meaning = text?.toString() ?: ""
                }

                removeButton.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemRemoved?.invoke(position)
                    }
                }
            }
        }
    }
}

class AbbreviationDiffCallback : DiffUtil.ItemCallback<AbbreviationItem>() {
    override fun areItemsTheSame(oldItem: AbbreviationItem, newItem: AbbreviationItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AbbreviationItem, newItem: AbbreviationItem): Boolean {
        return oldItem == newItem
    }
}

// Extension function for TextView to observe text changes
fun android.widget.TextView.doOnTextChanged(
    onTextChanged: (text: CharSequence?, start: Int, before: Int, count: Int) -> Unit
) {
    addTextChangedListener(object : android.text.TextWatcher {
        override fun afterTextChanged(s: android.text.Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChanged(s, start, before, count)
        }
    })
}
