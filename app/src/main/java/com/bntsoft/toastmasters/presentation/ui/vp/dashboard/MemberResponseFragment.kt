package com.bntsoft.toastmasters.presentation.ui.vp.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bntsoft.toastmasters.databinding.FragmentMemberResponseBinding

class MemberResponseFragment : Fragment() {

    private var _binding: FragmentMemberResponseBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMemberResponseBinding.inflate(inflater, container, false)
        return binding.root
    }

}