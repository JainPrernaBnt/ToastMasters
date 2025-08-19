package com.bntsoft.toastmasters.data.model.dto

import com.bntsoft.toastmasters.domain.models.ReportType

data class ReportRequestsDto(
    val id: Int = 0,
    val reportType: ReportType,
    val requestedBy: String,
    val startDate: String,
    val endDate: String,
    val generatedAt: String
)
