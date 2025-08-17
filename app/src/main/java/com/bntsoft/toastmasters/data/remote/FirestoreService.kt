package com.bntsoft.toastmasters.data.remote

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val MEETINGS_COLLECTION = "meetings"
    }

    // User operations
    fun getUserDocument(userId: String): DocumentReference {
        return firestore.collection(USERS_COLLECTION).document(userId)
    }

    suspend fun setUserDocument(userId: String, user: Any) {
        firestore.collection(USERS_COLLECTION).document(userId).set(user).await()
    }

    suspend fun updateUserField(userId: String, field: String, value: Any) {
        firestore.collection(USERS_COLLECTION).document(userId).update(field, value).await()
    }

    suspend fun getUserByEmail(email: String): QuerySnapshot {
        return firestore.collection(USERS_COLLECTION)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()
    }

    suspend fun getUserByPhone(phoneNumber: String): QuerySnapshot {
        return firestore.collection(USERS_COLLECTION)
            .whereEqualTo("phoneNumber", phoneNumber)
            .limit(1)
            .get()
            .await()
    }

    fun observeUser(userId: String): Flow<DocumentSnapshot> = callbackFlow {
        val listenerRegistration = getUserDocument(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    // Pending approvals
    fun getPendingApprovals(): Flow<List<DocumentSnapshot>> = flow {
        val snapshot = firestore.collection(USERS_COLLECTION)
            .whereEqualTo("isApproved", false)
            .get()
            .await()
        emit(snapshot.documents)
    }

    // Mentors
    fun getMentors(): Flow<List<DocumentSnapshot>> = flow {
        val snapshot = firestore.collection(USERS_COLLECTION)
            .whereEqualTo("isApproved", true)
            .whereEqualTo("role", "MENTOR") // Assuming we have a role field
            .get()
            .await()
        emit(snapshot.documents)
    }

    // Member operations
    suspend fun approveMember(
        userId: String,
        mentorIds: List<String>,
        isNewMember: Boolean
    ): Boolean {
        return try {
            val updates = hashMapOf<String, Any>(
                "isApproved" to true,
                "isNewMember" to isNewMember,
                "mentorIds" to mentorIds,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun rejectMember(userId: String, reason: String?): Boolean {
        return try {
            val updates = hashMapOf<String, Any>(
                "isRejected" to true,
                "rejectionReason" to (reason ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // Meeting operations
    fun getUpcomingMeetings(): Flow<List<DocumentSnapshot>> = flow {
        val now = System.currentTimeMillis()
        val snapshot = firestore.collection(MEETINGS_COLLECTION)
            .whereGreaterThanOrEqualTo("startTime", now)
            .orderBy("startTime")
            .get()
            .await()
        emit(snapshot.documents)
    }

    // Generic document operations
    suspend fun <T : Any> addDocument(
        collection: String,
        data: T,
        documentId: String? = null
    ): String {
        return if (documentId != null) {
            firestore.collection(collection).document(documentId).set(data).await()
            documentId
        } else {
            val ref = firestore.collection(collection).document()
            ref.set(data).await()
            ref.id
        }
    }

    suspend fun deleteDocument(collection: String, documentId: String): Boolean {
        return try {
            firestore.collection(collection).document(documentId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
