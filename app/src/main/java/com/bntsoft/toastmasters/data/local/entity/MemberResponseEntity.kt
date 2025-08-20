package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bntsoft.toastmasters.domain.model.MemberResponse

@Entity(
    tableName = "member_responses",
    indices = [
        Index(value = ["meetingId"]),
        Index(value = ["memberId"]),
        Index(value = ["meetingId", "memberId"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["meetingID"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MemberResponseEntity(
    @PrimaryKey
    val id: String = "",
    val meetingId: String,
    val memberId: String,
    val availability: String,
    val preferredRoles: String, // Comma-separated list of role IDs
    val notes: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {

        fun fromDomain(domain: MemberResponse): MemberResponseEntity {
            return MemberResponseEntity(
                id = domain.id,
                meetingId = domain.meetingId,
                memberId = domain.memberId,
                availability = domain.availability.name,
                preferredRoles = domain.preferredRoles.joinToString(","),
                notes = domain.notes,
                lastUpdated = domain.lastUpdated
            )
        }
    }

    fun toDomain(): MemberResponse {
        return MemberResponse(
            id = id,
            meetingId = meetingId,
            memberId = memberId,
            availability = MemberResponse.AvailabilityStatus.fromString(availability),
            preferredRoles = preferredRoles.split(",").filter { it.isNotBlank() },
            notes = notes,
            lastUpdated = lastUpdated
        )
    }
}
