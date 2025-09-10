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
    private lateinit var adapter: AgendaAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private val items = mutableListOf<AgendaItemDto>()

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

        // Load existing agenda items
        viewModel.loadAgendaItems(meetingId)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.agendaItems.collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val items = result.data ?: emptyList()
                            Log.d("AgendaTableFragment", "Agenda items loaded: ${items.size}")
                            adapter.submitList(ArrayList(items)) // Force fresh copy
                            this@AgendaTableFragment.items.clear()
                            this@AgendaTableFragment.items.addAll(items)
                        }

                        is Resource.Error -> {
                            showError(result.message ?: "Failed to load agenda items")
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
        adapter = AgendaAdapter(
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
            }
        )

        // Initialize with empty list to avoid NPE
        adapter.submitList(emptyList())

        binding.agendaRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@AgendaTableFragment.adapter
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
            setHasFixedSize(true)
        }

        // Set up item touch helper for drag and drop
        val callback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                if (fromPosition < 0 || fromPosition >= adapter.itemCount ||
                    toPosition < 0 || toPosition >= adapter.itemCount
                ) {
                    return false
                }

                // Move the item in the list
                val moved = items.removeAt(fromPosition)
                items.add(toPosition, moved)

                // Recompute orderIndex based on new positions
                for (i in items.indices) {
                    val it = items[i]
                    items[i] = it.copy(orderIndex = i)
                }

                // Refresh adapter with new order
                adapter.submitList(ArrayList(items))

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position < items.size) {
                    val item = items[position]
                    // Remove the item from the list and update the adapter
                    items.removeAt(position)
                    adapter.submitList(ArrayList(items))
                    // Notify the view model to delete the item
                    viewModel.deleteAgendaItem(meetingId, item.id)
                }
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.7f
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f

                // Update the order indices based on current positions
                val updatedItems = items.mapIndexed { index, item ->
                    item.copy(orderIndex = index, updatedAt = com.google.firebase.Timestamp.now())
                }

                // Update the local list
                items.clear()
                items.addAll(updatedItems)

                // Only save if there are items to save
                if (updatedItems.isNotEmpty()) {
                    viewModel.reorderItems(meetingId, updatedItems)
                }
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return true
            }
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.agendaRecyclerView)
    }

    private fun showEditDialog(item: AgendaItemDto) {
        val dialog = AgendaItemDialog.newInstance(
            meetingId = meetingId,
            agendaItem = item,
            onSave = { updatedDto ->
                viewModel.saveAgendaItem(updatedDto)
            }
        )
        dialog.show(parentFragmentManager, "EditAgendaItemDialog")
    }

    // Removed onStartDrag as it's now handled by the adapter

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            val agendaItems = adapter.getItems()
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

            val isFirstItem = items.isEmpty()
            val lastItem = items.maxByOrNull { it.orderIndex }

            viewLifecycleOwner.lifecycleScope.launch {
                // Get meeting start time for first item
                val defaultTime = if (isFirstItem) {
                    val meeting = meetingRepository.getMeetingById(meetingId)
                    meeting?.dateTime?.let { dateTime ->
                        try {
                            // Format LocalDateTime to 12-hour format string
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
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
                    val redDuration = lastItem?.redTime ?: 5
                    TimeUtils.calculateNextTime(
                        lastTime,
                        redDuration / 60
                    ) // Convert seconds to minutes
                }

                // Default to 0 seconds if no items exist, otherwise use last item's red time
                val defaultRedTime = lastItem?.redTime ?: 0

                // Show dialog on UI thread
                activity?.runOnUiThread {
                    AgendaItemDialog.newInstance(
                        meetingId = meetingId,
                        agendaItem = AgendaItemDto(
                            id = "",
                            meetingId = meetingId,
                            orderIndex = adapter.itemCount,
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
                                orderIndex = adapter.itemCount,
                                meetingId = meetingId // Ensure meetingId is set
                            )
                            Log.d("AgendaTableFragment", "Saving new item: $itemToSave")
                            viewModel.saveAgendaItem(itemToSave)
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