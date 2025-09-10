package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
import com.bntsoft.toastmasters.databinding.ItemAgendaBinding
import java.util.Collections

class AgendaAdapter(
    private val onItemClick: (AgendaItemDto) -> Unit,
    private val onItemDelete: (AgendaItemDto) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<AgendaAdapter.AgendaItemViewHolder>(), ItemTouchHelperAdapter {

    private val items = mutableListOf<AgendaItemDto>()
    private var dragStartListener: ((RecyclerView.ViewHolder) -> Unit)? = null

    fun setOnStartDragListener(listener: (RecyclerView.ViewHolder) -> Unit) {
        this.dragStartListener = listener
    }

    fun submitList(newItems: List<AgendaItemDto>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<AgendaItemDto> = items.toList()

    fun addItem(item: AgendaItemDto) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun updateItem(updatedItem: AgendaItemDto) {
        val position = items.indexOfFirst { it.id == updatedItem.id }
        if (position != -1) {
            items[position] = updatedItem
            notifyItemChanged(position)
        }
    }

    fun removeItem(item: AgendaItemDto) {
        val position = items.indexOfFirst { it.id == item.id }
        if (position != -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
            // Notify items after the removed position to update their positions
            if (position < items.size) {
                notifyItemRangeChanged(position, items.size - position)
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
        Log.d("AgendaAdapter", "onBindViewHolder called for position $position")
        val item = items.getOrNull(position)
        if (item != null) {
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

    override fun getItemCount(): Int = items.size

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < 0 || fromPosition >= items.size || toPosition < 0 || toPosition >= items.size) {
            return false
        }

        // Update the order indices
        val fromItem = items[fromPosition]
        val toItem = items[toPosition]
        val newFromIndex = toItem.orderIndex

        // Update the order indices of affected items
        if (fromPosition < toPosition) {
            for (i in fromPosition + 1..toPosition) {
                items[i] = items[i].copy(orderIndex = items[i].orderIndex - 1)
            }
        } else {
            for (i in toPosition until fromPosition) {
                items[i] = items[i].copy(orderIndex = items[i].orderIndex + 1)
            }
        }

        // Update the moved item's order index
        items[toPosition] = fromItem.copy(orderIndex = newFromIndex)

        // Swap the items in the list
        Collections.swap(items, fromPosition, toPosition)

        // Notify the adapter of the move
        notifyItemMoved(fromPosition, toPosition)

        // Notify item range changed to update positions in the UI
        val start = minOf(fromPosition, toPosition)
        val end = maxOf(fromPosition, toPosition)
        notifyItemRangeChanged(start, end - start + 1)

        return true
    }

    override fun onItemDismiss(position: Int) {
        if (position < 0 || position >= items.size) return

        val item = items[position]
        onItemDelete(item)
        items.removeAt(position)
        notifyItemRemoved(position)

        // Update positions of remaining items
        if (position < items.size) {
            notifyItemRangeChanged(position, items.size - position)
        }
    }

    inner class AgendaItemViewHolder(
        private val binding: ItemAgendaBinding
    ) : RecyclerView.ViewHolder(binding.root), View.OnTouchListener {

        private var currentItem: AgendaItemDto? = null

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < items.size) {
                    onItemClick(items[position])
                }
            }

            binding.deleteButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < items.size) {
                    onItemDelete(items[position])
                }
            }

        }

        fun bind(item: AgendaItemDto) {
            currentItem = item
            binding.apply {
                // Set time, default to empty string if null
                tvTime.text = item.time ?: ""

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

// This class is now defined in the domain model
