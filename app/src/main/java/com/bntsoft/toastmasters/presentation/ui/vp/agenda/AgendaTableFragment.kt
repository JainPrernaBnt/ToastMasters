package com.bntsoft.toastmasters.presentation.ui.vp.agenda

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
import com.bntsoft.toastmasters.data.mapper.AgendaItemMapper
import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
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
                viewModel.agendaItems.collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val loadedItems = result.data ?: emptyList()
                            Log.d("AgendaTableFragment", "Agenda items loaded: ${loadedItems.size}")
                            loadedItems.forEachIndexed { index, item ->
                                Log.d(
                                    "AgendaTableFragment",
                                    "Item $index: ${item.activity} (order: ${item.orderIndex})"
                                )
                            }
                            // Submit the loaded items to the adapter
                            val sortedItems = loadedItems.sortedBy { it.orderIndex }
                            Log.d(
                                "AgendaTableFragment",
                                "Submitting ${sortedItems.size} items to adapter"
                            )
                            agendaAdapter.submitList(sortedItems)
                            Log.d(
                                "AgendaTableFragment",
                                "Adapter item count after submit: ${agendaAdapter.itemCount}"
                            )
                        }

                        is Resource.Error -> {
                            showError(result.message ?: "An error occurred")
                        }

                        is Resource.Loading -> {
                            // Show loading state if needed
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe save status
                viewModel.saveStatus.collect { status ->
                    when (status) {
                        is Resource.Success -> {
                            binding.saveButton.isEnabled = true
                            showMessage("Agenda saved successfully")
                            // Reload agenda items after successful save
                            viewModel.loadAgendaItems(meetingId)
                            viewModel.clearStatus()
                        }

                        is Resource.Error -> {
                            binding.saveButton.isEnabled = true
                            showError(status.message ?: "Failed to save agenda")
                            viewModel.clearStatus()
                        }

                        is Resource.Loading -> {
                            binding.saveButton.isEnabled = false
                        }

                        null -> { /* No action needed */
                        }
                    }
                }
            }
        }
    }

    private fun showMessage(message: String) {
        view?.let { view ->
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        view?.let { view ->
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        agendaAdapter = AgendaAdapter(
            onItemClick = { item ->
                // Handle item click (edit)
                showEditDialog(item)
            },
            onItemDelete = { item ->
                // Handle item delete
                viewModel.deleteAgendaItem(meetingId, item.id)
            },
            onStartDrag = { viewHolder ->
                // Start drag
                itemTouchHelper.startDrag(viewHolder)
            },
            onItemsUpdated = { updatedItems ->
                // Update items in Firebase when order or times change
                viewModel.reorderItems(meetingId, updatedItems)
            },
            meetingStartTime = meetingStartTime
        )

        // Initialize with empty list to avoid NPE
        agendaAdapter.submitList(emptyList())

        binding.agendaRecyclerView.apply {
            // Use a LinearLayoutManager with vertical orientation
            layoutManager = LinearLayoutManager(requireContext()).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            this.adapter = this@AgendaTableFragment.agendaAdapter
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
            setHasFixedSize(true)
            isNestedScrollingEnabled = true

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
                                val position = rv.getChildAdapterPosition(currentChild ?: return@Runnable)
                                Log.d("AgendaTableFragment", "Long press position: $position")
                                if (position != RecyclerView.NO_POSITION) {
                                    val itemToDelete = agendaAdapter.getItemAt(position)
                                    Log.d("AgendaTableFragment", "Item to delete: ${itemToDelete?.activity}")
                                    if (itemToDelete != null) {
                                        showDeleteConfirmationDialog(position, itemToDelete)
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

        // Set up item touch helper for drag and drop only (no swipe)
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0 // Remove swipe directions
        ) {
            private var isDragging = false
            private var lastMoveTime = 0L
            private val moveThrottle = 100L // Throttle moves to prevent rapid updates

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastMoveTime < moveThrottle) {
                    return false // Throttle rapid moves
                }
                lastMoveTime = currentTime

                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                Log.d("AgendaTableFragment", "onMove: from $fromPosition to $toPosition")

                if (fromPosition < 0 || fromPosition >= agendaAdapter.itemCount ||
                    toPosition < 0 || toPosition >= agendaAdapter.itemCount
                ) {
                    Log.w("AgendaTableFragment", "Invalid positions: from=$fromPosition, to=$toPosition, count=${agendaAdapter.itemCount}")
                    return false
                }

                // Get current items from adapter
                val currentItems = agendaAdapter.getCurrentList().toMutableList()

                // Move the item in the list
                val moved = currentItems.removeAt(fromPosition)
                currentItems.add(toPosition, moved)

                Log.d("AgendaTableFragment", "Moved item: ${moved.activity} from $fromPosition to $toPosition")

                // Recompute orderIndex based on new positions
                for (i in currentItems.indices) {
                    val it = currentItems[i]
                    currentItems[i] = it.copy(orderIndex = i)
                }

                val updatedItems = AgendaTimeCalculator.recalculateTimesFromPosition(
                    currentItems,
                    0,
                    meetingStartTime
                )
                agendaAdapter.submitList(ArrayList(updatedItems))

                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                Log.d("AgendaTableFragment", "onSelectedChanged: actionState=$actionState")
                when (actionState) {
                    ItemTouchHelper.ACTION_STATE_DRAG -> {
                        isDragging = true
                        viewHolder?.itemView?.alpha = 0.7f
                        Log.d("AgendaTableFragment", "Drag started")
                    }

                    else -> {
                        isDragging = false
                        viewHolder?.itemView?.alpha = 1.0f
                        viewHolder?.itemView?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        Log.d("AgendaTableFragment", "Drag ended")
                    }
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
                viewHolder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isDragging = false

                val currentItems = agendaAdapter.getCurrentList()
                if (currentItems.isNotEmpty()) {
                    viewModel.reorderItems(meetingId, currentItems)
                }
            }

            override fun isLongPressDragEnabled(): Boolean = true

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used as swipe is disabled
            }
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.agendaRecyclerView)
    }

    private fun showDeleteConfirmationDialog(position: Int, itemToDelete: AgendaItemDto) {
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
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
                        }
                    } ?: run {
                        // Fallback to current time if no start time is set
                        val calendar = Calendar.getInstance()
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
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
                                id = newItem.id.ifEmpty { System.currentTimeMillis().toString() },
                                orderIndex = agendaAdapter.itemCount,
                                meetingId = meetingId // Ensure meetingId is set
                            )
                            Log.d("AgendaTableFragment", "Saving new item: $itemToSave")
                            viewModel.saveAgendaItem(itemToSave)
                            // After adding a new item, recalculate times for all items
                            val updatedItems = AgendaTimeCalculator.recalculateTimesFromPosition(
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