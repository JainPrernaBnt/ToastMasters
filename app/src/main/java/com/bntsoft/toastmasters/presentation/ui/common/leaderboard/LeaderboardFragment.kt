package com.bntsoft.toastmasters.presentation.ui.common.leaderboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.data.model.MeetingWithWinners
import com.bntsoft.toastmasters.data.model.Winner
import com.bntsoft.toastmasters.databinding.FragmentLeaderboardBinding
import com.bntsoft.toastmasters.utils.Constants.MEETINGS_COLLECTION
import com.bntsoft.toastmasters.utils.Constants.WINNERS_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var firestore: FirebaseFirestore


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
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.gemOfMonthCard.setOnClickListener {
            navigateToGemOfMonth()
        }
        
        binding.recentMeetingCard.setOnClickListener {
            navigateToRecentMeetingDetail()
        }
        
        binding.pastMeetingsCard.setOnClickListener {
            navigateToPastMeetings()
        }
    }

    private fun navigateToGemOfMonth() {
        findNavController().navigate(R.id.action_leaderboardFragment_to_gemOfMonthFragment)
    }

    private fun navigateToRecentMeetingDetail() {
        findNavController().navigate(R.id.action_leaderboardFragment_to_recentMeetingDetailFragment)
    }

    private fun navigateToPastMeetings() {
        findNavController().navigate(R.id.action_leaderboardFragment_to_pastMeetingsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
