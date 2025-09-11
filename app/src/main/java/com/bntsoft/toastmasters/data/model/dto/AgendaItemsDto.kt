package com.bntsoft.toastmasters.data.model.dto

data class AgendaItemsDto(
    val itemsId: Int = 0,
    val agendaId: Int,
    val time: String,
    val activity: String,
    val presenterId: Int?,
    val level: String?,
    val project: String?,
    val speechTitle: String?,
    val pathName: String?,
    val greenTimeMin: Int?,
    val yellowTimeMin: Int?,
    val redTimeMin: Int?,
    val order: Int? = null,
    val isSessionHeader: Boolean = false  // Indicates if this is a session header
)
