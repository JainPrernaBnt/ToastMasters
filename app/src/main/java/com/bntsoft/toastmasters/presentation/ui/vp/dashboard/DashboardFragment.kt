package com.bntsoft.toastmasters.presentation.ui.vp.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.TableTopicSpeaker
import com.bntsoft.toastmasters.data.model.Winner
import com.bntsoft.toastmasters.databinding.DialogSelectWinnersBinding
import com.bntsoft.toastmasters.databinding.DialogTableTopicSpeakersBinding
import com.bntsoft.toastmasters.databinding.FragmentDashboardBinding
import com.bntsoft.toastmasters.domain.models.WinnerCategory
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter.MeetingAdapter
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.adapter.TableTopicSpeakersAdapter
import com.bntsoft.toastmasters.presentation.ui.vp.dashboard.viewmodel.DashboardViewModel
import com.bntsoft.toastmasters.utils.Constants.AGENDA_COLLECTION
import com.bntsoft.toastmasters.utils.Constants.AGENDA_ITEMS_COLLECTION
import com.bntsoft.toastmasters.utils.Constants.EXTRA_MEETING_ID
import com.bntsoft.toastmasters.utils.Constants.MEETINGS_COLLECTION
import com.bntsoft.toastmasters.utils.Constants.TABLE_TOPICS_COLLECTION
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// Timber import removed, using Android Log instead

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var meetingAdapter: MeetingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        setupRecyclerView()
        observeUpcomingMeetings()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("DashboardDebug", "onViewCreated: Fragment view created")
        Log.d("DashboardDebug", "ViewModel: $viewModel")
        Log.d("DashboardDebug", "Adapter initialized: ${::meetingAdapter.isInitialized}")

        // Set up RecyclerView if not already done
        if (!::meetingAdapter.isInitialized) {
            setupRecyclerView()
        }

        // Force refresh data
        Log.d("DashboardDebug", "Loading upcoming meetings...")
        viewModel.loadUpcomingMeetings()
    }

    private var isDialogShowing = false
    private var currentMeetingId: String? = null

    override fun onResume() {
        super.onResume()
        viewModel.loadUpcomingMeetings()
        // Reset dialog state when coming back to this fragment
        isDialogShowing = false
        currentMeetingId = null
    }

    private fun setupRecyclerView() {
        meetingAdapter = MeetingAdapter(
            onEdit = { meetingId ->
                val bundle = bundleOf("meeting_id" to meetingId)
                findNavController().navigate(
                    R.id.action_dashboardFragment_to_editMeetingFragment,
                    bundle
                )
            },
            onDelete = { meetingId ->
                showDeleteConfirmationDialog(meetingId)
            },
            onComplete = { meetingId ->
                showCompleteConfirmationDialog(meetingId)
            },
            onItemClick = { meetingId ->
                findNavController().navigate(
                    R.id.action_dashboardFragment_to_memberResponseFragment,
                    bundleOf(EXTRA_MEETING_ID to meetingId)
                )
            }
        )
        binding.rvMeetings.apply {
            adapter = meetingAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeUpcomingMeetings() {
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("DashboardDebug", "Starting to observe upcoming meetings")
            viewModel.upcomingMeetingsStateWithCounts.collect { state ->
                Log.d("DashboardDebug", "Received state: ${state.javaClass.simpleName}")
                val message = when (state) {
                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Success -> {
                        Log.d(
                            "DashboardDebug",
                            "Received ${state.meetings.size} meetings from ViewModel"
                        )
                        if (state.meetings.isEmpty()) {
                            Log.d("DashboardDebug", "No meetings received from ViewModel")
                        } else {
                            state.meetings.forEachIndexed { index, meeting ->
                                Log.d(
                                    "DashboardDebug",
                                    "Meeting #${index + 1}: ${meeting.meeting.theme} (${meeting.meeting.id}) - " +
                                            "Status: ${meeting.meeting.status}, " +
                                            "Date: ${meeting.meeting.dateTime}, " +
                                            "Available: ${meeting.availableCount}, " +
                                            "Not Available: ${meeting.notAvailableCount}"
                                )
                            }
                        }
                        meetingAdapter.submitList(state.meetings) {
                            Log.d(
                                "DashboardDebug",
                                "Adapter updated with ${state.meetings.size} meetings"
                            )
                            Log.d(
                                "DashboardDebug",
                                "RecyclerView child count: ${binding.rvMeetings.childCount}"
                            )
                            Log.d(
                                "DashboardDebug",
                                "RecyclerView adapter item count: ${meetingAdapter.itemCount}"
                            )
                        }
                        "Loaded ${state.meetings.size} meetings"
                    }

                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Empty -> {
                        meetingAdapter.submitList(emptyList())
                        "No upcoming meetings found"
                    }

                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Error -> {
                        "Error: ${state.message}"
                    }

                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Loading -> {
                        "Loading meetings..."
                    }
                }

                // Log to console for debugging
                Log.d("DashboardDebug", message)
                if (state is DashboardViewModel.UpcomingMeetingsStateWithCounts.Success) {
                    state.meetings.forEach { meeting ->
                        Log.d(
                            "DashboardDebug",
                            "Meeting: ${meeting.meeting.theme}, " +
                                    "Available: ${meeting.availableCount}, " +
                                    "Not Available: ${meeting.notAvailableCount}, " +
                                    "Not Confirmed: ${meeting.notConfirmedCount}"
                        )
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(meetingId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Meeting")
            .setMessage("Are you sure you want to delete this meeting?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.deleteMeeting(meetingId)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCompleteConfirmationDialog(meetingId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Complete Meeting")
            .setMessage("Mark this meeting as completed? This action cannot be undone.")
            .setPositiveButton("Continue") { _, _ ->
                showTableTopicsDialog(meetingId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTableTopicsDialog(meetingId: String) {
        val dialogView = DialogTableTopicSpeakersBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView.root)
            .setCancelable(false)
            .create()

        val speakersAdapter = TableTopicSpeakersAdapter { speaker ->
            // Handle delete if needed
        }

        dialogView.rvSpeakers.apply {
            adapter = speakersAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        }

        dialogView.btnAddSpeaker.setOnClickListener {
            val name = dialogView.etSpeakerName.text.toString().trim()
            val topic = dialogView.etTopic.text.toString().trim()

            if (name.isBlank()) {
                // Show error
                return@setOnClickListener
            }

            val speaker = TableTopicSpeaker(
                meetingId = meetingId,
                speakerName = name,
                topic = topic.ifBlank { "" }
            )
            speakersAdapter.addSpeaker(speaker)

            // Clear inputs
            dialogView.etSpeakerName.text?.clear()
            dialogView.etTopic.text?.clear()
        }

        dialogView.btnNext.setOnClickListener {
            val speakers = speakersAdapter.getSpeakers()
            if (speakers.isNotEmpty()) {
                saveTableTopics(meetingId, speakers) {
                    dialog.dismiss()
                    showSelectWinnersDialog(meetingId)
                }
            } else {
                showSelectWinnersDialog(meetingId)
                dialog.dismiss()
            }
        }

        dialogView.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveTableTopics(
        meetingId: String,
        speakers: List<TableTopicSpeaker>,
        onComplete: () -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        val topicsRef = db.collection(MEETINGS_COLLECTION)
            .document(meetingId)
            .collection(TABLE_TOPICS_COLLECTION)

        // Clear existing topics
        topicsRef.get().addOnSuccessListener { snapshot ->
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            // Add new topics
            speakers.forEach { speaker ->
                val docRef = topicsRef.document()
                batch.set(docRef, speaker.copy(id = docRef.id))
            }

            batch.commit()
                .addOnSuccessListener { onComplete() }
                .addOnFailureListener { e ->
                    // Handle error
                    onComplete()
                }
        }
    }

    private fun showSelectWinnersDialog(meetingId: String) {
        val dialogView = DialogSelectWinnersBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView.root)
            .setCancelable(false)
            .create()

        // First, load presenters and table topics speakers
        loadPresentersAndSpeakers(meetingId) { presenters, tableTopicSpeakers ->
            val allParticipants = (presenters + tableTopicSpeakers).distinct()

            // Setup dropdowns with the combined list
            setupWinnerDropdown(dialogView.bestSpeakerInput, allParticipants)
            setupWinnerDropdown(dialogView.bestEvaluatorInput, allParticipants)
            setupWinnerDropdown(dialogView.bestTableTopicsInput, allParticipants)
            setupWinnerDropdown(dialogView.bestMainRoleInput, allParticipants)
            setupWinnerDropdown(dialogView.bestAuxRoleInput, allParticipants)
        }

        dialogView.btnSaveWinners.setOnClickListener {
            val winners = listOf(
                createWinner(meetingId, dialogView.bestSpeakerInput, WinnerCategory.BEST_SPEAKER),
                createWinner(
                    meetingId,
                    dialogView.bestEvaluatorInput,
                    WinnerCategory.BEST_EVALUATOR
                ),
                createWinner(
                    meetingId,
                    dialogView.bestTableTopicsInput,
                    WinnerCategory.BEST_TABLE_TOPICS
                ),
                createWinner(
                    meetingId,
                    dialogView.bestMainRoleInput,
                    WinnerCategory.BEST_MAIN_ROLE
                ),
                createWinner(meetingId, dialogView.bestAuxRoleInput, WinnerCategory.BEST_AUX_ROLE)
            )

            if (winners.all { it.memberName != null || it.guestName != null }) {
                saveWinners(
                    meetingId,
                    winners.filter { it.memberName != null || it.guestName != null })
                dialog.dismiss()
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.completeMeeting(meetingId)
                }
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Incomplete Information")
                    .setMessage("Please select winners for all categories or leave them empty if not applicable.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        dialog.show()

        dialogView.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun loadPresentersAndSpeakers(
        meetingId: String,
        onComplete: (List<String>, List<String>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val presenters = mutableListOf<String>()
        val tableTopicSpeakers = mutableListOf<String>()

        // Load presenters from agenda items
        db.collection(MEETINGS_COLLECTION)
            .document(meetingId)
            .collection(AGENDA_COLLECTION)
            .document(meetingId)  // Use meetingId as the document ID
            .collection(AGENDA_ITEMS_COLLECTION)
            .get()
            .addOnSuccessListener { itemsSnapshot ->
                if (itemsSnapshot.isEmpty) {
                    Log.d("WinnersDebug", "No agenda items found for meeting: $meetingId")
                } else {
                    itemsSnapshot.documents.forEach { item ->
                        item.getString("presenter")?.let { name ->
                            if (name.isNotBlank()) {
                                presenters.add(name)
                                Log.d("WinnersDebug", "Presenter fetched: $name")
                            } else {
                                Log.d(
                                    "WinnersDebug",
                                    "Empty or null presenter name found in document: ${item.id}"
                                )
                            }
                        } ?: run {
                            Log.d(
                                "WinnersDebug",
                                "No 'presenter' field found in document: ${item.id}"
                            )
                        }
                    }
                }

                // Load table topic speakers
                db.collection(MEETINGS_COLLECTION)
                    .document(meetingId)
                    .collection(TABLE_TOPICS_COLLECTION)
                    .get()
                    .addOnSuccessListener { topicsSnapshot ->
                        topicsSnapshot.documents.forEach { doc ->
                            doc.getString("speakerName")?.let { name ->
                                if (name.isNotBlank()) tableTopicSpeakers.add(name)
                                Log.d("WinnersDebug", "Table Topic speaker added: $name")
                            }
                        }
                        Log.d(
                            "WinnersDebug",
                            "All presenters: ${presenters.distinct()}, Speakers: ${tableTopicSpeakers.distinct()}"
                        )
                        onComplete(presenters.distinct(), tableTopicSpeakers.distinct())
                    }
            }
            .addOnFailureListener { e ->
                Log.e("WinnersDebug", "Error loading agenda items", e)
                // Still try to load table topic speakers even if agenda items fail
                loadTableTopicSpeakers(meetingId, emptyList(), onComplete)
            }


    }

    private fun loadTableTopicSpeakers(
        meetingId: String,
        presenters: List<String>,
        onComplete: (List<String>, List<String>) -> Unit
    ) {
        val tableTopicSpeakers = mutableListOf<String>()
        FirebaseFirestore.getInstance()
            .collection(MEETINGS_COLLECTION)
            .document(meetingId)
            .collection(TABLE_TOPICS_COLLECTION)
            .get()
            .addOnSuccessListener { topicsSnapshot ->
                topicsSnapshot.documents.forEach { doc ->
                    doc.getString("speakerName")?.let { name ->
                        if (name.isNotBlank()) tableTopicSpeakers.add(name)
                        Log.d("WinnersDebug", "Table Topic speaker added: $name")
                    }
                }
                Log.d(
                    "WinnersDebug",
                    "All presenters: $presenters, Speakers: ${tableTopicSpeakers.distinct()}"
                )
                onComplete(presenters, tableTopicSpeakers.distinct())
            }
            .addOnFailureListener { e ->
                Log.e("WinnersDebug", "Error loading table topic speakers", e)
                onComplete(presenters, emptyList())
            }
    }

    private fun setupWinnerDropdown(
        autoCompleteTextView: android.widget.AutoCompleteTextView,
        participants: List<String> = emptyList()
    ) {
        Log.d("WinnersDebug", "Setting up dropdown with participants: $participants")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            participants
        )
        autoCompleteTextView.setAdapter(adapter)
    }

    private fun createWinner(
        meetingId: String,
        input: android.widget.AutoCompleteTextView,
        category: WinnerCategory
    ): Winner {
        val selectedName = input.text.toString()
        return if (selectedName.isNotEmpty()) {
            // In a real app, you would get the user ID from your database
            Winner(
                meetingId = meetingId,
                category = category,
                isMember = true, // Assuming all selected are members for now
                memberName = selectedName,
                guestName = null
            )
        } else {
            Winner(meetingId = meetingId, category = category)
        }
    }

    private fun saveWinners(meetingId: String, winners: List<Winner>) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        val winnersRef = db.collection("meetings").document(meetingId).collection("winners")

        // Delete existing winners first
        winnersRef.get().addOnSuccessListener { snapshot ->
            winners.forEach { winner ->
                val winnerRef = winnersRef.document()
                batch.set(winnerRef, winner)
            }
            // Commit the batch
            batch.commit()
                .addOnSuccessListener {
                    // Show success message
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Success")
                        .setMessage("Winners saved successfully!")
                        .setPositiveButton("OK", null)
                        .show()
                }
                .addOnFailureListener { e ->
                    // Show error message
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Error")
                        .setMessage("Failed to save winners: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("DashboardFragment", "onDestroyView called")
        _binding = null
    }
}