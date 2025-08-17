package com.bntsoft.toastmasters.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bntsoft.toastmasters.domain.model.MemberResponse

@Entity(
    tableName = "member_responses",
    indices = [
        Index(value = ["meeting_id"]),
        Index(value = ["member_id"]),
        Index(value = ["meeting_id", "member_id"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["id"],
            childColumns = ["meeting_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MemberResponseEntity(
    @PrimaryKey
    val id: String,
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
                id = domain.id.ifEmpty { "${domain.meetingId}_${domain.memberId}" },
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
