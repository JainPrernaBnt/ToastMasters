package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "agenda_items",
    foreignKeys = [
        ForeignKey(
            entity = AgendaMaster::class,
            parentColumns = ["agendaId"],
            childColumns = ["agendaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["presenterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["agendaId"]),
        Index(value = ["presenterId"])
    ]
)
data class AgendaItems(
    @PrimaryKey(autoGenerate = true)
    val itemsId: Int = 0,

    val agendaId: Int,
    val time: String,
    val activity: String,
    val presenterId: Int?,
    // Speech-related fields (nullable if not a speech)
    val level: String?,
    val project: String?,
    val speechTitle: String?,
    val pathName: String?,
    // Timer lights (optional, nullable)
    val greenTimeMin: Int?,
    val yellowTimeMin: Int?,
    val redTimeMin: Int?,
    val order: Int? = null          // To sort items if needed
)
