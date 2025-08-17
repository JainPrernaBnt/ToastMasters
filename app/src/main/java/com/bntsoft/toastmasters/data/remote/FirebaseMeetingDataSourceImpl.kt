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
            .orderBy("dateTime")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val meetings = snapshot?.documents?.mapNotNull { document ->
                    try {
                        val dto = document.toObject(MeetingDto::class.java)
                        dto?.let { meetingMapper.mapToDomain(it) }
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(meetings)
            }
        
        awaitClose { subscription.remove() }
    }

    override fun getUpcomingMeetings(afterDate: LocalDate): Flow<List<Meeting>> = callbackFlow {
        val dateString = afterDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        val subscription = meetingsCollection
            .whereGreaterThanOrEqualTo("date", dateString)
            .orderBy("date")
            .orderBy("startTime")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val meetings = snapshot?.documents?.mapNotNull { document ->
                    try {
                        val dto = document.toObject(MeetingDto::class.java)
                        dto?.let { meetingMapper.mapToDomain(it) }
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(meetings)
            }
        
        awaitClose { subscription.remove() }
    }

    override suspend fun getMeetingById(id: String): Meeting? {
        return try {
            val document = meetingsCollection.document(id).get().await()
            val dto = document.toObject(MeetingDto::class.java)
            dto?.let { meetingMapper.mapToDomain(it) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createMeeting(meeting: Meeting): Result<Unit> {
        return try {
            val dto = meetingMapper.mapToDto(meeting)
            val document = meetingsCollection.document(meeting.id)
            document.set(dto).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMeeting(meeting: Meeting): Result<Unit> {
        return try {
            val dto = meetingMapper.mapToDto(meeting)
            val document = meetingsCollection.document(meeting.id)
            document.set(dto).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMeeting(id: String): Result<Unit> {
        return try {
            meetingsCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMeetingNotification(meeting: Meeting): Result<Unit> {
        return try {
            // Create a map with the notification data
            val notificationData = mapOf(
                "to" to "/topics/${NotificationAudience.ALL}",
                "notification" to mapOf(
                    "title" to "New Meeting Scheduled: ${meeting.title}",
                    "body" to "Date: ${meeting.dateTime.toLocalDate()}\nTime: ${meeting.dateTime.toLocalTime()}\nLocation: ${meeting.location.takeIf { it.isNotBlank() } ?: "TBD"}",
                    "click_action" to "OPEN_MEETING_DETAILS",
                    "meeting_id" to meeting.id
                ),
                "data" to mapOf(
                    "type" to "NEW_MEETING",
                    "meeting_id" to meeting.id,
                    "title" to meeting.title,
                    "date" to meeting.dateTime.toString(),
                    "location" to meeting.location
                )
            )

            // Send the notification using Firebase Cloud Messaging
            val response = firestore.collection("notifications").add(notificationData).await()
            
            if (response.id.isNotEmpty()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send notification"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
