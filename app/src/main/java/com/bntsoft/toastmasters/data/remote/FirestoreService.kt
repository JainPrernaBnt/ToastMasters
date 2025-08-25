package com.bntsoft.toastmasters.data.remote

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
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
    
    /**
     * Get a reference to a Firestore collection
     * @param collectionPath The path to the collection (e.g., "meetings", "users")
     * @return CollectionReference for the specified path
     */
    fun getCollection(collectionPath: String) = firestore.collection(collectionPath)

    fun getAllUsers(): Flow<List<DocumentSnapshot>> = callbackFlow {
        val subscription = firestore.collection(USERS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    trySend(snapshot.documents).isSuccess
                }
            }
        awaitClose { subscription.remove() }
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
        val collection = firestore.collection(USERS_COLLECTION)
        val results = linkedMapOf<String, DocumentSnapshot>()

        try {
            // Get all users first
            val allUsers = collection.get().await()

            // Filter for users who are either:
            // 1. Have isApproved = false
            // 2. Have approved = false
            // 3. Don't have isApproved field (new users)
            val pendingUsers = allUsers.documents.filter { doc ->
                val isApproved = doc.getBoolean("isApproved") ?: false
                !isApproved
            }

            pendingUsers.forEach { results[it.id] = it }
        } catch (e: Exception) {
            // Fallback to original behavior if there's an error
            try {
                val q1 = collection.whereEqualTo("isApproved", false).get().await()
                q1.documents.forEach { results[it.id] = it }
            } catch (_: Exception) { /* ignore */
            }

            try {
                val q2 = collection.whereEqualTo("approved", false).get().await()
                q2.documents.forEach { results[it.id] = it }
            } catch (_: Exception) { /* ignore */
            }
        }

        emit(results.values.toList())
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

    fun getApprovedMembers(): Flow<List<DocumentSnapshot>> =
        flow {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("isApproved", true)
                .get()
                .await()
            emit(snapshot.documents)
        }.catch { e ->
            // fallback logic
            try {
                val snapshot = firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("approved", true)
                    .get()
                    .await()
                emit(snapshot.documents)
            } catch (inner: Exception) {
                inner.printStackTrace()
                emit(emptyList())
            }
        }

    // Member operations
    suspend fun approveMember(
        userId: String,
        mentorNames: List<String>,
    ): Boolean {
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)

            // First get the current document to preserve existing fields
            val doc = userRef.get().await()
            val currentData = doc.data?.toMutableMap() ?: mutableMapOf()

            // Update the necessary fields
            val updates = hashMapOf<String, Any>(
                "isApproved" to true,
                "mentorNames" to mentorNames,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Merge with existing data
            currentData.putAll(updates)

            // Use set with merge to ensure all fields are updated
            userRef.set(currentData, com.google.firebase.firestore.SetOptions.merge()).await()

            // Force a read to ensure the update was applied
            val updatedDoc = userRef.get().await()
            val isApproved = updatedDoc.getBoolean("isApproved") ?: false

            isApproved
        } catch (e: Exception) {
            e.printStackTrace()
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
