package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.mapper.MeetingDomainMapper
import com.bntsoft.toastmasters.data.model.dto.MeetingDto
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.utils.Result
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMeetingDataSourceImpl @Inject constructor(
    private val meetingMapper: MeetingDomainMapper
) : FirebaseMeetingDataSource {

    companion object {
        private const val MEETINGS_COLLECTION = "meetings"
        private const val ROLES_COLLECTION = "meeting_roles"
        private const val ROLE_ASSIGNMENTS_COLLECTION = "role_assignments"
    }

    private val firestore: FirebaseFirestore by lazy { Firebase.firestore }
    private val meetingsCollection by lazy { firestore.collection(MEETINGS_COLLECTION) }

    override fun getAllMeetings(): Flow<List<Meeting>> = callbackFlow {
        val subscription = meetingsCollection
            .orderBy("date")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val meetings = snapshot?.documents?.mapNotNull { document ->
                    try {
                        val dateString = document.getString("date") ?: ""
                        val startTimeString = document.getString("startTime") ?: "00:00"
                        val endTimeString = document.getString("endTime") ?: ""
                        
                        // Parse date and times
                        val date = try {
                            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                        } catch (e: Exception) {
                            LocalDate.now()
                        }
                        
                        val startTime = try {
                            LocalTime.parse(startTimeString)
                        } catch (e: Exception) {
                            LocalTime.NOON
                        }
                        
                        val endTime = try {
                            if (endTimeString.isNotBlank()) {
                                LocalTime.parse(endTimeString)
                            } else {
                                startTime.plusHours(2)
                            }
                        } catch (e: Exception) {
                            startTime.plusHours(2)
                        }
                        
                        // Create LocalDateTime objects
                        val dateTime = LocalDateTime.of(date, startTime)
                        val endDateTime = LocalDateTime.of(date, endTime)
                        
                        val dto = MeetingDto(
                            meetingID = document.getString("meetingID") ?: document.id,
                            date = dateString,
                            startTime = startTimeString,
                            endTime = endTimeString,
                            dateTime = dateTime,
                            endDateTime = endDateTime,
                            venue = document.getString("venue") ?: "",
                            theme = document.getString("theme") ?: "No Theme",
                            preferredRoles = (document.get("preferredRoles") as? List<*>)?.filterIsInstance<String>()
                                ?: emptyList(),
                            createdAt = document.getLong("createdAt") ?: 0,
                            status = document.getString("status")?.let { 
                                try {
                                    com.bntsoft.toastmasters.domain.models.MeetingStatus.valueOf(it)
                                } catch (e: Exception) {
                                    com.bntsoft.toastmasters.domain.models.MeetingStatus.NOT_COMPLETED
                                }
                            } ?: com.bntsoft.toastmasters.domain.models.MeetingStatus.NOT_COMPLETED
                        )
                        
                        Timber.d("Mapped meeting in getAllMeetings: ${dto.meetingID} - ${dto.theme} - ${dto.startTime} to ${dto.endTime}")
                        meetingMapper.mapToDomain(dto)
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing meeting document in getAllMeetings")
                        null
                    }
                } ?: emptyList()

                trySend(meetings).isSuccess
            }

        awaitClose { subscription.remove() }
    }

    override fun getUpcomingMeetings(afterDate: LocalDate): Flow<List<Meeting>> = callbackFlow {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val dateString = afterDate.format(formatter)

        val subscription = meetingsCollection
            .whereGreaterThanOrEqualTo("date", dateString)
            .orderBy("date")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val meetings = snapshot?.documents?.mapNotNull { document ->
                    try {
                        val dateString = document.getString("date") ?: ""
                        val startTimeString = document.getString("startTime") ?: "00:00"
                        val endTimeString = document.getString("endTime") ?: "23:59"

                        // Parse date and times
                        val date = LocalDate.parse(dateString, formatter)
                        val startTime = LocalTime.parse(startTimeString)
                        val endTime = LocalTime.parse(endTimeString)

                        // Create LocalDateTime objects for both start and end times
                        val dateTime = LocalDateTime.of(date, startTime)
                        val endDateTime = LocalDateTime.of(date, endTime)

                        // Create the DTO with all fields
                        val dto = MeetingDto(
                            meetingID = document.getString("meetingID") ?: document.id,
                            date = dateString,
                            startTime = startTimeString,
                            endTime = endTimeString,
                            dateTime = dateTime,  // Set the parsed dateTime
                            endDateTime = endDateTime,  // Set the parsed endDateTime
                            venue = document.getString("venue") ?: "",
                            theme = document.getString("theme") ?: "No Theme",
                            preferredRoles = (document.get("preferredRoles") as? List<*>)?.filterIsInstance<String>()
                                ?: emptyList(),
                            createdAt = document.getLong("createdAt") ?: 0,
                            status = document.getString("status")?.let { 
                                try {
                                    com.bntsoft.toastmasters.domain.models.MeetingStatus.valueOf(it)
                                } catch (e: Exception) {
                                    com.bntsoft.toastmasters.domain.models.MeetingStatus.NOT_COMPLETED
                                }
                            } ?: com.bntsoft.toastmasters.domain.models.MeetingStatus.NOT_COMPLETED
                        )
                        
                        Timber.d("Mapped meeting: ${dto.meetingID} - ${dto.theme} - ${dto.startTime} to ${dto.endTime} (${dto.dateTime} to ${dto.endDateTime})")

                        val meeting = meetingMapper.mapToDomain(dto)
                        // Ensure the meeting's date matches the filter
                        if (date.isAfter(afterDate) || date.isEqual(afterDate)) {
                            meeting
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing meeting document")
                        null
                    }
                } ?: emptyList()

                trySend(meetings).isSuccess
            }

        awaitClose {
            subscription.remove()

        }
    }


    override suspend fun getUserPreferredRoles(meetingId: String, userId: String): List<String> {
        return try {
            // Check if the user has marked any preferred roles for this meeting
            val preferenceDoc = firestore
                .collection("meetings")
                .document(meetingId)
                .collection("availability")
                .document(userId)
                .get()
                .await()

            // Return the preferred roles if they exist, otherwise empty list
            (preferenceDoc.get("preferredRoles") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error fetching preferred roles for user $userId in meeting $meetingId")
            emptyList()
        }
    }

    override suspend fun getMeetingPreferredRoles(meetingId: String): List<String> {
        return try {
            // Get the meeting document
            val meetingDoc = firestore
                .collection("meetings")
                .document(meetingId)
                .get()
                .await()

            // Return the meeting's preferred roles if they exist, otherwise empty list
            (meetingDoc.get("preferredRoles") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error fetching preferred roles for meeting $meetingId")
            emptyList()
        }
    }

    override suspend fun getMeetingById(id: String): Meeting? {
        return try {
            // First try with meetingID (uppercase ID)
            var query = meetingsCollection.whereEqualTo("meetingID", id).limit(1).get().await()
            var document = query.documents.firstOrNull()

            // If not found, try with id (lowercase)
            if (document == null) {
                query = meetingsCollection.whereEqualTo("id", id).limit(1).get().await()
                document = query.documents.firstOrNull()
            }

            val dto = document?.toObject(MeetingDto::class.java)
            dto?.let { meetingMapper.mapToDomain(it) }
        } catch (e: Exception) {
            Timber.e(e, "Error getting meeting by id: $id")
            null
        }
    }

    override suspend fun createMeeting(meeting: Meeting): Result<Meeting> {
        return try {
            val document = meetingsCollection.document()
            val dto = meetingMapper.mapToDto(meeting).copy(meetingID = document.id)
            document.set(dto).await()
            val newMeeting = meetingMapper.mapToDomain(dto)

            // Send notification about the new meeting
            sendMeetingNotification(newMeeting)

            Result.Success(newMeeting)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create meeting")
            Result.Error(e)
        }
    }

    override suspend fun updateMeeting(meeting: Meeting): Result<Unit> {
        return try {
            val dto = meetingMapper.mapToDto(meeting)
            // Find the document with the matching meetingID
            val query =
                meetingsCollection.whereEqualTo("meetingID", meeting.id).limit(1).get().await()
            val document = query.documents.firstOrNull()

            if (document != null) {
                document.reference.update(
                    mapOf(
                        "date" to dto.date,
                        "startTime" to dto.startTime,
                        "endTime" to dto.endTime,
                        "venue" to dto.venue,
                        "theme" to dto.theme,
                        "preferredRoles" to dto.preferredRoles,
                    )
                ).await()
                Result.Success(Unit)
            } else {
                Result.Error(NoSuchElementException("Meeting not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update meeting")
            Result.Error(e)
        }
    }

    override suspend fun deleteMeeting(id: String): Result<Unit> {
        return try {
            // Find the document with the matching meetingID field and delete it
            val query = meetingsCollection.whereEqualTo("meetingID", id).limit(1).get().await()
            val document = query.documents.firstOrNull()
            if (document != null) {
                document.reference.delete().await()
                Result.Success(Unit)
            } else {
                Result.Error(NoSuchElementException("Meeting not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete meeting")
            Result.Error(e)
        }
    }

    override suspend fun completeMeeting(meetingId: String): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to "COMPLETED"
            )
            val query =
                meetingsCollection.whereEqualTo("meetingID", meetingId).limit(1).get().await()
            val document = query.documents.firstOrNull()

            if (document != null) {
                document.reference.update(updates).await()
                Result.Success(Unit)
            } else {
                Result.Error(NoSuchElementException("Meeting not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error completing meeting $meetingId")
            Result.Error(e)
        }
    }

    override suspend fun sendMeetingNotification(meeting: Meeting): Result<Unit> {
        return try {
            // Get all members assigned to roles in this meeting
            val snapshot = firestore.collection(ROLE_ASSIGNMENTS_COLLECTION)
                .whereEqualTo("meetingId", meeting.id)
                .get()
                .await()

            // Extract unique member IDs
            val memberIds = snapshot.documents.mapNotNull { doc ->
                doc.getString("memberId")
            }.distinct()

            // In a real implementation, we would send notifications to each member
            // For now, we'll just log the notification details
            Timber.d("Sending notification for meeting ${meeting.id} to members: $memberIds")
            
            // Simulate notification sending
            memberIds.forEach { memberId ->
                // In a real app, this would use a notification service
                // For example: notificationService.sendMeetingNotification(memberId, meeting)
                Timber.d("Notification sent to member $memberId about meeting ${meeting.id}")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send notifications for meeting: ${meeting.id}")
            Result.Error(e)
        }
    }

}
