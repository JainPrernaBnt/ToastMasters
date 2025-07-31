package com.bntsoft.toastmasters.data.mapper

import com.bntsoft.toastmasters.data.local.entity.AgendaItems
import com.bntsoft.toastmasters.data.model.dto.AgendaItemsDto
import javax.inject.Inject

class AgendaItemsMapper @Inject constructor() : Mapper<AgendaItemsDto, AgendaItems> {

    override fun mapFromEntity(entity: AgendaItems): AgendaItemsDto {
        return AgendaItemsDto(
            itemsId = entity.itemsId,
            agendaId = entity.agendaId,
            time = entity.time,
            activity = entity.activity,
            presenterId = entity.presenterId,
            level = entity.level,
            project = entity.project,
            speechTitle = entity.speechTitle,
            pathName = entity.pathName,
            greenTimeMin = entity.greenTimeMin,
            yellowTimeMin = entity.yellowTimeMin,
            redTimeMin = entity.redTimeMin,
            order = entity.order
        )
    }

    override fun mapToEntity(dto: AgendaItemsDto): AgendaItems {
        return AgendaItems(
            itemsId = dto.itemsId,
            agendaId = dto.agendaId,
            time = dto.time,
            activity = dto.activity,
            presenterId = dto.presenterId,
            level = dto.level,
            project = dto.project,
            speechTitle = dto.speechTitle,
            pathName = dto.pathName,
            greenTimeMin = dto.greenTimeMin,
            yellowTimeMin = dto.yellowTimeMin,
            redTimeMin = dto.redTimeMin,
            order = dto.order
        )
    }

    fun mapFromEntityList(entities: List<AgendaItems>): List<AgendaItemsDto> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(dtos: List<AgendaItemsDto>): List<AgendaItems> {
        return dtos.map { mapToEntity(it) }
    }
}
