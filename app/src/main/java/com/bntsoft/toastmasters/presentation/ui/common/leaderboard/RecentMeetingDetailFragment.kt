package com.bntsoft.toastmasters.presentation.ui.common.leaderboard

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.data.model.Winner
import com.bntsoft.toastmasters.databinding.DialogSelectWinnersBinding
import com.bntsoft.toastmasters.databinding.FragmentRecentMeetingDetailBinding
import com.bntsoft.toastmasters.domain.models.MeetingStatus
import com.bntsoft.toastmasters.domain.models.UserRole
import com.bntsoft.toastmasters.domain.models.WinnerCategory
import com.bntsoft.toastmasters.utils.Constants.AGENDA_COLLECTION
import com.bntsoft.toastmasters.utils.Constants.AGENDA_ITEMS_COLLECTION
import com.bntsoft.toastmasters.utils.Constants.MEETINGS_COLLECTION
import com.bntsoft.toastmasters.utils.Constants.TABLE_TOPICS_COLLECTION
import com.bntsoft.toastmasters.utils.Constants.WINNERS_COLLECTION
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.bntsoft.toastmasters.presentation.ui.common.leaderboard.adapter.WinnerAdapter
import com.google.firebase.Timestamp

@AndroidEntryPoint
class RecentMeetingDetailFragment : Fragment() {

    private var _binding: FragmentRecentMeetingDetailBinding? = null
    private val binding get() = _binding!!
    private val winnersAdapter = WinnerAdapter()

