package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.remote.dto.MemberResponseDto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMemberResponseDataSourceImpl @Inject constructor() : FirebaseMemberResponseDataSource {

    companion object {
        private const val COLLECTION_RESPONSES = "meeting_responses"
        private const val FIELD_MEETING_ID = "meetingId"
        private const val FIELD_MEMBER_ID = "memberId"
        private const val FIELD_LAST_UPDATED = "lastUpdated"
    }

    private val db: FirebaseFirestore = Firebase.firestore
    private val responsesCollection = db.collection(COLLECTION_RESPONSES)

    override suspend fun getResponse(meetingId: String, memberId: String): MemberResponseDto? {
        return try {
            val querySnapshot = responsesCollection
                .whereEqualTo(FIELD_MEETING_ID, meetingId)
                .whereEqualTo(FIELD_MEMBER_ID, memberId)
                .limit(1)
                .get()
                .await()

            querySnapshot.documents.firstOrNull()?.toObject(MemberResponseDto::class.java)
        } catch (e: Exception) {
            Timber.e(e, "Error getting response for meeting $meetingId and member $memberId")
            null
        }
    }

    override suspend fun getResponsesForMeeting(meetingId: String): List<MemberResponseDto> {
        return try {
            val querySnapshot = responsesCollection
                .whereEqualTo(FIELD_MEETING_ID, meetingId)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { it.toObject(MemberResponseDto::class.java) }
        } catch (e: Exception) {
            Timber.e(e, "Error getting responses for meeting $meetingId")
            emptyList()
        }
    }

    override suspend fun getResponsesByMember(memberId: String): List<MemberResponseDto> {
        return try {
            val querySnapshot = responsesCollection
                .whereEqualTo(FIELD_MEMBER_ID, memberId)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { it.toObject(MemberResponseDto::class.java) }
        } catch (e: Exception) {
            Timber.e(e, "Error getting responses for member $memberId")
            emptyList()
        }
    }

    override suspend fun saveResponse(response: MemberResponseDto) {
        try {
            val responseWithTimestamp = response.copy(
                lastUpdated = System.currentTimeMillis()
            )
            
            val documentId = "${response.meetingId}_${response.memberId}"
            responsesCollection.document(documentId).set(responseWithTimestamp).await()
            
            // Update the last updated timestamp in the meeting document
            db.collection("meetings").document(response.meetingId)
                .update("lastUpdated", Timestamp.now()).await()
                
        } catch (e: Exception) {
            Timber.e(e, "Error saving response for meeting ${response.meetingId} and member ${response.memberId}")
            throw e
        }
    }

    override suspend fun deleteResponse(meetingId: String, memberId: String) {
        try {
            val documentId = "${meetingId}_$memberId"
            responsesCollection.document(documentId).delete().await()
            
            // Update the last updated timestamp in the meeting document
            db.collection("meetings").document(meetingId)
                .update("lastUpdated", Timestamp.now()).await()
                
        } catch (e: Exception) {
            Timber.e(e, "Error deleting response for meeting $meetingId and member $memberId")
            throw e
        }
    }

    override fun observeResponse(meetingId: String, memberId: String): Flow<MemberResponseDto?> = callbackFlow {
        val documentId = "${meetingId}_$memberId"
        val listener = responsesCollection.document(documentId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Timber.e(e, "Error observing response for meeting $meetingId and member $memberId")
                    trySend(null)
                    return@addSnapshotListener
                }

                val response = snapshot?.toObject(MemberResponseDto::class.java)
                trySend(response)
            }

        // Remove the listener when the flow is no longer collected
        awaitClose { listener.remove() }
    }

    override fun observeResponsesForMeeting(meetingId: String): Flow<List<MemberResponseDto>> = callbackFlow {
        val listener = responsesCollection
            .whereEqualTo(FIELD_MEETING_ID, meetingId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Timber.e(e, "Error observing responses for meeting $meetingId")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val responses = snapshot?.documents?.mapNotNull { it.toObject(MemberResponseDto::class.java) } ?: emptyList()
                trySend(responses)
            }

        // Remove the listener when the flow is no longer collected
        awaitClose { listener.remove() }
    }
}
