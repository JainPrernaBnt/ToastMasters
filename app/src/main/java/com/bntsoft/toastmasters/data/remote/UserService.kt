package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.model.UserDeserializer
import com.bntsoft.toastmasters.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val USERS_COLLECTION = "users"
    }

    private val usersCollection = firestore.collection(USERS_COLLECTION)
    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun getCurrentUser(): User? {
        val currentUser = firebaseAuth.currentUser ?: return null
        return try {
            val document = usersCollection.document(currentUser.uid).get().await()
            UserDeserializer.fromDocument(document)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            val document = usersCollection.document(userId).get().await()
            UserDeserializer.fromDocument(document)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getUserByEmail(email: String): User? {
        return try {
            val querySnapshot = usersCollection
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            val document = querySnapshot.documents.firstOrNull()
            document?.let { UserDeserializer.fromDocument(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateUser(user: User) {
        try {
            val updates = mapOf(
                "name" to user.name,
                "email" to user.email,
                "phoneNumber" to user.phoneNumber,
                "address" to user.address,
                "level" to user.level,
                "gender" to user.gender,
                "toastmastersId" to user.toastmastersId,
                "clubId" to user.clubId,
                "role" to user.role,
                "isApproved" to user.isApproved,
                "mentorNames" to user.mentorNames,
                "fcmToken" to user.fcmToken,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            usersCollection.document(user.id).update(updates).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteUser(userId: String) {
        try {
            usersCollection.document(userId).delete().await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun searchUsers(query: String): List<User> {
        return try {
            // Search by name (case-insensitive)
            val nameQuery = usersCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .get()
                .await()

            // Search by email
            val emailQuery = usersCollection
                .whereGreaterThanOrEqualTo("email", query)
                .whereLessThanOrEqualTo("email", query + "\uf8ff")
                .get()
                .await()

            // Combine results and remove duplicates
            val allDocuments = (nameQuery.documents + emailQuery.documents).distinctBy { it.id }

            allDocuments.mapNotNull { UserDeserializer.fromDocument(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun updateUserRole(userId: String, role: String) {
        try {
            val updates = mapOf(
                "role" to role,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            usersCollection.document(userId).update(updates).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateUserStatus(userId: String, isActive: Boolean) {
        try {
            val updates = mapOf(
                "isApproved" to isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            usersCollection.document(userId).update(updates).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateFcmToken(userId: String, token: String) {
        try {
            val updates = mapOf(
                "fcmToken" to token,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            usersCollection.document(userId).update(updates).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun removeFcmToken(userId: String) {
        try {
            val updates = mapOf(
                "fcmToken" to FieldValue.delete(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            usersCollection.document(userId).update(updates).await()
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun getAvailableMembersForMeeting(meetingId: String): List<User> {
        return try {
            // Get all members who marked themselves as available for this meeting
            val availabilityRef = firestore
                .collection("meetings")
                .document(meetingId)
                .collection("availability")
            
            println("DEBUG: Querying availability for meeting: $meetingId")
            val querySnapshot = availabilityRef
                .whereEqualTo("status", "AVAILABLE")
                .get()
                .await()
            
            val availabilityDocs = querySnapshot.documents
            println("DEBUG: Found ${availabilityDocs.size} available members")
            
            if (availabilityDocs.isEmpty()) {
                println("DEBUG: No available members found for meeting $meetingId")
                return emptyList()
            }
            
            // Get user IDs from the availability documents (document ID is the memberId)
            val memberIds = availabilityDocs.map { it.id }
            println("DEBUG: Found member IDs: $memberIds")
            
            // Fetch user details in batches to avoid too many requests
            val users = mutableListOf<User>()
            for (batch in memberIds.chunked(10)) { // Process 10 users at a time
                println("DEBUG: Fetching users for batch: $batch")
                val querySnapshot = usersCollection
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                    .get()
                    .await()
                
                val batchUsers = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        UserDeserializer.fromDocument(doc)
                    } catch (e: Exception) {
                        println("DEBUG: Error parsing user ${doc.id}: ${e.message}")
                        null
                    }
                }
                users.addAll(batchUsers)
            }
            
            println("DEBUG: Successfully fetched ${users.size} users")
            users
        } catch (e: Exception) {
            println("DEBUG: Error in getAvailableMembersForMeeting: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getUserRecentRoles(userId: String, limit: Int): List<String> {
        return try {
            // Query the user's role assignments, ordered by date descending
            val querySnapshot = firestore
                .collection("role_assignments")
                .whereEqualTo("userId", userId)
                .whereNotEqualTo("role", "") // Exclude empty roles if any
                .orderBy("assignedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            // Extract and return the role names
            querySnapshot.documents.mapNotNull { doc ->
                doc.getString("role")
            }.distinct() // Ensure we don't have duplicate roles
            
        } catch (e: Exception) {
            println("DEBUG: Error fetching recent roles for user $userId: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

}
