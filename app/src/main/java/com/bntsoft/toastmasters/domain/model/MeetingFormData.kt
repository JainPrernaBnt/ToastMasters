package com.bntsoft.toastmasters.domain.model

import com.bntsoft.toastmasters.databinding.ItemMeetingFormBinding
import java.util.Calendar

data class MeetingFormData(
    val binding: ItemMeetingFormBinding,
    val startCalendar: Calendar,
    val endCalendar: Calendar
)