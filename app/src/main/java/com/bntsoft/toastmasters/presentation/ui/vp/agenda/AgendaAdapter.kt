package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
import com.bntsoft.toastmasters.databinding.ItemAgendaBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgendaAdapter(
    private val onItemClick: (AgendaItemDto) -> Unit,
    private val onItemDelete: (AgendaItemDto) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onItemsUpdated: (List<AgendaItemDto>) -> Unit = {}
) : RecyclerView.Adapter<AgendaAdapter.AgendaItemViewHolder>(), ItemTouchHelperAdapter {

    private val _items = mutableListOf<AgendaItemDto>()
    val currentList: List<AgendaItemDto> get() = _items.toList()
    private var dragStartListener: ((RecyclerView.ViewHolder) -> Unit)? = null

    fun setOnStartDragListener(listener: (RecyclerView.ViewHolder) -> Unit) {
        this.dragStartListener = listener
    }

    fun submitList(newItems: List<AgendaItemDto>) {
        Log.d("AgendaAdapter", "submitList called with ${newItems.size} items")
        _items.clear()
        _items.addAll(newItems)
        Log.d("AgendaAdapter", "Items after update: ${_items.size}")
        notifyDataSetChanged()
        Log.d("AgendaAdapter", "notifyDataSetChanged called")
    }

    fun getItems(): List<AgendaItemDto> = _items.toList()

    fun addItem(item: AgendaItemDto) {
        _items.add(item)
        notifyItemInserted(_items.size - 1)
    }

    fun updateItem(updatedItem: AgendaItemDto) {
        val position = _items.indexOfFirst { it.id == updatedItem.id }
        if (position != -1) {
            _items[position] = updatedItem
            notifyItemChanged(position)
        }
    }

    fun removeItem(item: AgendaItemDto) {
        val position = _items.indexOfFirst { it.id == item.id }
        if (position != -1) {
            _items.removeAt(position)
            notifyItemRemoved(position)
            // Notify items after the removed position to update their positions
            if (position < _items.size) {
                notifyItemRangeChanged(position, _items.size - position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendaItemViewHolder {
        val binding = ItemAgendaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AgendaItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AgendaItemViewHolder, position: Int) {
        Log.d(
            "AgendaAdapter",
            "onBindViewHolder called for position $position, itemCount: ${_items.size}"
        )
        val item = _items.getOrNull(position)

        if (item != null) {
            Log.d(
                "AgendaAdapter",
                "Binding item at $position: ${item.activity} (order: ${item.orderIndex})"
            )
            holder.bind(item)
        } else {
            Log.e("AgendaAdapter", "Item at position $position is null")
        }
    }

    override fun onBindViewHolder(
        holder: AgendaItemViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.isNotEmpty()) {
            Log.d(
                "AgendaAdapter",
                "onBindViewHolder with payloads: $payloads for position $position"
            )
        }
        onBindViewHolder(holder, position)
    }

    override fun getItemCount(): Int = _items.size

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Log.d("AgendaAdapter", "onItemMove from $fromPosition to $toPosition")
        
        // Get the item being moved
        val movedItem = _items[fromPosition]
        
        // Remove the item from its original position
        _items.removeAt(fromPosition)
        // Insert it at the new position
        _items.add(toPosition, movedItem)
        
        // Update order indices to reflect the new positions
        _items.forEachIndexed { index, item ->
            _items[index] = item.copy(orderIndex = index)
        }
        
        // Recalculate times starting from the first item
        recalculateTimes()
        
        // Notify the adapter of the move
        notifyItemMoved(fromPosition, toPosition)
        
        // Notify all items to update their times
        notifyItemRangeChanged(0, _items.size)
        
        // Notify listener that items were updated
        onItemsUpdated(_items)
        
        return true
    }

    private fun recalculateTimes() {
        if (_items.isEmpty()) return
        
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        // Use the first item's time if it exists, otherwise use current time
        val firstTime = if (_items[0].time.isNotBlank()) {
            timeFormat.parse(_items[0].time) ?: Calendar.getInstance().time
        } else {
            Calendar.getInstance().time
        }
        
        // Update the first item's time
        _items[0] = _items[0].copy(time = timeFormat.format(firstTime))
        
        // Start with the first item's time
        calendar.time = firstTime
        
        // Recalculate times for all items
        for (i in 0 until _items.size) {
            if (i > 0) {
                // For items after the first, add the previous item's red time
                val prevItem = _items[i - 1]
                calendar.add(Calendar.MINUTE, (prevItem.redTime / 60).coerceAtLeast(0))
                
                // Update current item's time
                _items[i] = _items[i].copy(time = timeFormat.format(calendar.time))
            }
            
            Log.d("AgendaAdapter", "Recalculated time for item $i: ${_items[i].time}")
        }
        
        // Notify the adapter that data has changed
        notifyDataSetChanged()
    }

    override fun onItemDismiss(position: Int) {
        if (position < 0 || position >= _items.size) return

        val item = _items[position]
        onItemDelete(item)
        _items.removeAt(position)
        notifyItemRemoved(position)

        // Recalculate times for remaining items
        if (_items.isNotEmpty()) {
            recalculateTimes()
            notifyItemRangeChanged(0, _items.size)
            // Notify listener that items were updated
            onItemsUpdated(_items)
        }
    }


    inner class AgendaItemViewHolder(
        private val binding: ItemAgendaBinding
    ) : RecyclerView.ViewHolder(binding.root), View.OnTouchListener {


        private var currentItem: AgendaItemDto? = null

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < _items.size) {
                    onItemClick(_items[position])
                }
            }

        }

        fun bind(item: AgendaItemDto) {
            currentItem = item
            binding.apply {
                // Set time, default to empty string if null
                tvTime.text = item.time ?: ""

                // Make time text bold for better visibility
                tvTime.setTypeface(tvTime.typeface, android.graphics.Typeface.BOLD)

                // Set activity, default to empty string if null
                tvActivity.text = item.activity ?: ""

                // Set presenter name, default to empty string if null
                etPresenter.setText(item.presenterName ?: "")

                // Format and set card times if available, otherwise hide them
                item.greenTime?.let { seconds ->
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60
                    tvGreenTime.text = if (remainingSeconds > 0) {
                        String.format("%d:%02d", minutes, remainingSeconds)
                    } else {
                        "$minutes min"
                    }
                    tvGreenTime.visibility = View.VISIBLE
                } ?: run { tvGreenTime.visibility = View.GONE }

                item.yellowTime?.let { seconds ->
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60
                    tvYellowTime.text = if (remainingSeconds > 0) {
                        String.format("%d:%02d", minutes, remainingSeconds)
                    } else {
                        "$minutes min"
                    }
                    tvYellowTime.visibility = View.VISIBLE
                } ?: run { tvYellowTime.visibility = View.GONE }

                item.redTime?.let { seconds ->
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60
                    tvRedTime.text = if (remainingSeconds > 0) {
                        String.format("%d:%02d", minutes, remainingSeconds)
                    } else {
                        "$minutes min"
                    }
                    tvRedTime.visibility = View.VISIBLE
                } ?: run { tvRedTime.visibility = View.GONE }

                // Set position tag for drag and drop
                itemView.tag = item.orderIndex
            }
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                dragStartListener?.invoke(this)
            }
            return false
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

