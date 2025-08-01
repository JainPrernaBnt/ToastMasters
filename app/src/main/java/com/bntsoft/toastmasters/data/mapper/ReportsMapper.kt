package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.ReportRequests
import com.bntsoft.toastmasters.data.model.dto.ReportRequestsDto
import javax.inject.Inject

class ReportsMapper @Inject constructor() : Mapper<ReportRequestsDto, ReportRequests> {
    override fun mapFromEntity(entity: ReportRequests): ReportRequestsDto {
        return ReportRequestsDto(
            id = entity.id,
            reportType = entity.reportType,
            requestedBy = entity.requestedBy,
            startDate = entity.startDate,
            endDate = entity.endDate,
            generatedAt = entity.generatedAt
        )
    }

    override fun mapToEntity(dto: ReportRequestsDto): ReportRequests {
        return ReportRequests(
            id = dto.id,
            reportType = dto.reportType,
            requestedBy = dto.requestedBy,
            startDate = dto.startDate,
            endDate = dto.endDate,
            generatedAt = dto.generatedAt
        )
    }

    fun mapFromEntityList(entities: List<ReportRequests>): List<ReportRequestsDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<ReportRequestsDto>): List<ReportRequests> {
        return dtos.map { mapToEntity(it) }
    }
}
