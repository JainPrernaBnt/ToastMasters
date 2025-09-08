package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.bntsoft.toastmasters.domain.model.AgendaItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.bntsoft.toastmasters.utils.Resource
import com.bntsoft.toastmasters.utils.TimeUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AgendaTableFragment : Fragment() {
    @Inject
    lateinit var agendaItemMapper: AgendaItemMapper

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

        meetingId = arguments?.getString("meetingId") ?: run {
            findNavController().navigateUp()
            return
        }

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
                            adapter.submitList(items)

                            // Update local items list
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
                        null -> { /* No action needed */ }
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
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
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
                    toPosition < 0 || toPosition >= adapter.itemCount) {
                    return false
                }
                
                // Update the order indices
                val fromItem = items[fromPosition]
                val toItem = items[toPosition]
                
                // Swap the order indices
                val newFromIndex = toItem.orderIndex
                
                // Update the order indices of affected items
                if (fromPosition < toPosition) {
                    for (i in fromPosition + 1..toPosition) {
                        val currentItem = items[i]
                        items[i] = currentItem.copy(orderIndex = currentItem.orderIndex - 1)
                    }
                } else {
                    for (i in toPosition until fromPosition) {
                        val currentItem = items[i]
                        items[i] = currentItem.copy(orderIndex = currentItem.orderIndex + 1)
                    }
                }
                
                // Update the moved item's order index
                items[toPosition] = fromItem.copy(orderIndex = newFromIndex)
                
                // Update the adapter with the new list
                adapter.submitList(ArrayList(items))
                // No need for callback as the adapter will handle the diffing
                
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
            
            // Get the last item to calculate next time
            val lastItem = items.maxByOrNull { it.orderIndex }
            val lastTime = lastItem?.time ?: "09:00 AM" // Default start time if no items exist
            val redDuration = lastItem?.redTime ?: 5 // Default to 5 minutes if no items exist
            
            // Calculate next time by adding red duration to last time
            val nextTime = if (lastItem != null) {
                TimeUtils.calculateNextTime(lastTime, redDuration)
            } else {
                lastTime
            }
            
            AgendaItemDialog.newInstance(
                meetingId = meetingId,
                agendaItem = AgendaItemDto(
                    id = "", // New item, will be generated by Firestore
                    meetingId = meetingId,
                    orderIndex = adapter.itemCount, // Set order index to current count
                    activity = "",
                    presenterName = "",
                    time = nextTime, // Set the calculated time
                    greenTime = 0,
                    yellowTime = 0,
                    redTime = redDuration // Pass the last item's red duration
                ),
                onSave = { newItem ->
                    // Ensure the item has a valid ID and order index
                    val itemToSave = newItem.copy(
                        id = newItem.id.ifEmpty { System.currentTimeMillis().toString() },
                        orderIndex = adapter.itemCount // Set order index to current count
                    )
                    Log.d("AgendaTableFragment", "Saving new item: $itemToSave")
                    viewModel.saveAgendaItem(itemToSave)
                }
            ).show(parentFragmentManager, "AgendaItemDialog")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}