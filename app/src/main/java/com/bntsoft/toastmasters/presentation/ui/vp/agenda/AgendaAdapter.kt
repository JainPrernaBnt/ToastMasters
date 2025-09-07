package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemAgendaBinding

class AgendaAdapter : RecyclerView.Adapter<AgendaAdapter.AgendaViewHolder>(),
    ItemTouchHelperAdapter {

    private val items = mutableListOf<AgendaItem>()
    private var onStartDragListener: OnStartDragListener? = null

    init {
        items.add(AgendaItem())
    }

    fun setOnStartDragListener(listener: OnStartDragListener) {
        this.onStartDragListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendaViewHolder {
        val binding = ItemAgendaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AgendaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AgendaViewHolder, position: Int) {
        holder.bind(items[position], position == 0)
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: AgendaItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun addItemAtPosition(item: AgendaItem, position: Int) {
        items.add(position, item)
        notifyItemInserted(position)
    }

    fun removeItem(position: Int) {
        if (position > 0) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        // Don't allow moving the first item
        if (fromPosition == 0 || toPosition == 0) {
            return false
        }

        // Swap the items
        val fromItem = items[fromPosition]
        items.removeAt(fromPosition)
        items.add(toPosition, fromItem)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onItemDismiss(position: Int) {
        // Not used - we handle item dismissal in removeItem
    }

    fun getItems(): List<AgendaItem> = items.toList()

    var onItemAdded: ((position: Int) -> Unit)? = null

    inner class AgendaViewHolder(
        private val binding: ItemAgendaBinding
    ) : RecyclerView.ViewHolder(binding.root), ItemTouchHelperViewHolder {

        fun bind(item: AgendaItem, isFirstItem: Boolean) {
            // Hide delete button for the first item
            binding.deleteButton.visibility = if (isFirstItem) View.GONE else View.VISIBLE

            // Set up delete button click listener
            binding.deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    removeItem(position)
                }
            }

            // Set up drag handle touch listener
            binding.dragHandle.setOnTouchListener { _, event ->
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    onStartDragListener?.onStartDrag(this)
                }
                false
            }

            // Set up add item FAB click listener
            binding.addItemFab.setOnClickListener {
                val nextPosition = adapterPosition + 1
                onItemAdded?.invoke(nextPosition)
            }
        }

        override fun onItemSelected() {
            // Add visual feedback when item is being dragged
            itemView.alpha = 0.7f
        }

        override fun onItemClear() {
            // Remove visual feedback when drag ends
            itemView.alpha = 1.0f
        }
    }
}

interface ItemTouchHelperAdapter {
    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean
    fun onItemDismiss(position: Int)
}

interface ItemTouchHelperViewHolder {
    fun onItemSelected()
    fun onItemClear()
}

interface OnStartDragListener {
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
}

data class AgendaItem(
    val id: String = "",
    val time: String = "",
    val activity: String = "",
    val presenter: String = "",
    val greenTime: Int = 0,
    val yellowTime: Int = 0,
    val redTime: Int = 0
)
