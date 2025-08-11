package com.bntsoft.toastmasters.ui.members.upcoming

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentMemberDashboardBinding
import com.bntsoft.toastmasters.databinding.FragmentUpcomingMeetingsBinding

class UpcomingMeetingsFragment : Fragment() {
    private var _binding: FragmentUpcomingMeetingsBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentUpcomingMeetingsBinding.inflate(inflater, container, false)
        return binding.root
    }

}