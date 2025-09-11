package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
import com.bntsoft.toastmasters.databinding.ItemAddAgendaButtonBinding
import com.bntsoft.toastmasters.databinding.ItemAgendaBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val VIEW_TYPE_ITEM = 0
const val VIEW_TYPE_ADD_BUTTON = 1
const val VIEW_TYPE_TIME_BREAK = 2
const val VIEW_TYPE_SESSION = 3
const val PAYLOAD_ITEM_CHANGED = "item_changed"

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

        // Calculate the differences between old and new lists
        val oldItems = _items.toList()
        val sortedNewItems = newItems.sortedBy { it.orderIndex }

        if (oldItems.isEmpty()) {
            // If the list is empty, just add all new items
            _items.addAll(sortedNewItems)
            notifyItemRangeInserted(0, sortedNewItems.size)
        } else {
            // Use DiffUtil to calculate the differences
            val diffCallback = object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldItems.size
                override fun getNewListSize(): Int = sortedNewItems.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldItems[oldItemPosition].id == sortedNewItems[newItemPosition].id
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return oldItems[oldItemPosition] == sortedNewItems[newItemPosition]
                }

                override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                    return if (oldItems[oldItemPosition] != sortedNewItems[newItemPosition]) {
                        PAYLOAD_ITEM_CHANGED
                    } else {
                        null
                    }
                }
            }

            val diffResult = DiffUtil.calculateDiff(diffCallback)
            _items.clear()
            _items.addAll(sortedNewItems)
            diffResult.dispatchUpdatesTo(this)
        }

        // Always notify the add button
        notifyItemChanged(_items.size)

        Log.d("AgendaAdapter", "List updated. Total items: ${_items.size}")
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
            val item = _items[position]
            when {
                item.isSessionHeader -> VIEW_TYPE_SESSION
                item.activity?.contains("BREAK", ignoreCase = true) == true -> VIEW_TYPE_TIME_BREAK
                else -> VIEW_TYPE_ITEM
            }
        }
    }

    override fun getItemCount(): Int = _items.size + 1 // +1 for the add button

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemAgendaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return when (viewType) {
            VIEW_TYPE_ADD_BUTTON -> {
                val binding = ItemAddAgendaButtonBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AddButtonViewHolder(binding)
            }

            else -> ViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                if (position < _items.size) {
                    val item = _items[position]
                    val viewType = getItemViewType(position)
                    holder.bind(item, viewType)

                    // Only set click listeners for normal items
                    if (viewType == VIEW_TYPE_ITEM) {
                        // Set click listener for the item
                        holder.itemView.setOnClickListener {
                            onItemClick(item)
                        }

                        // Set long click listener for drag
                        holder.itemView.setOnLongClickListener {
                            if (canDrag(position)) {
                                onStartDrag(holder)
                            }
                            true
                        }
                    } else {
                        // Clear click listeners for non-normal items
                        holder.itemView.setOnClickListener(null)
                        holder.itemView.setOnLongClickListener(null)
                    }
                }
            }

            is AddButtonViewHolder -> {
                // Handle add button click
                holder.binding.btnAddItem.setOnClickListener {
                    onAddItemClick()
                }
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

            when (holder) {
                is ViewHolder -> {
                    if (position < _items.size) {
                        val item = _items[position]
                        val viewType = getItemViewType(position)

                        // Only rebind if there's a payload indicating content change
                        if (payloads.any { it == PAYLOAD_ITEM_CHANGED }) {
                            holder.bind(item, viewType)
                        }
                    }
                }
                // No special handling needed for AddButtonViewHolder with payloads
                else -> onBindViewHolder(holder, position)
            }
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

    inner class AddButtonViewHolder(
        val binding: ItemAddAgendaButtonBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnAddItem.setOnClickListener { onAddItemClick() }
            binding.btnAddItem.setOnLongClickListener { false }
        }
    }

    inner class ViewHolder(private val binding: ItemAgendaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AgendaItemDto, viewType: Int) {
            binding.apply {
                // Hide all views first
                normalItemLayout.visibility = View.GONE
                sessionHeaderLayout.visibility = View.GONE
                timeBreakLayout.visibility = View.GONE

                when (viewType) {
                    VIEW_TYPE_ITEM -> {
                        // Show normal item layout
                        normalItemLayout.visibility = View.VISIBLE

                        // Set time if available
                        tvTime.text = item.time ?: ""

                        // Set activity name
                        tvActivity.text = item.activity ?: ""

                        // Set presenter name if available
                        tvPresenter.setText(item.presenterName ?: "")

                        // Set time indicators if available
                        item.greenTime?.let {
                            tvGreenTime.text = (it / 60).toString()
                            tvGreenTime.visibility = View.VISIBLE
                        } ?: run { tvGreenTime.visibility = View.GONE }

                        item.yellowTime?.let {
                            tvYellowTime.text = (it / 60).toString()
                            tvYellowTime.visibility = View.VISIBLE
                        } ?: run { tvYellowTime.visibility = View.GONE }

                        item.redTime?.let {
                            tvRedTime.text = (it / 60).toString()
                            tvRedTime.visibility = View.VISIBLE
                        } ?: run { tvRedTime.visibility = View.GONE }
                    }

                    VIEW_TYPE_SESSION -> {
                        // Show session header layout
                        sessionHeaderLayout.visibility = View.VISIBLE
                        tvSessionTitle.text = item.activity ?: ""
                    }

                    VIEW_TYPE_TIME_BREAK -> {
                        // Show time break layout
                        timeBreakLayout.visibility = View.VISIBLE
                        tvTimeBreakText.text = item.activity ?: ""
                    }
                }

                // Set position tag for drag and drop
                itemView.tag = item.orderIndex

                // Disable click for non-normal items to prevent selection
                if (viewType != VIEW_TYPE_ITEM) {
                    itemView.isClickable = false
                    itemView.isFocusable = false
                } else {
                    itemView.isClickable = true
                    itemView.isFocusable = true
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
        // Only allow dragging normal items (not time breaks, sessions, or the add button)
        return position < _items.size &&
                getItemViewType(position) == VIEW_TYPE_ITEM
    }

    override fun canSwipe(position: Int): Boolean {
        // Don't allow swiping any items (including time breaks and sessions)
        return false
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


