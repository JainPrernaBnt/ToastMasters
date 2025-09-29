package com.bntsoft.toastmasters.presentation.ui.common.leaderboard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.data.model.MeetingWithWinners
import com.bntsoft.toastmasters.data.model.Winner
import com.bntsoft.toastmasters.databinding.FragmentPastMeetingsBinding
import com.bntsoft.toastmasters.presentation.ui.common.leaderboard.adapter.LeaderboardMeetingAdapter
import com.bntsoft.toastmasters.utils.Constants.MEETINGS_COLLECTION
import com.bntsoft.toastmasters.utils.Constants.WINNERS_COLLECTION
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PastMeetingsFragment : Fragment() {

    private var _binding: FragmentPastMeetingsBinding? = null
    private val binding get() = _binding!!
    private val meetingsAdapter = LeaderboardMeetingAdapter()
    private var allMeetingsWithWinners = mutableListOf<MeetingWithWinners>()

    @Inject
    lateinit var firestore: FirebaseFirestore

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPastMeetingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        loadPastMeetingsData()
    }

    private fun setupRecyclerView() {
        binding.meetingsRecyclerView.apply {
            adapter = meetingsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterMeetings(s?.toString() ?: "")
            }
        })

        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text?.clear()
        }
    }

    private fun loadPastMeetingsData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE

        firestore.collection(MEETINGS_COLLECTION)
            .whereEqualTo("status", "COMPLETED")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { meetingsSnapshot ->
                val meetings = meetingsSnapshot.documents

                if (meetings.isEmpty()) {
                    showEmptyState(true)
                    return@addOnSuccessListener
                }

                val pastMeetings = if (meetings.size > 1) {
                    meetings.drop(1)
                } else {
                    emptyList()
                }

                if (pastMeetings.isEmpty()) {
                    showEmptyState(true)
                    return@addOnSuccessListener
                }

                loadPastMeetingsWinners(pastMeetings)
            }
            .addOnFailureListener { e ->
                Log.e("PastMeetings", "Error loading past meetings", e)
                showErrorState()
            }
    }

    private fun loadPastMeetingsWinners(meetingDocuments: List<DocumentSnapshot>) {
        val tasks = meetingDocuments.map { meetingDocument ->
            val meetingId = meetingDocument.id
            firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection(WINNERS_COLLECTION)
                .get()
                .continueWith { task ->
                    val winners = if (task.isSuccessful) {
                        task.result?.toObjects(Winner::class.java) ?: emptyList()
                    } else emptyList()

                    // Build a MeetingWithWinners object (only if winners exist)
                    if (winners.isNotEmpty()) {
                        val createdAtTimestamp = when (val value = meetingDocument.get("createdAt")) {
                            is com.google.firebase.Timestamp -> value
                            is Long -> com.google.firebase.Timestamp(Date(value))
                            else -> null
                        }

                        val updatedAtTimestamp = when (val value = meetingDocument.get("updatedAt")) {
                            is com.google.firebase.Timestamp -> value
                            is Long -> com.google.firebase.Timestamp(Date(value))
                            else -> null
                        }

                        MeetingWithWinners(
                            meetingId = meetingId,
                            date = dateFormat.format(
                                meetingDocument.getTimestamp("dateTime")?.toDate() ?: Date()
                            ),
                            theme = meetingDocument.getString("theme") ?: "",
                            winners = winners.sortedBy { it.category.ordinal }, // Optional: sort winners by category
                            createdAt = createdAtTimestamp,
                            updatedAt = updatedAtTimestamp
                        )
                    } else null
                }
        }

        Tasks.whenAllSuccess<MeetingWithWinners?>(tasks)
            .addOnSuccessListener { results ->
                val meetingsWithWinners = results.filterNotNull() // remove nulls

                allMeetingsWithWinners = meetingsWithWinners.toMutableList()
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE

                if (meetingsWithWinners.isEmpty()) {
                    showEmptyState(true)
                } else {
                    meetingsAdapter.submitList(meetingsWithWinners)
                    showEmptyState(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("PastMeetings", "Error loading winners", e)
                showErrorState()
            }
    }

    private fun filterMeetings(query: String) {
        if (query.isBlank()) {
            meetingsAdapter.submitList(allMeetingsWithWinners)
            return
        }

        val filteredMeetings = allMeetingsWithWinners.filter { meeting ->
            meeting.theme.contains(query, ignoreCase = true) ||
                    meeting.date.contains(query, ignoreCase = true) ||
                    meeting.winners.any { winner ->
                        (winner.memberName?.contains(query, ignoreCase = true) == true) ||
                                (winner.guestName?.contains(query, ignoreCase = true) == true) ||
                                winner.category.name.contains(query, ignoreCase = true)
                    }
        }

        meetingsAdapter.submitList(filteredMeetings)
    }

    private fun showEmptyState(show: Boolean) {
        binding.apply {
            progressBar.visibility = View.GONE
            contentLayout.visibility = if (show) View.GONE else View.VISIBLE
            emptyStateText.visibility = if (show) View.VISIBLE else View.GONE
            meetingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
            emptyStateText.text = "No past meetings found"
        }
    }

    private fun showErrorState() {
        binding.apply {
            progressBar.visibility = View.GONE
            contentLayout.visibility = View.GONE
            emptyStateText.visibility = View.VISIBLE
            emptyStateText.text = "Error loading data. Please try again."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
