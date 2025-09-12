package com.bntsoft.toastmasters.presentation.ui.common.leaderboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bntsoft.toastmasters.data.model.MeetingWithWinners
import com.bntsoft.toastmasters.data.model.Winner
import com.bntsoft.toastmasters.databinding.FragmentLeaderboardBinding
import com.bntsoft.toastmasters.utils.Constants.MEETINGS_COLLECTION
import com.bntsoft.toastmasters.utils.Constants.WINNERS_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val adapter = LeaderboardMeetingAdapter()

    @Inject
    lateinit var firestore: FirebaseFirestore

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadMeetingsWithWinners()
    }

    private fun setupRecyclerView() {
        binding.meetingsRecyclerView.adapter = adapter
    }

    private fun loadMeetingsWithWinners() {
        firestore.collection(MEETINGS_COLLECTION)
            .whereEqualTo("status", "COMPLETED")
            .get()
            .addOnSuccessListener { meetingsSnapshot ->
                val meetingIds = meetingsSnapshot.documents.map { it.id }
                val meetingsWithWinners = mutableListOf<MeetingWithWinners>()

                if (meetingIds.isEmpty()) {
                    showEmptyState(true)
                    return@addOnSuccessListener
                }

                meetingIds.forEach { meetingId ->
                    firestore.collection(MEETINGS_COLLECTION)
                        .document(meetingId)
                        .collection(WINNERS_COLLECTION)
                        .get()
                        .addOnSuccessListener { winnersSnapshot ->
                            val winners = winnersSnapshot.toObjects(Winner::class.java)
                            if (winners.isNotEmpty()) {
                                val meetingData = meetingsSnapshot.documents.find { it.id == meetingId }
                                val meeting = MeetingWithWinners(
                                    meetingId = meetingId,
                                    date = dateFormat.format(meetingData?.getTimestamp("dateTime")?.toDate() ?: Date()),
                                    theme = meetingData?.getString("theme") ?: "",
                                    winners = winners
                                )
                                meetingsWithWinners.add(meeting)
                                adapter.submitList(meetingsWithWinners.sortedByDescending { it.date })
                                showEmptyState(meetingsWithWinners.isEmpty())
                            } else {
                                showEmptyState(meetingsWithWinners.isEmpty())
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Leaderboard", "Error loading winners", e)
                            showEmptyState(true)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Leaderboard", "Error loading meetings", e)
                showEmptyState(true)
            }
    }

    private fun showEmptyState(show: Boolean) {
        binding.apply {
            emptyStateText.visibility = if (show) View.VISIBLE else View.GONE
            meetingsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
