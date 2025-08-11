package com.bntsoft.toastmasters.ui.common.leaderboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentCreateMeetingBinding
import com.bntsoft.toastmasters.databinding.FragmentLeaderboardBinding

class LeaderboardFragment : Fragment() {
    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding =  FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }


}