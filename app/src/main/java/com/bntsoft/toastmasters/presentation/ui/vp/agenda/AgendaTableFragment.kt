package com.bntsoft.toastmasters.presentation.ui.vp.agenda

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bntsoft.toastmasters.R
import com.bntsoft.toastmasters.databinding.FragmentAgendaTableBinding

class AgendaTableFragment : Fragment() {

    private var _binding: FragmentAgendaTableBinding? = null
    private val binding get() = _binding!!
    private lateinit var meetingId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            meetingId = it.getString(ARG_MEETING_ID) ?: throw IllegalStateException("meetingId is required")
//        } ?: throw IllegalStateException("meetingId argument is required")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgendaTableBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
//        private const val ARG_MEETING_ID = "meetingId"
//
//        @JvmStatic
//        fun newInstance(meetingId: String) =
//            AgendaTableFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_MEETING_ID, meetingId)
//                }
//            }
    }
}