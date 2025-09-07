package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.databinding.FragmentAgendaTableBinding

class AgendaTableFragment : Fragment(), OnStartDragListener {

    private var _binding: FragmentAgendaTableBinding? = null
    private val binding get() = _binding!!
    private lateinit var meetingId: String
    private lateinit var adapter: AgendaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Uncomment and use when needed
        // arguments?.let {
        //     meetingId = it.getString(ARG_MEETING_ID) ?: throw IllegalStateException("meetingId is required")
        // } ?: throw IllegalStateException("meetingId argument is required")
    }

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
        setupRecyclerView()
        setupClickListeners()
    }

    private lateinit var itemTouchHelper: ItemTouchHelper

    private fun setupRecyclerView() {
        adapter = AgendaAdapter()
        adapter.setOnStartDragListener(this)
        
        // Set up item added listener
        adapter.onItemAdded = { position ->
            val newItem = AgendaItem()
            adapter.addItemAtPosition(newItem, position)
            binding.agendaRecyclerView.smoothScrollToPosition(position)
        }
        
        // Set up ItemTouchHelper
        val callback = SimpleItemTouchHelperCallback(adapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.agendaRecyclerView)
        
        binding.agendaRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@AgendaTableFragment.adapter
            setHasFixedSize(true)
        }
    }
    
    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    private fun setupClickListeners() {
        binding.addAgendaItemButton.setOnClickListener {
            adapter.addItem(AgendaItem())
            // Scroll to the bottom when a new item is added
            binding.agendaRecyclerView.smoothScrollToPosition(adapter.itemCount - 1)
        }

        binding.saveButton.setOnClickListener {
            // Handle save button click
            val agendaItems = adapter.getItems()
            // TODO: Save the agenda items
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        // Uncomment when needed
        // private const val ARG_MEETING_ID = "meetingId"
        //
        // @JvmStatic
        // fun newInstance(meetingId: String) =
        //     AgendaTableFragment().apply {
        //         arguments = Bundle().apply {
        //             putString(ARG_MEETING_ID, meetingId)
        //         }
        //     }
    }
}