package com.bntsoft.toastmasters.presentation.ui.common.leaderboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.data.model.Winner
import com.bntsoft.toastmasters.databinding.FragmentRecentMeetingDetailBinding
import com.bntsoft.toastmasters.utils.Constants.MEETINGS_COLLECTION
import com.bntsoft.toastmasters.utils.Constants.WINNERS_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class RecentMeetingDetailFragment : Fragment() {

    private var _binding: FragmentRecentMeetingDetailBinding? = null
    private val binding get() = _binding!!
    private val winnersAdapter = WinnerAdapter()

    @Inject
    lateinit var firestore: FirebaseFirestore

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

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
        loadRecentMeetingData()
    }

    private fun setupRecyclerView() {
        binding.winnersRecyclerView.apply {
            adapter = winnersAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadRecentMeetingData() {
        showLoading()

        firestore.collection(MEETINGS_COLLECTION)
            .whereEqualTo("status", "COMPLETED")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val recentMeeting = snapshot.documents.firstOrNull()

                if (recentMeeting == null) {
                    showEmptyState("No recent meetings found.")
                    return@addOnSuccessListener
                }

                val meetingId = recentMeeting.id
                binding.meetingTitle.text = recentMeeting.getString("theme") ?: "No Theme"
                binding.meetingDate.text =
                    recentMeeting.getTimestamp("dateTime")?.toDate()?.let(dateFormat::format)
                        ?: "Date not available"
                binding.meetingVenue.text =
                    recentMeeting.getString("venue") ?: "Venue not specified"

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
                val winners = snapshot.toObjects(Winner::class.java)
                    .sortedBy { it.category.ordinal }

                if (winners.isEmpty()) {
                    showEmptyState("No winners recorded for this meeting.")
                } else {
                    winnersAdapter.submitList(winners)
                    showContent()
                }
            }
            .addOnFailureListener { e ->
                Log.e("RecentMeetingDetail", "Error loading winners", e)
                showErrorState("Failed to load winners.")
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