    @Inject
    lateinit var firestore: FirebaseFirestore
    
    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private var currentMeetingId: String = ""
    private var currentUserRole: UserRole = UserRole.MEMBER
    private var currentWinners: List<Winner> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentMeetingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadCurrentUserRole()
        setupEditButton()
        loadRecentMeetingData()
    }

    private fun setupRecyclerView() {
        binding.winnersRecyclerView.apply {
            adapter = winnersAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadCurrentUserRole() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role")
                    currentUserRole = if (role == "VP_EDUCATION") {
                        UserRole.VP_EDUCATION
                    } else {
                        UserRole.MEMBER
                    }
                    updateEditButtonVisibility()
                }
                .addOnFailureListener {
                    currentUserRole = UserRole.MEMBER
                    updateEditButtonVisibility()
                }
        } else {
            currentUserRole = UserRole.MEMBER
            updateEditButtonVisibility()
        }
    }

    private fun setupEditButton() {
        binding.btnEditWinners.setOnClickListener {
            showEditWinnersDialog()
        }
    }

    private fun updateEditButtonVisibility() {
        binding.btnEditWinners.visibility = if (currentUserRole == UserRole.VP_EDUCATION && currentMeetingId.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun formatTimeTo12Hour(time24: String): String {
        return try {
            val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = sdf24.parse(time24)
            val sdf12 = SimpleDateFormat("h:mm a", Locale.getDefault())
            date?.let { sdf12.format(it) } ?: time24
        } catch (e: Exception) {
            time24
        }
    }

    private fun loadRecentMeetingData() {
        showLoading()

        firestore.collection(MEETINGS_COLLECTION)
            .whereEqualTo("status", MeetingStatus.COMPLETED)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val recentMeeting = snapshot.documents.firstOrNull()

                if (recentMeeting == null) {
                    showEmptyState("No recent meetings found.")
                    return@addOnSuccessListener
                }

                val meetingTheme = recentMeeting.getString("theme") ?: "No Theme"
                val meetingDate = recentMeeting.getString("date") ?: "Date not available"
                val meetingVenue = recentMeeting.getString("venue") ?: "Venue not specified"
                val startTime = recentMeeting.getString("startTime") ?: "--:--"
                val endTime = recentMeeting.getString("endTime") ?: "--:--"

                val formattedStartTime = formatTimeTo12Hour(startTime)
                val formattedEndTime = formatTimeTo12Hour(endTime)

                val meetingId = recentMeeting.id
                currentMeetingId = meetingId
                binding.meetingTitle.text = "Theme: $meetingTheme"
                val formattedDate = meetingDate.let {
                    try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
                        parsed?.let { d -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(d) }
                    } catch (e: Exception) {
                        meetingDate
                    }
                }

                binding.meetingDate.text = "Date: $formattedDate"

                binding.meetingVenue.text = "Venue: $meetingVenue"

                binding.meetingTime.text = "Time: $formattedStartTime - $formattedEndTime"

                updateEditButtonVisibility()
                loadMeetingWinners(meetingId)
            }
            .addOnFailureListener { e ->
                Log.e("RecentMeetingDetail", "Error loading recent meeting", e)
                showErrorState("Error loading meeting. Please try again.")
            }
    }

    private fun loadMeetingWinners(meetingId: String) {
        firestore.collection(MEETINGS_COLLECTION)
            .document(meetingId)
            .collection(WINNERS_COLLECTION)
            .get()
            .addOnSuccessListener { snapshot ->
                currentWinners = snapshot.toObjects(Winner::class.java)
                    .sortedBy { it.category.ordinal }

                if (currentWinners.isEmpty()) {
                    showEmptyState("No winners recorded for this meeting.")
                } else {
                    winnersAdapter.submitList(currentWinners)
                    showContent()
                }
            }
            .addOnFailureListener { e ->
                Log.e("RecentMeetingDetail", "Error loading winners", e)
                showErrorState("Failed to load winners.")
            }
    }

    private fun showEditWinnersDialog() {
        if (currentMeetingId.isEmpty()) return

        val dialogView = DialogSelectWinnersBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView.root)
            .setCancelable(false)
            .create()

        // Show progress while loading data
        dialogView.btnSaveWinners.isEnabled = false
        dialogView.btnSaveWinners.text = "Loading..."

        // Load presenters and speakers for dropdown
        loadPresentersAndSpeakersForEdit(currentMeetingId) { presenters, tableTopicSpeakers ->
            val allParticipants = (presenters + tableTopicSpeakers).distinct()

            // Setup dropdowns
            setupWinnerDropdownForEdit(dialogView.bestSpeakerInput, allParticipants, WinnerCategory.BEST_SPEAKER)
            setupWinnerDropdownForEdit(dialogView.bestEvaluatorInput, allParticipants, WinnerCategory.BEST_EVALUATOR)
            setupWinnerDropdownForEdit(dialogView.bestTableTopicsInput, allParticipants, WinnerCategory.BEST_TABLE_TOPICS)
            setupWinnerDropdownForEdit(dialogView.bestMainRoleInput, allParticipants, WinnerCategory.BEST_MAIN_ROLE)
            setupWinnerDropdownForEdit(dialogView.bestAuxRoleInput, allParticipants, WinnerCategory.BEST_AUX_ROLE)

            // Enable save button
            dialogView.btnSaveWinners.isEnabled = true
            dialogView.btnSaveWinners.text = "Save Winners"
        }

        dialogView.btnSaveWinners.setOnClickListener {
            val winners = listOf(
                createWinnerForEdit(dialogView.bestSpeakerInput, WinnerCategory.BEST_SPEAKER),
                createWinnerForEdit(dialogView.bestEvaluatorInput, WinnerCategory.BEST_EVALUATOR),
                createWinnerForEdit(dialogView.bestTableTopicsInput, WinnerCategory.BEST_TABLE_TOPICS),
                createWinnerForEdit(dialogView.bestMainRoleInput, WinnerCategory.BEST_MAIN_ROLE),
                createWinnerForEdit(dialogView.bestAuxRoleInput, WinnerCategory.BEST_AUX_ROLE)
            )

            val validWinners = winners.filter { it.memberName != null || it.guestName != null }
            
            if (validWinners.isNotEmpty()) {
                saveEditedWinners(validWinners, dialog)
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Incomplete Information")
                    .setMessage("Please select at least one winner or cancel.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        dialogView.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadPresentersAndSpeakersForEdit(
        meetingId: String,
        onComplete: (List<String>, List<String>) -> Unit
    ) {
        val presenters = mutableListOf<String>()
        val tableTopicSpeakers = mutableListOf<String>()

        // Load presenters from agenda items
        firestore.collection(MEETINGS_COLLECTION)
            .document(meetingId)
            .collection(AGENDA_COLLECTION)
            .document(meetingId)
            .collection(AGENDA_ITEMS_COLLECTION)
            .get()
            .addOnSuccessListener { itemsSnapshot ->
                itemsSnapshot.documents.forEach { item ->
                    item.getString("presenter")?.let { name ->
                        if (name.isNotBlank()) {
                            presenters.add(name)
                        }
                    }
                }

                // Load table topic speakers
                firestore.collection(MEETINGS_COLLECTION)
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
            .addOnFailureListener {
                onComplete(emptyList(), emptyList())
            }
    }

    private fun setupWinnerDropdownForEdit(
        autoCompleteTextView: AutoCompleteTextView,
        participants: List<String>,
        category: WinnerCategory
    ) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            participants
        )
        autoCompleteTextView.setAdapter(adapter)
        
        // Pre-fill with existing winner
        val existingWinner = currentWinners.find { it.category == category }
        existingWinner?.let { winner ->
            autoCompleteTextView.setText(winner.memberName ?: winner.guestName ?: "", false)
        }
    }

    private fun createWinnerForEdit(
        input: AutoCompleteTextView,
        category: WinnerCategory
    ): Winner {
        val selectedName = input.text.toString()
        return if (selectedName.isNotEmpty()) {
            Winner(
                meetingId = currentMeetingId,
                category = category,
                isMember = true, // Assuming all selected are members for now
                memberName = selectedName,
                guestName = null
            )
        } else {
            Winner(meetingId = currentMeetingId, category = category)
        }
    }

    private fun saveEditedWinners(winners: List<Winner>, dialog: AlertDialog) {
        // Show progress
        dialog.setCancelable(false)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = false

        val db = firestore
        val batch = db.batch()
        val winnersRef = db.collection(MEETINGS_COLLECTION)
            .document(currentMeetingId)
            .collection(WINNERS_COLLECTION)
        val meetingRef = db.collection(MEETINGS_COLLECTION).document(currentMeetingId)

        // Clear existing winners first
        winnersRef.get().addOnSuccessListener { snapshot ->
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            val now = Timestamp.now()

            // Add new winners
            winners.forEach { winner ->
                val winnerRef = winnersRef.document()
                val winnerWithTimestamps = winner.copy(
                    id = winnerRef.id,
                    meetingId = currentMeetingId,
                    createdAt = now,
                    updatedAt = now
                )
                batch.set(winnerRef, winnerWithTimestamps)
            }

            // Update the parent meeting's updatedAt
            batch.update(meetingRef, "updatedAt", now)

            // Commit the batch
            batch.commit()
                .addOnSuccessListener {
                    dialog.dismiss()
                    // Refresh the winners list
                    loadMeetingWinners(currentMeetingId)
                    // Show success message
                    Snackbar.make(binding.root, "Winners updated successfully!", Snackbar.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    dialog.setCancelable(true)
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
                    dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.isEnabled = true
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Error")
                        .setMessage("Failed to update winners: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
        }
    }

    // --- UI State Helpers ---

    private fun showLoading() {
        binding.apply {
            progressBar.visibility = View.VISIBLE
            contentLayout.visibility = View.GONE
            emptyStateText.visibility = View.GONE
        }
    }

    private fun showContent() {
        binding.apply {
            progressBar.visibility = View.GONE
            contentLayout.visibility = View.VISIBLE
            emptyStateText.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        binding.apply {
            progressBar.visibility = View.GONE
            contentLayout.visibility = View.GONE
            emptyStateText.visibility = View.VISIBLE
            emptyStateText.text = message
        }
    }

    private fun showErrorState(message: String) = showEmptyState(message)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

