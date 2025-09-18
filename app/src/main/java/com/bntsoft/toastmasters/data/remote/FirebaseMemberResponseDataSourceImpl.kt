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
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMemberResponseDataSourceImpl @Inject constructor() :
    FirebaseMemberResponseDataSource {

    companion object {
        private const val FIELD_AVAILABILITY = "status"
        private const val FIELD_LAST_UPDATED = "lastUpdated"
        private const val FIELD_MEETING_ID = "meetingId"
        private const val FIELD_MEMBER_ID = "memberId"
    }

    private val db: FirebaseFirestore = Firebase.firestore

    override suspend fun getResponse(meetingId: String, memberId: String): MemberResponseDto? {
        return try {
            val doc = db.collection("meetings")
                .document(meetingId)
                .collection("availability")
                .document(memberId)
                .get()
                .await()

            if (doc.exists()) {
                val status = doc.getString(FIELD_AVAILABILITY) ?: return null
                val preferredRoles = doc.get("preferredRoles") as? List<*>
                MemberResponseDto(
                    id = "${meetingId}_$memberId",
                    meetingId = meetingId,
                    memberId = memberId,
                    availability = status,
                    preferredRoles = preferredRoles?.filterIsInstance<String>() ?: emptyList(),
                    lastUpdated = doc.getLong(FIELD_LAST_UPDATED) ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseRespDS", "Error getting response for meeting $meetingId and member $memberId", e)
            null
        }
    }

    override suspend fun getResponsesForMeeting(meetingId: String): List<MemberResponseDto> {
        return try {
            val querySnapshot = db.collection("meetings")
                .document(meetingId)
                .collection("availability")
                .get()
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                val status = doc.getString(FIELD_AVAILABILITY) ?: return@mapNotNull null
                val memberId = doc.id
                val preferredRoles = doc.get("preferredRoles") as? List<*>
                MemberResponseDto(
                    id = "${meetingId}_$memberId",
                    meetingId = meetingId,
                    memberId = memberId,
                    availability = status,
                    preferredRoles = preferredRoles?.filterIsInstance<String>() ?: emptyList(),
                    lastUpdated = doc.getLong(FIELD_LAST_UPDATED) ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e("FirebaseRespDS", "Error getting responses for meeting $meetingId", e)
            emptyList()
        }
    }

    override suspend fun getResponsesByMember(memberId: String): List<MemberResponseDto> {
        return try {
            val querySnapshot = db.collectionGroup("availability")
                .whereEqualTo(FIELD_MEMBER_ID, memberId)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { it.toObject(MemberResponseDto::class.java) }
        } catch (e: Exception) {
            Log.e("FirebaseRespDS", "Error getting responses for member $memberId", e)
            emptyList()
        }
    }

    override suspend fun getBackoutMembers(meetingId: String): List<Pair<String, Long>> {
        return try {
            val backoutMembers = db.collection("meetings")
                .document(meetingId)
                .collection("backoutMembers")
                .get()
                .await()

            backoutMembers.documents.mapNotNull { doc ->
                val userId = doc.id
                val timestamp = doc.getLong("timestamp") ?: 0L
                if (userId.isNotBlank() && timestamp > 0) {
                    userId to timestamp
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseRespDS", "Error getting backout members for meeting $meetingId", e)
            emptyList()
        }
    }

    override suspend fun saveResponse(response: MemberResponseDto) {
        try {
            val data = hashMapOf(
                FIELD_AVAILABILITY to response.availability,
                FIELD_LAST_UPDATED to System.currentTimeMillis(),
                "preferredRoles" to response.preferredRoles,
                "memberId" to response.memberId,
                "meetingId" to response.meetingId
            )

            db.collection("meetings")
                .document(response.meetingId)
                .collection("availability")
                .document(response.memberId)
                .set(data)
                .await()

            // Update the last updated timestamp in the meeting document
            db.collection("meetings")
                .document(response.meetingId)
                .update("lastUpdated", Timestamp.now())
                .await()

        } catch (e: Exception) {
            Log.e("FirebaseRespDS", "Error saving response for meeting ${response.meetingId} and member ${response.memberId}", e)
            throw e
        }
    }

    override suspend fun deleteResponse(meetingId: String, memberId: String) {
        try {
            db.collection("meetings")
                .document(meetingId)
                .collection("availability")
                .document(memberId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseRespDS", "Error deleting response for meeting $meetingId and member $memberId", e)
            throw e
        }
    }

    override fun observeResponse(meetingId: String, memberId: String): Flow<MemberResponseDto?> =
        callbackFlow {
            val docRef = db.collection("meetings")
                .document(meetingId)
                .collection("availability")
                .document(memberId)

            val listener = docRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirebaseRespDS", "Error observing response for meeting $meetingId and member $memberId", e)
                    return@addSnapshotListener
                }

                val response = if (snapshot != null && snapshot.exists()) {
                    val status =
                        snapshot.getString(FIELD_AVAILABILITY) ?: return@addSnapshotListener
                    val preferredRoles = snapshot.get("preferredRoles") as? List<*>
                    MemberResponseDto(
                        id = "${meetingId}_$memberId",
                        meetingId = meetingId,
                        memberId = memberId,
                        availability = status,
                        preferredRoles = preferredRoles?.filterIsInstance<String>() ?: emptyList(),
                        lastUpdated = snapshot.getLong(FIELD_LAST_UPDATED)
                            ?: System.currentTimeMillis()
                    )
                } else {
                    null
                }

                trySend(response)
            }

            awaitClose {
                try {
                    listener.remove()
                } catch (e: Exception) {
                    Log.e("FirebaseRespDS", "Error removing listener", e)
                }
            }
        }

    override fun observeResponsesForMeeting(meetingId: String): Flow<List<MemberResponseDto>> =
        callbackFlow {
            val listener = db.collection("meetings")
                .document(meetingId)
                .collection("availability")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FirebaseRespDS", "Error observing responses for meeting $meetingId", e)
                        try {
                            trySend(emptyList())
                        } catch (sendException: Exception) {
                            // Ignore send exception - channel might be closed
                            Log.d("FirebaseRespDS", "Failed to send empty list: ${sendException.message}")
                        }
                        return@addSnapshotListener
                    }

                    val responses = snapshot?.documents?.mapNotNull { doc ->
                        val status = doc.getString(FIELD_AVAILABILITY) ?: return@mapNotNull null
                        val memberId = doc.id
                        val preferredRoles = doc.get("preferredRoles") as? List<*>
                        MemberResponseDto(
                            id = "${meetingId}_$memberId",
                            meetingId = meetingId,
                            memberId = memberId,
                            availability = status,
                            preferredRoles = preferredRoles?.filterIsInstance<String>()
                                ?: emptyList(),
                            lastUpdated = doc.getLong(FIELD_LAST_UPDATED)
                                ?: System.currentTimeMillis()
                        )
                    } ?: emptyList()

                    try {
                        trySend(responses).isSuccess
                    } catch (sendException: Exception) {
                        // Ignore send exception - channel might be closed
                        Log.d("FirebaseRespDS", "Failed to send responses: ${sendException.message}")
                    }
                }

            awaitClose {
                try {
                    listener.remove()
                } catch (e: Exception) {
                    Log.e("FirebaseRespDS", "Error removing listener", e)
                }
            }
        }
}
