package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.mapper.MeetingDomainMapper
import com.bntsoft.toastmasters.data.model.dto.MeetingDto
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.utils.NotificationAudience
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMeetingDataSourceImpl @Inject constructor(
    private val meetingMapper: MeetingDomainMapper
) : FirebaseMeetingDataSource {

    companion object {
        private const val MEETINGS_COLLECTION = "meetings"
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
                        val dto = MeetingDto(
                            meetingID = document.getString("meetingID") ?: document.id,
                            date = document.getString("date") ?: "",
                            startTime = document.getString("startTime") ?: "",
                            endTime = document.getString("endTime") ?: "",
                            venue = document.getString("venue") ?: "",
                            theme = document.getString("theme") ?: "No Theme",
                            preferredRoles = (document.get("preferredRoles") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            createdAt = document.getLong("createdAt") ?: 0,
                            isRecurring = document.getBoolean("isRecurring") ?: false
                        )
                        meetingMapper.mapToDomain(dto)
                    } catch (e: Exception) {
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
                        val dto = MeetingDto(
                            meetingID = document.getString("meetingID") ?: document.id,
                            date = document.getString("date") ?: "",
                            startTime = document.getString("startTime") ?: "",
                            endTime = document.getString("endTime") ?: "",
                            venue = document.getString("venue") ?: "",
                            theme = document.getString("theme") ?: "No Theme",
                            preferredRoles = (document.get("preferredRoles") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            createdAt = document.getLong("createdAt") ?: 0,
                            isRecurring = document.getBoolean("isRecurring") ?: false
                        )
                        meetingMapper.mapToDomain(dto)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(meetings).isSuccess
            }

        awaitClose { subscription.remove() }
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
            
            Result.success(newMeeting)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create meeting")
            Result.failure(e)
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
                document.reference.set(dto).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Meeting not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMeeting(id: String): Result<Unit> {
        return try {
            // Find the document with the matching meetingID field and delete it
            val query = meetingsCollection.whereEqualTo("meetingID", id).limit(1).get().await()
            val document = query.documents.firstOrNull()
            document?.reference?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMeetingNotification(meeting: Meeting): Result<Unit> {
        return try {
            // Get all approved members who should receive the notification
            val usersSnapshot = firestore.collection("users")
                .whereEqualTo("accountStatus", "APPROVED")
                .get()
                .await()
                
            val fcmTokens = usersSnapshot.documents
                .mapNotNull { it.getString("fcmToken") }
                .filter { it.isNotBlank() }
                .toSet()
                
            if (fcmTokens.isEmpty()) {
                Timber.w("No valid FCM tokens found for notification")
                return Result.success(Unit)
            }
            
            // Create notification data
            val notificationData = mapOf(
                "registration_ids" to fcmTokens.toList(),
                "priority" to "high",
                "notification" to mapOf(
                    "title" to "New Meeting: ${meeting.theme}",
                    "body" to "${meeting.dateTime.toLocalDate()} at ${meeting.dateTime.toLocalTime()}",
                    "sound" to "default"
                ),
                "data" to mapOf(
                    "type" to "MEETING_CREATED",
                    "meetingId" to meeting.id,
                    "title" to "New Meeting: ${meeting.theme}",
                    "body" to "${meeting.theme} on ${meeting.dateTime.toLocalDate()} at ${meeting.dateTime.toLocalTime()}",
                    "date" to meeting.dateTime.toString(),
                    "location" to meeting.location,
                    "click_action" to "FLUTTER_NOTIFICATION_CLICK"
                )
            )
            
            // Send to all tokens in batches (FCM allows up to 500 tokens per batch)
            val batchSize = 500
            fcmTokens.chunked(batchSize).forEach { batch ->
                val batchData = notificationData.toMutableMap()
                batchData["registration_ids"] = batch
                
                // Store notification in Firestore for history and offline access
                firestore.collection("notifications").add(mapOf(
                    "type" to "MEETING_CREATED",
                    "meetingId" to meeting.id,
                    "title" to "New Meeting: ${meeting.theme}",
                    "body" to "${meeting.theme} on ${meeting.dateTime.toLocalDate()}",
                    "timestamp" to System.currentTimeMillis(),
                    "sentTo" to batch.size,
                    "data" to notificationData["data"]
                )).await()
                
                // Log the notification
                Timber.d("Sent meeting notification to ${batch.size} devices")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send meeting notification")
            Result.failure(e)
        }
    }
}
