package com.bntsoft.toastmasters.presentation.ui.members.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bntsoft.toastmasters.databinding.FragmentMemberDashboardBinding

class MemberDashboardFragment : Fragment() {
    private var _binding: FragmentMemberDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

}