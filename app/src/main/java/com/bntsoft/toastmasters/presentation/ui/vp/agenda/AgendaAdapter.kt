package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.ItemAgendaBinding

class AgendaAdapter(
    private val onEditClick: (Int) -> Unit = {},
    private val onDeleteClick: (Int) -> Unit = {}
) : RecyclerView.Adapter<AgendaAdapter.AgendaViewHolder>(), ItemTouchHelperAdapter {

    private val items = mutableListOf<AgendaItem>()
    private var onStartDragListener: OnStartDragListener? = null

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

    override fun onBindViewHolder(holder: AgendaViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty()) {
            Log.d(
                "AgendaAdapter",
                "onBindViewHolder with payloads: $payloads for position $position"
            )
            // If we have payloads, we can do a partial update
            val item = items.getOrNull(position)
            if (item != null) {
                holder.bind(item, position)
            } else {
                Log.e("AgendaAdapter", "Item at position $position is null")
            }
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onBindViewHolder(holder: AgendaViewHolder, position: Int) {
        Log.d("AgendaAdapter", "onBindViewHolder called for position $position")
        val item = items.getOrNull(position)
        if (item != null) {
            holder.bind(item, position)
        } else {
            Log.e("AgendaAdapter", "Item at position $position is null")
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: AgendaItem) {
        Log.d("AgendaAdapter", "Adding item: $item")
        items.add(item)
        Log.d("AgendaAdapter", "Items after add: $items")
        notifyItemInserted(items.size - 1)
    }

    fun removeItemAtPosition(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateItemAtPosition(item: AgendaItem, position: Int) {
        Log.d("AgendaAdapter", "updateItemAtPosition called with position: $position, item: $item")
        if (position in items.indices) {
            val oldItem = items[position]
            Log.d("AgendaAdapter", "Old item at position $position: $oldItem")
            Log.d("AgendaAdapter", "New item to set: $item")

            items[position] = item
            Log.d("AgendaAdapter", "Items after update: $items")

            // Use notifyItemChanged with payload to force a rebind
            notifyItemChanged(position, "UPDATE")
            Log.d("AgendaAdapter", "notifyItemChanged called for position $position")
        } else {
            Log.e(
                "AgendaAdapter",
                "Invalid position $position for update. Items size: ${items.size}"
            )
        }
    }

    fun getItemAtPosition(position: Int): AgendaItem = items[position]

    fun getItems(): List<AgendaItem> = items.toList()


    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= items.size || toPosition >= items.size) {
            return false
        }

        val fromItem = items[fromPosition]
        items.removeAt(fromPosition)
        items.add(toPosition, fromItem)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onItemDismiss(position: Int) {
        removeItemAtPosition(position)
    }

    inner class AgendaViewHolder(
        private val binding: ItemAgendaBinding
    ) : RecyclerView.ViewHolder(binding.root), ItemTouchHelperViewHolder {

        @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
        fun bind(item: AgendaItem, position: Int) {
            Log.d("AgendaAdapter", "Binding item at position $position: $item")
            try {
                binding.apply {
                    // Set item data with null checks
                    tvTime.text = "Time: ${item.time}"
                    tvActivity.text = "Activity: ${item.activity}"
                    tvPresenter.text = "Presenter: ${item.presenter}"

                    // Format and set card times
                    fun formatTime(minutes: Int): String {
                        return if (minutes <= 0) "0 min"
                        else if (minutes < 60) "$minutes min"
                        else "${minutes / 60}:${String.format("%02d", minutes % 60)}"
                    }

                    // Update time indicators
                    tvGreenTime.text = "Green: ${formatTime(item.greenTime)}"
                    tvYellowTime.text = "Yellow: ${formatTime(item.yellowTime)}"
                    tvRedTime.text = "Red: ${formatTime(item.redTime)}"

                    // Request layout update
                    root.requestLayout()

                    // Set up drag handle
                    dragHandle?.setOnTouchListener { _, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                            onStartDragListener?.onStartDrag(this@AgendaViewHolder)
                        }
                        false
                    }

                    // Set up edit button
                    EditButton?.setOnClickListener {
                        val pos = bindingAdapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            Log.d("AgendaAdapter", "Edit clicked at position: $pos")
                            onEditClick(pos)
                        }
                    }

                    // Set up delete button
                    deleteButton?.setOnClickListener {
                        val pos = bindingAdapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            Log.d("AgendaAdapter", "Delete clicked at position: $pos")
                            onDeleteClick(pos)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AgendaAdapter", "Error binding item at position $position", e)
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
    val meetingId: String = "",
    val orderIndex: Int = 0,
    val time: String = "",
    val activity: String = "",
    val presenter: String = "",
    val greenTime: Int = 0,
    val yellowTime: Int = 0,
    val redTime: Int = 0
)
