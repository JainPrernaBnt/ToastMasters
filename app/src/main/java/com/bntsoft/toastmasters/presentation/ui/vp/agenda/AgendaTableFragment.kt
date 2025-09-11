package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.app.Dialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.mapper.AgendaItemMapper
import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
import com.bntsoft.toastmasters.databinding.DialogAddItemBinding
import com.bntsoft.toastmasters.databinding.FragmentAgendaTableBinding
import com.bntsoft.toastmasters.domain.repository.MeetingRepository
import com.bntsoft.toastmasters.utils.AgendaTimeCalculator
import com.bntsoft.toastmasters.utils.Resource
import com.bntsoft.toastmasters.utils.TimeUtils
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AgendaTableFragment : Fragment() {
    @Inject
    lateinit var agendaItemMapper: AgendaItemMapper

    @Inject
    lateinit var meetingRepository: MeetingRepository

    private var _binding: FragmentAgendaTableBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AgendaTableViewModel by viewModels()
    lateinit var meetingId: String
    private lateinit var agendaAdapter: AgendaAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var meetingStartTime: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgendaTableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get and validate meetingId
        meetingId = arguments?.getString("meetingId")?.trim() ?: run {
            Log.e("AgendaTableFragment", "No meetingId provided in arguments")
            findNavController().navigateUp()
            return
        }

        if (meetingId.isBlank()) {
            Log.e("AgendaTableFragment", "Empty meetingId provided")
            findNavController().navigateUp()
            return
        }

        Log.d("AgendaTableFragment", "Initialized with meetingId: $meetingId")

        setupRecyclerView()
        setupClickListeners()
        setupObservers()

        // Load meeting start time and existing agenda items
        loadMeetingStartTime()
        viewModel.loadAgendaItems(meetingId)
    }

    private fun loadMeetingStartTime() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val meeting = meetingRepository.getMeetingById(meetingId)
                meeting?.dateTime?.let { dateTime ->
                    try {
                        // Format LocalDateTime to 12-hour format string
                        val formatter = java.time.format.DateTimeFormatter.ofPattern(
                            "hh:mm a",
                            Locale.getDefault()
                        )
                        meetingStartTime = dateTime.format(formatter)
                        Log.d("AgendaTableFragment", "Meeting start time loaded: $meetingStartTime")
                        // Update the adapter with the meeting start time
                        agendaAdapter.updateMeetingStartTime(meetingStartTime)
                    } catch (e: Exception) {
                        Log.e(
                            "AgendaTableFragment",
                            "Error formatting meeting start time: ${e.message}"
                        )
                        // Fallback to current time
                        val calendar = Calendar.getInstance()
                        meetingStartTime =
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
                        agendaAdapter.updateMeetingStartTime(meetingStartTime)
                    }
                } ?: run {
                    // Fallback to current time if no start time is set
                    val calendar = Calendar.getInstance()
                    meetingStartTime =
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
                    agendaAdapter.updateMeetingStartTime(meetingStartTime)
                }
            } catch (e: Exception) {
                Log.e("AgendaTableFragment", "Error loading meeting start time: ${e.message}")
                // Fallback to current time
                val calendar = Calendar.getInstance()
                meetingStartTime =
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
                agendaAdapter.updateMeetingStartTime(meetingStartTime)
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.agendaItems.collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            // Show loading state if needed
                        }

                        is Resource.Success -> {
                            val items = resource.data ?: emptyList()
                            Log.d("AgendaTableFragment", "Received ${items.size} agenda items")
                            agendaAdapter.submitList(items)
                        }

                        is Resource.Error -> {
                            Log.e(
                                "AgendaTableFragment",
                                "Error loading agenda items: ${resource.message}"
                            )
                            // Show error state
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveStatus.collect { status ->
                    when (status) {
                        is Resource.Success -> {
                            // Refresh the list after successful save
                            viewModel.loadAgendaItems(meetingId)
                        }

                        is Resource.Error -> {
                            // Show error message
                            status.message?.let { showError(it) }
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun showBreakTimeDialog() {
        AgendaDialogs.showTimeBreakDialog(
            requireContext(),
            object : AgendaDialogs.OnTimeBreakSetListener {
                override fun onTimeBreakSet(minutes: Int, seconds: Int) {
                    val totalSeconds = (minutes * 60) + seconds
                    val breakItem = AgendaItemDto(
                        id = "break_${System.currentTimeMillis()}",
                        meetingId = meetingId,
                        activity = if (minutes > 0) "$minutes MINUTE${if (minutes > 1) "S" else ""} BREAK"
                        else "$seconds SECOND${if (seconds > 1) "S" else ""} BREAK",
                        time = "", // Will be calculated
                        orderIndex = agendaAdapter.itemCount - 1, // Add before the add button
                        greenTime = 0,
                        yellowTime = 0,
                        redTime = totalSeconds,
                        presenterName = ""
                    )
                    viewModel.saveAgendaItem(breakItem)
                }
            })
    }

    private fun showSessionSelectionDialog() {
        AgendaDialogs.showSessionSelectionDialog(
            requireContext(),
            object : AgendaDialogs.OnSessionSelectedListener {
                override fun onSessionSelected(sessionName: String) {
                    val sessionItem = AgendaItemDto(
                        id = "session_${System.currentTimeMillis()}",
                        meetingId = meetingId,
                        activity = sessionName.uppercase(),
                        time = "", // Will be calculated
                        orderIndex = agendaAdapter.itemCount - 1, // Add before the add button
                        greenTime = 0,
                        yellowTime = 0,
                        redTime = 0,
                        presenterName = "",
                        isSessionHeader = true
                    )

                    // Add the session item to the list
                    val currentItems = agendaAdapter.getItems().toMutableList()
                    currentItems.add(sessionItem)

                    // Recalculate times for all items
                    val updatedItems = AgendaTimeCalculator.recalculateTimesFromPosition(
                        currentItems,
                        0,
                        meetingStartTime
                    )

                    // Update the adapter and save to Firebase
                    agendaAdapter.submitList(updatedItems)
                    viewModel.saveAllAgendaItems(meetingId, updatedItems)
                }
            })
    }

    private fun showError(message: String) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAddItemDialog() {
        Log.d("AgendaTableFragment", "showAddItemDialog called")
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val binding = DialogAddItemBinding.bind(dialogView)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
            
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.let { w ->
            val lp = w.attributes
            lp.dimAmount = 0f
            w.attributes = lp
            w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        binding.btnTimeBreak.setOnClickListener {
            dialog.dismiss()
            showTimeBreakDialog()
        }

        binding.btnSession.setOnClickListener {
            dialog.dismiss()
            showSessionSelectionDialog()
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        
    }

    private fun showTimeBreakDialog() {
        AgendaDialogs.showTimeBreakDialog(
            requireContext(),
            object : AgendaDialogs.OnTimeBreakSetListener {
                override fun onTimeBreakSet(minutes: Int, seconds: Int) {
                    val totalSeconds = (minutes * 60) + seconds
                    val timeBreakText = if (minutes > 0) {
                        "$minutes MINUTE${if (minutes > 1) "S" else ""} BREAK"
                    } else {
                        "$seconds SECOND${if (seconds > 1) "S" else ""} BREAK"
                    }

                    // Create a new time break item
                    val newItem = AgendaItemDto(
                        id = "break_${System.currentTimeMillis()}",
                        meetingId = meetingId,
                        orderIndex = agendaAdapter.itemCount - 1,
                        activity = timeBreakText,
                        presenterName = "",
                        time = "",
                        greenTime = 0,
                        yellowTime = 0,
                        redTime = totalSeconds,
                        isSessionHeader = false
                    )

                    // Add the new item and update the list
                    val currentItems = agendaAdapter.getItems().toMutableList()
                    currentItems.add(newItem)

                    // Recalculate times for all items
                    val updatedItems = AgendaTimeCalculator.recalculateTimesFromPosition(
                        currentItems,
                        0,
                        meetingStartTime
                    )

                    // Update the adapter and save to Firebase
                    agendaAdapter.submitList(updatedItems)
                    viewModel.saveAllAgendaItems(meetingId, updatedItems)
                }
            })
    }

    private fun setupRecyclerView() {
        Log.d("AgendaTableFragment", "Setting up RecyclerView")

        // Initialize the adapter with all necessary callbacks
        agendaAdapter = AgendaAdapter(
            onItemClick = { item ->
                Log.d("AgendaTableFragment", "Item clicked: ${item.activity}")
                // Handle item click if needed
            },
            onItemDelete = { item ->
                viewModel.deleteAgendaItem(meetingId, item.id)
            },
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            },
            onItemsUpdated = { updatedItems ->
                viewModel.saveAllAgendaItems(meetingId, updatedItems)
            },
            meetingStartTime = meetingStartTime,
            onAddItemClick = {
                Log.d("AgendaTableFragment", "Add button clicked, showing dialog")
                showAddItemDialog()
            },
            onItemMove = { fromPosition, toPosition ->
                val items = agendaAdapter.getItems().toMutableList()
                if (fromPosition < toPosition) {
                    for (i in fromPosition until toPosition) {
                        items[i] = items[i + 1].also { items[i + 1] = items[i] }
                        items[i].orderIndex = i
                        items[i + 1].orderIndex = i + 1
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        items[i] = items[i - 1].also { items[i - 1] = items[i] }
                        items[i].orderIndex = i
                        items[i - 1].orderIndex = i - 1
                    }
                }
                agendaAdapter.submitList(items)
                viewModel.saveAllAgendaItems(meetingId, items)
                true
            }
        )

        // Initialize with empty list to avoid NPE
        agendaAdapter.submitList(emptyList())

        // Configure RecyclerView
        binding.agendaRecyclerView.apply {
            // Use a LinearLayoutManager with vertical orientation
            layoutManager = object : LinearLayoutManager(requireContext()) {
                override fun canScrollVertically(): Boolean = true
            }.apply {
                orientation = LinearLayoutManager.VERTICAL
            }

            adapter = this@AgendaTableFragment.agendaAdapter
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
            setHasFixedSize(false)
            isNestedScrollingEnabled = false

            // Add long press listener for delete
            addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                private var longPressStartTime: Long = 0
                private val longPressDuration = 1000L // 1 second for long press
                private var longPressRunnable: Runnable? = null
                private var currentChild: View? = null

                override fun onInterceptTouchEvent(
                    rv: RecyclerView,
                    e: android.view.MotionEvent
                ): Boolean {
                    when (e.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            longPressStartTime = System.currentTimeMillis()
                            currentChild = rv.findChildViewUnder(e.x, e.y)

                            // Start long press detection
                            longPressRunnable = Runnable {
                                Log.d("AgendaTableFragment", "Long press detected!")
                                val position = rv.getChildAdapterPosition(
                                    currentChild ?: return@Runnable
                                )
                                Log.d(
                                    "AgendaTableFragment",
                                    "Long press position: $position"
                                )
                                if (position != RecyclerView.NO_POSITION) {
                                    val itemToDelete = agendaAdapter.getItemAt(position)
                                    Log.d(
                                        "AgendaTableFragment",
                                        "Item to delete: ${itemToDelete?.activity}"
                                    )
                                    if (itemToDelete != null) {
                                        showDeleteConfirmationDialog(
                                            position,
                                            itemToDelete
                                        )
                                    }
                                }
                            }
                            rv.postDelayed(longPressRunnable!!, longPressDuration)
                        }

                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            // Cancel long press if finger is lifted
                            longPressRunnable?.let { rv.removeCallbacks(it) }
                            longPressRunnable = null
                            currentChild = null
                        }
                    }
                    return false
                }
            })
        }

        // Set up item touch helper for drag and drop
        val callback = object : ItemTouchHelper.Callback() {
            private var isDragging = false
            private var lastMoveTime = 0L
            private val moveThrottle = 100L // Throttle moves to prevent rapid updates

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // Only allow drag for normal items (not time breaks, sessions, or add button)
                val dragFlags = when (viewHolder.itemViewType) {
                    VIEW_TYPE_ITEM -> ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    else -> 0
                }
                return makeMovementFlags(dragFlags, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // Don't move items if target is not a normal item or if source is not a normal item
                if (target.itemViewType != VIEW_TYPE_ITEM || viewHolder.itemViewType != VIEW_TYPE_ITEM) {
                    return false
                }

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastMoveTime < moveThrottle) {
                    return false // Throttle rapid moves
                }
                lastMoveTime = currentTime

                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                Log.d("AgendaTableFragment", "onMove: from $fromPosition to $toPosition")

                // Get the current list of items
                val items = agendaAdapter.getItems().toMutableList()

                // Check if positions are valid
                if (fromPosition < 0 || toPosition < 0 ||
                    fromPosition >= items.size || toPosition >= items.size
                ) {
                    return false
                }

                // Don't allow moving time breaks or session headers
                val fromItem = items[fromPosition]
                val toItem = items[toPosition]
                
                if (fromItem.activity.contains("BREAK", ignoreCase = true) || 
                    fromItem.isSessionHeader ||
                    toItem.activity.contains("BREAK", ignoreCase = true) || 
                    toItem.isSessionHeader) {
                    return false
                }

                // Swap the items
                Collections.swap(items, fromPosition, toPosition)

                // Update order indices
                items[fromPosition].orderIndex = fromPosition
                items[toPosition].orderIndex = toPosition

                // Update the adapter
                agendaAdapter.submitList(items)

                // Save the new order
                viewModel.saveAllAgendaItems(meetingId, items)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No swipe action
            }

            override fun isLongPressDragEnabled(): Boolean {
                // Enable long press drag
                return true
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                // Disable swipe to dismiss
                return false
            }

            override fun onSelectedChanged(
                viewHolder: RecyclerView.ViewHolder?,
                actionState: Int
            ) {
                super.onSelectedChanged(viewHolder, actionState)
                Log.d("AgendaTableFragment", "onSelectedChanged: actionState=$actionState")
                when (actionState) {
                    ItemTouchHelper.ACTION_STATE_DRAG -> {
                        viewHolder?.itemView?.alpha = 0.7f
                    }

                    ItemTouchHelper.ACTION_STATE_IDLE -> {
                        viewHolder?.itemView?.alpha = 1.0f
                        Log.d("AgendaTableFragment", "Drag ended")
                        // Recalculate times including breaks from the top and persist in one batch
                        val reordered = agendaAdapter.getItems().toMutableList()
                        if (reordered.isNotEmpty()) {
                            reordered.forEachIndexed { index, item -> item.orderIndex = index }
                            val recalculated = AgendaTimeCalculator.recalculateTimesFromPosition(
                                reordered,
                                0,
                                meetingStartTime
                            )
                            binding.agendaRecyclerView.post {
                                agendaAdapter.submitList(recalculated)
                                viewModel.reorderItems(meetingId, recalculated)
                            }
                        }
                    }
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
                Log.d("AgendaTableFragment", "clearView called")
            }
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.agendaRecyclerView)
    }

    private fun showDeleteConfirmationDialog(
        position: Int,
        itemToDelete: AgendaItemDto
    ) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete '${itemToDelete.activity}'?")
            .setPositiveButton("Delete") { _, _ ->
                performDelete(position, itemToDelete)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDelete(position: Int, itemToDelete: AgendaItemDto) {
        Log.d(
            "AgendaTableFragment",
            "Performing delete for item: ${itemToDelete.activity} at position $position"
        )

        // Get current items and remove the deleted item
        val currentItems = agendaAdapter.getCurrentList().toMutableList()
        currentItems.removeAt(position)

        // Recalculate times for remaining items starting from the deleted position
        if (currentItems.isNotEmpty()) {
            val startPosition = if (position == 0) 0 else position - 1
            val updatedItems = AgendaTimeCalculator.recalculateTimesFromPosition(
                currentItems,
                startPosition,
                meetingStartTime
            )

            // Update the adapter with recalculated times
            agendaAdapter.submitList(updatedItems)

            // Save the updated items to Firebase
            viewModel.reorderItems(meetingId, updatedItems)
        } else {
            // If no items left, update adapter with empty list
            agendaAdapter.submitList(emptyList())
        }

        // Delete the item from Firebase
        viewModel.deleteAgendaItem(meetingId, itemToDelete.id)

        // Show confirmation message
        showMessage("Item deleted: ${itemToDelete.activity}")
    }

    private fun showMessage(message: String) {
        view?.let { view ->
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showEditDialog(item: AgendaItemDto) {
        val dialog = AgendaItemDialog.newInstance(
            meetingId = meetingId,
            agendaItem = item,
            onSave = { updatedDto ->
                viewModel.saveAgendaItem(updatedDto)
                // After saving, recalculate times for all items to ensure consistency
                val currentItems = agendaAdapter.getCurrentList().toMutableList()
                val itemIndex = currentItems.indexOfFirst { it.id == updatedDto.id }
                if (itemIndex != -1) {
                    currentItems[itemIndex] = updatedDto
                    // Recalculate times starting from the updated item's position
                    val updatedItems = AgendaTimeCalculator.recalculateTimesFromPosition(
                        currentItems,
                        itemIndex,
                        meetingStartTime
                    )
                    agendaAdapter.submitList(updatedItems)
                    // Save the recalculated items to Firebase
                    viewModel.reorderItems(meetingId, updatedItems)
                }
            }
        )
        dialog.show(parentFragmentManager, "EditAgendaItemDialog")
    }

// Removed onStartDrag as it's now handled by the adapter

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            val agendaItems = agendaAdapter.getItems()
            if (agendaItems.isNotEmpty()) {
                // Update order indices based on current position
                val updatedItems = agendaItems.mapIndexed { index, item ->
                    item.copy(orderIndex = index)
                }
                viewModel.saveAllAgendaItems(meetingId, updatedItems)
            } else {
                showMessage("No agenda items to save")
            }
        }

        binding.fabAddItem.setOnClickListener {
            Log.d("AgendaTableFragment", "Add button clicked, meetingId: $meetingId")

            val currentItems = agendaAdapter.getCurrentList()
            val isFirstItem = currentItems.isEmpty()
            val lastItem = currentItems.maxByOrNull { it.orderIndex }

            viewLifecycleOwner.lifecycleScope.launch {
                // Get meeting start time for first item
                val defaultTime = if (isFirstItem) {
                    val meeting = meetingRepository.getMeetingById(meetingId)
                    meeting?.dateTime?.let { dateTime ->
                        try {
                            // Format LocalDateTime to 12-hour format string
                            val formatter = java.time.format.DateTimeFormatter.ofPattern(
                                "hh:mm a",
                                Locale.getDefault()
                            )
                            dateTime.format(formatter)
                        } catch (e: Exception) {
                            Log.e(
                                "AgendaTableFragment",
                                "Error formatting meeting start time: ${e.message}"
                            )
                            // Fallback to current time
                            val calendar = Calendar.getInstance()
                            SimpleDateFormat(
                                "hh:mm a",
                                Locale.getDefault()
                            ).format(calendar.time)
                        }
                    } ?: run {
                        // Fallback to current time if no start time is set
                        val calendar = Calendar.getInstance()
                        SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                        ).format(calendar.time)
                    }
                } else {
                    // For non-first items, calculate next time based on previous item
                    val lastTime = lastItem?.time ?: "09:00 AM"
                    // Default to 0 minutes if no red time is set
                    val redDuration = lastItem?.redTime?.let { it / 60 } ?: 0
                    TimeUtils.calculateNextTime(
                        lastTime,
                        redDuration
                    )
                }
                // Default to 0 seconds for new items
                val defaultRedTime = 0

                // Show dialog on UI thread
                activity?.runOnUiThread {
                    AgendaItemDialog.newInstance(
                        meetingId = meetingId,
                        agendaItem = AgendaItemDto(
                            id = "",
                            meetingId = meetingId,
                            orderIndex = agendaAdapter.itemCount,
                            activity = "",
                            presenterName = "",
                            time = defaultTime,
                            greenTime = 0,
                            yellowTime = 0,
                            redTime = defaultRedTime
                        ),
                        onSave = { newItem ->
                            val itemToSave = newItem.copy(
                                id = newItem.id.ifEmpty {
                                    System.currentTimeMillis().toString()
                                },
                                orderIndex = agendaAdapter.itemCount,
                                meetingId = meetingId // Ensure meetingId is set
                            )
                            Log.d("AgendaTableFragment", "Saving new item: $itemToSave")
                            viewModel.saveAgendaItem(itemToSave)
                            // After adding a new item, recalculate times for all items
                            val updatedItems =
                                AgendaTimeCalculator.recalculateTimesFromPosition(
                                    agendaAdapter.getCurrentList().toMutableList()
                                        .apply { add(itemToSave) },
                                    0,
                                    meetingStartTime
                                )
                            agendaAdapter.submitList(updatedItems)
                            // Save the recalculated items to Firebase
                            viewModel.reorderItems(meetingId, updatedItems)
                        }
                    ).show(parentFragmentManager, "AgendaItemDialog")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}