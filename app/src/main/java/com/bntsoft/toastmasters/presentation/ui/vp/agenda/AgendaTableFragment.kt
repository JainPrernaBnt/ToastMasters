package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.data.model.dto.AgendaItemDto
import com.bntsoft.toastmasters.databinding.FragmentAgendaTableBinding

class AgendaTableFragment : Fragment(), OnStartDragListener {

    private var _binding: FragmentAgendaTableBinding? = null
    private val binding get() = _binding!!
    lateinit var meetingId: String
    private lateinit var adapter: AgendaAdapter

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
    }

    private lateinit var itemTouchHelper: ItemTouchHelper

    private fun setupRecyclerView() {
        adapter = AgendaAdapter(
            onEditClick = { position ->
                val item = adapter.getItemAtPosition(position)
                Log.d("AgendaTableFragment", "Editing item: $item")

                AgendaItemDialog.newInstance(
                    agendaItem = AgendaItemDto(
                        id = item.id,
                        meetingId = item.meetingId.ifEmpty { meetingId },
                        orderIndex = item.orderIndex,
                        activity = item.activity,
                        presenterName = item.presenter,
                        time = item.time,
                        greenTime = item.greenTime,
                        yellowTime = item.yellowTime,
                        redTime = item.redTime
                    ),
                    onSave = { updatedItem ->
                        try {
                            Log.d(
                                "AgendaTableFragment",
                                "Saving updated item at position $position"
                            )
                            val updated = AgendaItem(
                                id = updatedItem.id.ifEmpty { item.id },
                                meetingId = updatedItem.meetingId.ifEmpty { meetingId },
                                orderIndex = item.orderIndex,
                                activity = updatedItem.activity,
                                presenter = updatedItem.presenterName,
                                time = updatedItem.time,
                                greenTime = updatedItem.greenTime,
                                yellowTime = updatedItem.yellowTime,
                                redTime = updatedItem.redTime
                            )
                            Log.d("AgendaTableFragment", "Updated item data: $updated")
                            adapter.updateItemAtPosition(updated, position)

                            binding.agendaRecyclerView.post {
                                binding.agendaRecyclerView.scrollToPosition(position)
                            }
                        } catch (e: Exception) {
                            Log.e(
                                "AgendaTableFragment",
                                "Error updating item at position $position",
                                e
                            )
                        }
                    }
                ).show(parentFragmentManager, "EditAgendaItemDialog")
            },
            onDeleteClick = { position ->
                adapter.removeItemAtPosition(position)
            }
        )

        adapter.setOnStartDragListener(this)

        // Set up ItemTouchHelper for drag and drop
        val callback = SimpleItemTouchHelperCallback(adapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.agendaRecyclerView)

        binding.agendaRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this@apply.adapter = this@AgendaTableFragment.adapter
            setHasFixedSize(true)
        }
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            // Handle save button click
            val agendaItems = adapter.getItems()
            // TODO: Save the agenda items
        }

        binding.fabAddItem.setOnClickListener {
            Log.d("AgendaTableFragment", "Add button clicked, meetingId: $meetingId")
            AgendaItemDialog.newInstance(onSave = { newItem ->
                val item = AgendaItem(
                    id = newItem.id.ifEmpty { System.currentTimeMillis().toString() },
                    meetingId = newItem.meetingId.ifEmpty { meetingId },
                    orderIndex = adapter.itemCount,
                    activity = newItem.activity,
                    presenter = newItem.presenterName,
                    time = newItem.time,
                    greenTime = newItem.greenTime,
                    yellowTime = newItem.yellowTime,
                    redTime = newItem.redTime
                )
                Log.d("AgendaTableFragment", "Adding new item: $item")
                adapter.addItem(item)
                binding.agendaRecyclerView.smoothScrollToPosition(adapter.itemCount - 1)
            }).show(parentFragmentManager, "AgendaItemDialog")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}