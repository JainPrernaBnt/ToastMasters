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
    private val onAddItemClick: () -> Unit = {},
    private var isVpEducation: Boolean = true
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemTouchHelperAdapter {

    private val _items = mutableListOf<AgendaItemDto>()

    private var dragStartListener: ((RecyclerView.ViewHolder) -> Unit)? = null

    fun setOnStartDragListener(listener: (RecyclerView.ViewHolder) -> Unit) {
        this.dragStartListener = listener
    }

    fun updateMeetingStartTime(startTime: String?) {
        this.meetingStartTime = startTime
    }

    fun updateUserRole(isVpEducation: Boolean) {
        this.isVpEducation = isVpEducation
        notifyDataSetChanged() // Refresh to show/hide add button
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
        return if (position == _items.size && isVpEducation) {
            // Last position is the add button only for VP Education
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

    override fun getItemCount(): Int = _items.size + if (isVpEducation) 1 else 0 // +1 for the add button only for VP Education

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

                    // Set click listeners for draggable items
                    if (viewType == VIEW_TYPE_ITEM ||
                        viewType == VIEW_TYPE_TIME_BREAK ||
                        viewType == VIEW_TYPE_SESSION) {

                        // Click listener only for normal items
                        if (viewType == VIEW_TYPE_ITEM) {
                            holder.itemView.setOnClickListener {
                                onItemClick(item)
                            }
                        } else {
                            holder.itemView.setOnClickListener(null)
                        }

                        holder.itemView.setOnLongClickListener {
                            if (isVpEducation && canDrag(position)) {
                                onStartDrag(holder)
                            }
                            true
                        }

                    } else {
                        // For ADD button or other view types
                        holder.itemView.setOnClickListener(null)
                        holder.itemView.setOnLongClickListener(null)
                    }

                }
            }

            is AddButtonViewHolder -> {
                // Handle add button click (only for VP Education)
                if (isVpEducation) {
                    holder.binding.btnAddItem.setOnClickListener {
                        onAddItemClick()
                    }
                } else {
                    holder.binding.btnAddItem.setOnClickListener(null)
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
                // Always allow long clicks for drag functionality
                itemView.isLongClickable = true
            }
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition == toPosition) return false

        val items = _items.toMutableList()
        val movingItem = items.removeAt(fromPosition)
        items.add(toPosition, movingItem)

        // Update orderIndex
        items.forEachIndexed { index, item ->
            item.orderIndex = index
        }

        _items.clear()
        _items.addAll(items)
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
        // Allow dragging normal items, time breaks, and sessions (not the add button)
        val canDrag = position < _items.size &&
                (getItemViewType(position) == VIEW_TYPE_ITEM || 
                 getItemViewType(position) == VIEW_TYPE_TIME_BREAK || 
                 getItemViewType(position) == VIEW_TYPE_SESSION)
        Log.d("AgendaAdapter", "canDrag($position): $canDrag, viewType=${getItemViewType(position)}, isVpEducation=$isVpEducation")
        return canDrag
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
