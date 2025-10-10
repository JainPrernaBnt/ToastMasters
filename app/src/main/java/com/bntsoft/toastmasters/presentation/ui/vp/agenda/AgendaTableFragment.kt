package com.bntsoft.toastmasters.presentation.ui.vp.agenda

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
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.utils.AgendaTimeCalculator
import com.bntsoft.toastmasters.utils.Resource
import com.bntsoft.toastmasters.utils.TimeUtils
import com.bntsoft.toastmasters.utils.UserManager
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

    @Inject
    lateinit var userManager: UserManager

    private var _binding: FragmentAgendaTableBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AgendaTableViewModel by viewModels()
    lateinit var meetingId: String
    private lateinit var agendaAdapter: AgendaAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var meetingStartTime: String? = null
    private var isVpEducation: Boolean = false
    private var isAgendaPublished: Boolean = false
    private var isStatusCheckInProgress: Boolean = false

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

        // Check user role and agenda status
        checkUserRoleAndAgendaStatus()
        setupRecyclerView()
        setupClickListeners()
        setupObservers()

        // Load meeting start time and existing agenda items
        loadMeetingStartTime()
        viewModel.loadAgendaItems(meetingId)
    }

    private fun checkUserRoleAndAgendaStatus() {
        // Prevent multiple concurrent calls
        if (isStatusCheckInProgress) {
            Log.d("AgendaTableFragment", "Status check already in progress, skipping")
            return
        }
        
        isStatusCheckInProgress = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Check if user is VP Education
                isVpEducation = userManager.isVpEducation()
                Log.d("AgendaTableFragment", "User is VP Education: $isVpEducation")

                // Check agenda status from meeting - force fresh fetch
                val meeting = meetingRepository.getMeetingById(meetingId)
                val currentAgendaStatus = meeting?.agendaStatus
                // If agendaStatus is null or not FINALIZED, treat as draft (editable)
                isAgendaPublished = currentAgendaStatus == AgendaStatus.FINALIZED
                Log.d("AgendaTableFragment", "Meeting agenda status: $currentAgendaStatus, isAgendaPublished: $isAgendaPublished")

                // Apply role-based UI visibility
                applyRoleBasedVisibility()
            } catch (e: Exception) {
                Log.e(
                    "AgendaTableFragment",
                    "Error checking user role and agenda status: ${e.message}"
                )
                // Default to Member view for safety
                isVpEducation = false
                isAgendaPublished = false
                applyRoleBasedVisibility()
            } finally {
                isStatusCheckInProgress = false
            }
        }
    }

    private fun applyRoleBasedVisibility() {
        Log.d("AgendaTableFragment", "applyRoleBasedVisibility: isVpEducation=$isVpEducation, isAgendaPublished=$isAgendaPublished")
        
        if (isVpEducation) {
            // VP Education: Show publish/edit button
            binding.publishAgendaButton.visibility = View.VISIBLE
            
            if (isAgendaPublished) {
                // Agenda is published - show edit button and hide editing controls
                binding.publishAgendaButton.text = "Edit Agenda"
                binding.fabAddItem.visibility = View.GONE
                Log.d("AgendaTableFragment", "UI set to PUBLISHED state - Edit Agenda button shown, FAB hidden")
            } else {
                // Agenda is draft - show publish button and editing controls
                binding.publishAgendaButton.text = "Publish Agenda to Members"
                binding.fabAddItem.visibility = View.VISIBLE
                Log.d("AgendaTableFragment", "UI set to DRAFT state - Publish button shown, FAB visible")
            }
        } else {
            // Member: Hide editing UI elements
            binding.publishAgendaButton.visibility = View.GONE
            binding.fabAddItem.visibility = View.GONE
        }

        // Update adapter with user role and publish status (only if adapter is initialized)
        if (::agendaAdapter.isInitialized) {
            agendaAdapter.updateUserRole(isVpEducation && !isAgendaPublished)
        }
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
                            // Don't automatically refresh - let the adapter handle updates
                            // Only show success message if needed
                            Log.d("AgendaTableFragment", "Agenda items saved successfully")
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
                    val timeBreakText = if (minutes > 0) {
                        "$minutes MINUTE${if (minutes > 1) "S" else ""} BREAK"
                    } else {
                        "$seconds SECOND${if (seconds > 1) "S" else ""} BREAK"
                    }

                    // Create a new time break item
                    val breakItem = AgendaItemDto(
                        id = "break_${System.currentTimeMillis()}",
                        meetingId = meetingId,
                        activity = timeBreakText,
                        time = "", // Will be calculated
                        orderIndex = agendaAdapter.getItems().size, // Add at the end
                        greenTime = 0,
                        yellowTime = 0,
                        redTime = totalSeconds,
                        presenterName = "",
                        isSessionHeader = false
                    )

                    // Add the break item to the list
                    val currentItems = agendaAdapter.getItems().toMutableList()
                    currentItems.add(breakItem)

                    // Recalculate times for all items
                    val updatedItems = AgendaTimeCalculator.recalculateTimesFromPosition(
                        currentItems,
                        0,
                        meetingStartTime
                    )

                    // Update order indices to be sequential
                    updatedItems.forEachIndexed { index, item ->
                        item.orderIndex = index
                    }

                    // Update the adapter and save to Firebase
                    agendaAdapter.submitList(updatedItems)
                    viewModel.saveAllAgendaItems(meetingId, updatedItems)
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
                        orderIndex = agendaAdapter.getItems().size, // Add at the end
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

                    // Update order indices to be sequential
                    updatedItems.forEachIndexed { index, item ->
                        item.orderIndex = index
                    }

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
            showBreakTimeDialog()
        }

        binding.btnSession.setOnClickListener {
            dialog.dismiss()
            showSessionSelectionDialog()
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

    }

    private fun setupRecyclerView() {
        Log.d("AgendaTableFragment", "Setting up RecyclerView")

        // Initialize the adapter with all necessary callbacks
        Log.d("AgendaTableFragment", "Initializing adapter with isVpEducation=$isVpEducation")
        agendaAdapter = AgendaAdapter(
            onItemClick = { item ->
                Log.d("AgendaTableFragment", "Item clicked: ${item.activity}")
                if (isVpEducation && !isAgendaPublished) {
                    showEditDialog(item)
                }
            },
            onItemDelete = { item ->
                // Find the position of the item for the delete confirmation dialog
                val position = agendaAdapter.getCurrentList().indexOfFirst { it.id == item.id }
                if (position != -1) {
                    showDeleteConfirmationDialog(position, item)
                }
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
            },
            isVpEducation = isVpEducation,
            onTimeRowClick = { item ->
                if (isVpEducation && !isAgendaPublished) {
                    showEditTimeBreakDialog(item)
                }
            },
            onSessionRowClick = { item ->
                if (isVpEducation && !isAgendaPublished) {
                    showEditSessionDialog(item)
                }
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
                val position = viewHolder.adapterPosition
                // Disable drag if agenda is published or user is not VP Education
                val dragFlags = if (agendaAdapter.canDrag(position) && isVpEducation && !isAgendaPublished) {
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN
                } else 0
                return makeMovementFlags(dragFlags, 0)
            }

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

                // Get the current list of items
                val items = agendaAdapter.getItems().toMutableList()

                // Check if positions are valid
                if (fromPosition < 0 || toPosition < 0 ||
                    fromPosition >= items.size || toPosition >= items.size
                ) {
                    return false
                }

                // Remove the item from the original position
                val movedItem = items.removeAt(fromPosition)

                // Insert it at the new position
                items.add(toPosition, movedItem)

                // Update order indices for all items
                items.forEachIndexed { index, item ->
                    item.orderIndex = index
                }

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
                // Enable long press drag only if VP Education and agenda not published
                return isVpEducation && !isAgendaPublished
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

    private fun publishAgenda() {
        // Show confirmation dialog before publishing
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Publish Agenda")
            .setMessage("Do you want to publish the agenda to members? Once published, editing will be restricted.")
            .setPositiveButton("Publish") { _, _ ->
                performPublishAgenda()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performPublishAgenda() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showMessage("Publishing agenda...")
                // Update agenda status to FINALIZED (published)
                val result = viewModel.publishAgenda(meetingId)
                when (result) {
                    is Resource.Success -> {
                        isAgendaPublished = true
                        showMessage("Agenda published successfully! Members can now view it.")
                        // Update UI to reflect published state
                        applyRoleBasedVisibility()
                        // Refresh the ItemTouchHelper to disable drag and drop
                        itemTouchHelper.attachToRecyclerView(null)
                        itemTouchHelper.attachToRecyclerView(binding.agendaRecyclerView)
                    }

                    is Resource.Error -> {
                        showMessage("Failed to publish agenda: ${result.message}")
                    }

                    is Resource.Loading -> {
                        showMessage("Publishing agenda...")
                    }
                }
            } catch (e: Exception) {
                showMessage("Error publishing agenda: ${e.message}")
            }
        }
    }

    private fun editAgenda() {
        // Show confirmation dialog before enabling edit mode
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Agenda")
            .setMessage("Do you want to edit the published agenda? This will make it a draft again.")
            .setPositiveButton("Edit") { _, _ ->
                performEditAgenda()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performEditAgenda() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showMessage("Enabling edit mode...")
                // Update agenda status back to DRAFT
                val result = viewModel.updateAgendaStatus(meetingId, AgendaStatus.DRAFT)
                when (result) {
                    is Resource.Success -> {
                        isAgendaPublished = false
                        showMessage("Agenda is now in edit mode.")
                        // Update UI to reflect draft state
                        applyRoleBasedVisibility()
                        // Refresh the ItemTouchHelper to enable drag and drop
                        itemTouchHelper.attachToRecyclerView(null)
                        itemTouchHelper.attachToRecyclerView(binding.agendaRecyclerView)
                    }

                    is Resource.Error -> {
                        showMessage("Failed to enable edit mode: ${result.message}")
                    }

                    is Resource.Loading -> {
                        showMessage("Enabling edit mode...")
                    }
                }
            } catch (e: Exception) {
                showMessage("Error enabling edit mode: ${e.message}")
            }
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

    private fun showEditTimeBreakDialog(item: AgendaItemDto) {
        AgendaDialogs.showEditTimeBreakDialog(
            requireContext(),
            item.activity ?: "",
            object : AgendaDialogs.OnTimeBreakSetListener {
                override fun onTimeBreakSet(minutes: Int, seconds: Int) {
                    val totalSeconds = (minutes * 60) + seconds
                    val timeBreakText = if (minutes > 0) {
                        "$minutes MINUTE${if (minutes > 1) "S" else ""} BREAK"
                    } else {
                        "$seconds SECOND${if (seconds > 1) "S" else ""} BREAK"
                    }

                    // Update the item
                    val updatedItem = item.copy(
                        activity = timeBreakText,
                        redTime = totalSeconds
                    )

                    // Update in the list and recalculate times
                    val currentItems = agendaAdapter.getCurrentList().toMutableList()
                    val itemIndex = currentItems.indexOfFirst { it.id == item.id }
                    if (itemIndex != -1) {
                        currentItems[itemIndex] = updatedItem
                        // Recalculate times for all items
                        val updatedItems = AgendaTimeCalculator.recalculateTimesFromPosition(
                            currentItems,
                            0,
                            meetingStartTime
                        )
                        agendaAdapter.submitList(updatedItems)
                        viewModel.saveAllAgendaItems(meetingId, updatedItems)
                    }
                }
            }
        )
    }

    private fun showEditSessionDialog(item: AgendaItemDto) {
        AgendaDialogs.showSessionSelectionDialog(
            requireContext(),
            object : AgendaDialogs.OnSessionSelectedListener {
                override fun onSessionSelected(sessionName: String) {
                    // Update the item
                    val updatedItem = item.copy(
                        activity = sessionName.uppercase()
                    )

                    // Update in the list
                    val currentItems = agendaAdapter.getCurrentList().toMutableList()
                    val itemIndex = currentItems.indexOfFirst { it.id == item.id }
                    if (itemIndex != -1) {
                        currentItems[itemIndex] = updatedItem
                        agendaAdapter.submitList(currentItems)
                        viewModel.saveAllAgendaItems(meetingId, currentItems)
                    }
                }
            }
        )
    }

// Removed onStartDrag as it's now handled by the adapter

    private fun setupClickListeners() {
        // Publish/Edit button click listener (only for VP Education)
        binding.publishAgendaButton.setOnClickListener {
            if (isVpEducation) {
                if (isAgendaPublished) {
                    editAgenda()
                } else {
                    publishAgenda()
                }
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

    override fun onResume() {
        super.onResume()
        Log.d("AgendaTableFragment", "onResume() called - refreshing agenda status")
        // Refresh agenda status when fragment resumes to handle navigation back scenarios
        checkUserRoleAndAgendaStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}