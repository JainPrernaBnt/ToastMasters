package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.bntsoft.toastmasters.data.local.converters.ReportTypeConverter
import com.bntsoft.toastmasters.domain.model.ReportType

@Entity(tableName = "report_requests")
@TypeConverters(ReportTypeConverter::class)
data class ReportRequests(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val reportType: ReportType,
    val requestedBy: String,
    val startDate: String,
    val endDate: String,
    val generatedAt: String
)
