package com.bntsoft.toastmasters.presentation.ui.vp.dashboard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.animation.doOnEnd
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.TableTopicSpeaker
import com.bntsoft.toastmasters.data.model.Winner
import com.bntsoft.toastmasters.databinding.DialogSelectWinnersBinding
import com.bntsoft.toastmasters.databinding.DialogTableTopicSpeakersBinding
import com.bntsoft.toastmasters.databinding.FragmentDashboardBinding
import com.bntsoft.toastmasters.domain.model.MeetingWithCounts
import com.bntsoft.toastmasters.domain.models.MeetingStatus
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
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var upcomingMeetingsAdapter: MeetingAdapter
    private lateinit var pastMeetingsAdapter: MeetingAdapter

    private val searchQuery = MutableStateFlow("")
    private var currentMeetings = listOf<MeetingWithCounts>()

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

    private var isUpcomingExpanded = true
    private var isPastExpanded = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up RecyclerView if not already done
        if (!::upcomingMeetingsAdapter.isInitialized || !::pastMeetingsAdapter.isInitialized) {
            setupRecyclerView()
        }

        setupSearch()
        setupSectionToggles()
        // Force refresh data
        viewModel.loadUpcomingMeetings()
    }
    private fun setupSectionToggles() {
        binding.headerUpcoming.setOnClickListener {
            isUpcomingExpanded = !isUpcomingExpanded
            toggleSection(binding.rvUpcomingMeetings, binding.ivUpcomingArrow, isUpcomingExpanded)
        }

        binding.headerPast.setOnClickListener {
            isPastExpanded = !isPastExpanded
            toggleSection(binding.rvPastMeetings, binding.ivPastArrow, isPastExpanded)
        }
    }

    private fun toggleSection(
        recyclerView: RecyclerView,
        arrowView: ImageView,
        isExpanded: Boolean
    ) {
        val startHeight = recyclerView.height

        if (isExpanded) {
            // EXPAND
            recyclerView.measure(
                View.MeasureSpec.makeMeasureSpec(recyclerView.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val targetHeight = recyclerView.measuredHeight

            recyclerView.visibility = View.VISIBLE

            val animator = ValueAnimator.ofInt(0, targetHeight)
            animator.addUpdateListener { valueAnimator ->
                val layoutParams = recyclerView.layoutParams
                layoutParams.height = valueAnimator.animatedValue as Int
                recyclerView.layoutParams = layoutParams
            }
            animator.duration = 250
            animator.start()

        } else {
            // COLLAPSE
            val animator = ValueAnimator.ofInt(startHeight, 0)
            animator.addUpdateListener { valueAnimator ->
                val layoutParams = recyclerView.layoutParams
                layoutParams.height = valueAnimator.animatedValue as Int
                recyclerView.layoutParams = layoutParams
            }
            animator.duration = 250
            animator.doOnEnd {
                recyclerView.visibility = View.GONE
                recyclerView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            animator.start()
        }

        // Rotate arrow
        val rotation = if (isExpanded) 0f else 180f
        arrowView.animate()
            .rotation(rotation)
            .setDuration(200)
            .start()
    }

    private fun setupRecyclerView() {
        // Upcoming Meetings Adapter (with edit option)
        upcomingMeetingsAdapter = MeetingAdapter(
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
            },
            showOverflowMenu = true
        )

        // Past Meetings Adapter (without edit option)
        pastMeetingsAdapter = MeetingAdapter(
            onEdit = { /* No edit for past meetings */ },
            onDelete = { meetingId ->
                showDeleteConfirmationDialog(meetingId)
            },
            onComplete = { /* No complete action for past meetings */ },
            onItemClick = { meetingId ->
                findNavController().navigate(
                    R.id.action_dashboardFragment_to_memberResponseFragment,
                    bundleOf(EXTRA_MEETING_ID to meetingId)
                )
            },
            showOverflowMenu = false
        )

        binding.rvUpcomingMeetings.apply {
            adapter = upcomingMeetingsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }

        binding.rvPastMeetings.apply {
            adapter = pastMeetingsAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    private fun observeUpcomingMeetings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.upcomingMeetingsStateWithCounts.collect { state ->
                when (state) {
                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Success -> {
                        currentMeetings = state.meetings
                        filterAndSortMeetings(searchQuery.value)
                    }

                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Empty -> {
                        currentMeetings = emptyList()
                        upcomingMeetingsAdapter.submitList(emptyList())
                        pastMeetingsAdapter.submitList(emptyList())

                        // Also hide the sections
                        binding.tvUpcomingMeetings.visibility = View.VISIBLE
                        binding.rvUpcomingMeetings.visibility = View.GONE
                        binding.tvPastMeetings.visibility = View.VISIBLE
                        binding.rvPastMeetings.visibility = View.GONE
                    }

                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Error -> {
                        // Handle error
                        Log.e("DashboardFragment", "Error loading meetings: ${state.message}")
                    }

                    is DashboardViewModel.UpcomingMeetingsStateWithCounts.Loading -> {
                        // Show loading state if needed
                    }
                }
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery.value = s?.toString() ?: ""
            }
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard
                val imm =
                    context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                true
            } else {
                false
            }
        }

        // Debounce search to avoid too many updates
        viewLifecycleOwner.lifecycleScope.launch {
            searchQuery
                .debounce(300)
                .collect { query ->
                    filterAndSortMeetings(query)
                }
        }
    }

    private fun filterAndSortMeetings(query: String) {
        val filtered = if (query.isBlank()) {
            currentMeetings
        } else {
            currentMeetings.filter { meetingWithCounts ->
                val meeting = meetingWithCounts.meeting
                meeting.theme.contains(query, ignoreCase = true) ||
                        meeting.location.contains(query, ignoreCase = true) ||
                        formatDate(meeting.dateTime).contains(query, ignoreCase = true)
            }
        }

        // Separate meetings by status and date
        val now = LocalDateTime.now()
        val upcomingMeetings = filtered.filter { 
            it.meeting.status == MeetingStatus.NOT_COMPLETED && 
            !it.meeting.dateTime.isBefore(now.minusDays(1)) // Include meetings from yesterday to catch any in-progress meetings
        }.sortedBy { it.meeting.dateTime } // Sort upcoming meetings in ascending order (earliest first)

        val pastMeetings = filtered.filter { 
            it.meeting.status == MeetingStatus.COMPLETED || 
            it.meeting.dateTime.isBefore(now.minusDays(1)) // Include any meetings older than yesterday as past
        }.sortedByDescending { it.meeting.dateTime } // Sort past meetings in descending order (newest first)

        Log.d("DashboardFragment", "Upcoming count=${upcomingMeetings.size}, Past count=${pastMeetings.size}")

        // Update UI based on whether there are any meetings to show
        if (upcomingMeetings.isNotEmpty()) {
            binding.tvUpcomingMeetings.visibility = View.VISIBLE
            binding.rvUpcomingMeetings.visibility = View.VISIBLE
            upcomingMeetingsAdapter.submitList(upcomingMeetings)
        } else {
            binding.tvUpcomingMeetings.visibility = View.GONE
            binding.rvUpcomingMeetings.visibility = View.GONE
        }

        if (pastMeetings.isNotEmpty()) {
            binding.tvPastMeetings.visibility = View.VISIBLE
            binding.rvPastMeetings.visibility = View.VISIBLE
            pastMeetingsAdapter.submitList(pastMeetings)
        } else {
            binding.tvPastMeetings.visibility = View.GONE
            binding.rvPastMeetings.visibility = View.GONE
        }
        binding.tvPastMeetings.visibility =
            if (pastMeetings.isEmpty()) View.GONE else View.VISIBLE
        binding.rvPastMeetings.visibility =
            if (pastMeetings.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun formatDate(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
        return dateTime.format(formatter)
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
                } else {
                    itemsSnapshot.documents.forEach { item ->
                        item.getString("presenter")?.let { name ->
                            if (name.isNotBlank()) {
                                presenters.add(name)
                            } else {

                            }
                        } ?: run {

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
                            }
                        }

                        onComplete(presenters.distinct(), tableTopicSpeakers.distinct())
                    }
            }
            .addOnFailureListener { e ->
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
                    }
                }

                onComplete(presenters, tableTopicSpeakers.distinct())
            }
            .addOnFailureListener { e ->
                onComplete(presenters, emptyList())
            }
    }

    private fun setupWinnerDropdown(
        autoCompleteTextView: android.widget.AutoCompleteTextView,
        participants: List<String> = emptyList()
    ) {
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
        _binding = null
    }
}