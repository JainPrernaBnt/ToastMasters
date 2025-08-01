package com.bntsoft.toastmasters.data.local.converters

import androidx.room.TypeConverter
import com.bntsoft.toastmasters.domain.model.ReportType

class ReportTypeConverter {
    @TypeConverter
    fun fromReportType(reportType: ReportType): String = reportType.name

    @TypeConverter
    fun toReportType(value: String): ReportType = ReportType.valueOf(value)
}