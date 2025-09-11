package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
import com.bntsoft.toastmasters.databinding.ItemAgendaBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val VIEW_TYPE_ITEM = 0
const val VIEW_TYPE_ADD_BUTTON = 1

class AgendaAdapter(
    private val onItemClick: (AgendaItemDto) -> Unit,
    private val onItemDelete: (AgendaItemDto) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onItemsUpdated: (List<AgendaItemDto>) -> Unit = {},
    private var meetingStartTime: String? = null,
    private val onItemMove: (Int, Int) -> Boolean = { _, _ -> false },
    private val onAddItemClick: () -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemTouchHelperAdapter {

    private val _items = mutableListOf<AgendaItemDto>()

    private var dragStartListener: ((RecyclerView.ViewHolder) -> Unit)? = null

    fun setOnStartDragListener(listener: (RecyclerView.ViewHolder) -> Unit) {
        this.dragStartListener = listener
    }

    fun updateMeetingStartTime(startTime: String?) {
        this.meetingStartTime = startTime
    }

    fun submitList(newItems: List<AgendaItemDto>) {
        Log.d("AgendaAdapter", "submitList called with ${newItems.size} items")
        _items.clear()
        _items.addAll(newItems.sortedBy { it.orderIndex })
        Log.d("AgendaAdapter", "Items after update: ${_items.size}")
        notifyDataSetChanged()
        Log.d("AgendaAdapter", "notifyDataSetChanged called")
    }

    fun getItems(): List<AgendaItemDto> = _items.toList()

    fun getCurrentList(): List<AgendaItemDto> = _items.toList()

    fun getItemAt(position: Int): AgendaItemDto? {
        return if (position >= 0 && position < _items.size) {
            _items[position]
        } else {
            null
        }
    }

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

    override fun getItemViewType(position: Int): Int {
        return if (position == _items.size) {
            // Last position is always the add button
            VIEW_TYPE_ADD_BUTTON
        } else {
            // Check if this is a session header
            val item = _items[position]
            if (item.isSessionHeader) {
                VIEW_TYPE_ITEM // We'll handle the header in the ViewHolder
            } else {
                VIEW_TYPE_ITEM
            }
        }
    }

    override fun getItemCount(): Int = _items.size + 1 // +1 for the add button

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> {
                val binding = ItemAgendaBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AgendaItemViewHolder(binding)
            }

            VIEW_TYPE_ADD_BUTTON -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_add_agenda_button, parent, false)
                AddButtonViewHolder(view)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AgendaItemViewHolder -> {
                if (position < _items.size) {
                    val item = _items[position]
                    Log.d(
                        "AgendaAdapter",
                        "Binding item at $position: ${item.activity} (order: ${item.orderIndex})"
                    )
                    holder.bind(item)

                    // Set click listener only for non-header items
                    if (!item.isSessionHeader) {
                        holder.itemView.setOnClickListener {
                            onItemClick(item)
                        }
                        holder.itemView.isClickable = true
                    } else {
                        holder.itemView.isClickable = false
                    }
                }
            }

            is AddButtonViewHolder -> {
                // Click handling is now in the ViewHolder's init block
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            Log.d(
                "AgendaAdapter",
                "onBindViewHolder with payloads: $payloads for position $position"
            )
            onBindViewHolder(holder, position)
        }
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
                // For items after the first, add the previous item's red time (convert from seconds to minutes)
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

    inner class AddButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val addButton: Button = view.findViewById(R.id.addItemButton)
        
        init {
            addButton.setOnClickListener { onAddItemClick() }
            addButton.setOnLongClickListener { false }
        }
    }

    inner class AgendaItemViewHolder(
        private val binding: ItemAgendaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AgendaItemDto) {
            binding.apply {
                if (item.isSessionHeader) {
                    // Show session header layout and hide normal item layout
                    normalItemLayout.visibility = View.GONE
                    sessionHeaderLayout.visibility = View.VISIBLE
                    tvSessionTitle.text = item.activity ?: ""

                    // Make the entire item not clickable for session headers
                    itemView.isClickable = false
                    itemView.isFocusable = false
                    itemView.isLongClickable = false
                    itemView.background = null
                } else {
                    // Show normal item layout and hide session header
                    normalItemLayout.visibility = View.VISIBLE
                    sessionHeaderLayout.visibility = View.GONE

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
                    }

                    item.yellowTime?.let { seconds ->
                        val minutes = seconds / 60
                        val remainingSeconds = seconds % 60
                        tvYellowTime.text = if (remainingSeconds > 0) {
                            String.format("%d:%02d", minutes, remainingSeconds)
                        } else {
                            "$minutes min"
                        }
                        tvYellowTime.visibility = View.VISIBLE
                    }

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
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= _items.size || toPosition >= _items.size) {
            return false
        }

        val from = _items[fromPosition]
        val to = _items[toPosition]

        // Swap the items
        _items[fromPosition] = to
        _items[toPosition] = from

        // Update order indices
        _items[fromPosition].orderIndex = fromPosition
        _items[toPosition].orderIndex = toPosition

        notifyItemMoved(fromPosition, toPosition)
        onItemsUpdated(_items)
        return true
    }

    override fun onItemDismiss(position: Int): Boolean {
        if (position < 0 || position >= _items.size) return false
        val item = _items[position]
        _items.removeAt(position)
        notifyItemRemoved(position)
        onItemDelete(item)
        return true
    }

    override fun canDrag(position: Int): Boolean {
        // Don't allow dragging the add button or session headers
        return position < _items.size && !_items[position].isSessionHeader
    }

    override fun canSwipe(position: Int): Boolean {
        // Don't allow swiping the add button or session headers
        return position < _items.size && !_items[position].isSessionHeader
    }
}
interface ItemTouchHelperAdapter {
    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean
    fun onItemDismiss(position: Int): Boolean
    fun canDrag(position: Int): Boolean
    fun canSwipe(position: Int): Boolean
}

interface ItemTouchHelperViewHolder {
    fun onItemSelected()
    fun onItemClear()
}

interface OnStartDragListener {
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
}


