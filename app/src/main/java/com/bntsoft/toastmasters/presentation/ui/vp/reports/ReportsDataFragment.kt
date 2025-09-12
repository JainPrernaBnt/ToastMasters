package com.bntsoft.toastmasters.presentation.ui.vp.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bntsoft.toastmasters.databinding.FragmentReportsDataBinding

class ReportsDataFragment : Fragment() {
    private var _binding: FragmentReportsDataBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentReportsDataBinding.inflate(inflater, container, false)
        return binding.root
    }

}