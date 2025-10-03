package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.model.NotificationData
import com.bntsoft.toastmasters.data.model.UserDeserializer
import com.bntsoft.toastmasters.data.remote.FirestoreService
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.domain.repository.NotificationRepository
import com.bntsoft.toastmasters.domain.usecase.notification.NotificationTriggerUseCase
import com.bntsoft.toastmasters.utils.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val notificationRepository: NotificationRepository,
    private val notificationTriggerUseCase: NotificationTriggerUseCase,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) : MemberRepository {

    override fun getPendingApprovals(): Flow<List<User>> {
        return firestoreService.getPendingApprovals().map { documents ->
            documents.mapNotNull { doc ->
                UserDeserializer.fromDocument(doc)
            }
        }
    }

    override suspend fun approveMember(userId: String, mentorNames: List<String>): Boolean {
        return try {
            // First update mentor names if provided
            if (mentorNames.isNotEmpty()) {
                val updates = mapOf(
                    "mentorNames" to mentorNames,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                firestoreService.getUserDocument(userId).update(updates).await()
            }

            // Then update approval status
            var updateSuccess = firestoreService.approveMember(userId, mentorNames)

            if (updateSuccess) {
                // Double-check the approval status was actually updated
                val userDoc = firestoreService.getUserDocument(userId).get().await()
                val isApproved =
                    userDoc.getBoolean("isApproved") ?: userDoc.getBoolean("approved") ?: false

                if (!isApproved) {
                    // If not approved, try a direct update
                    val updates = mapOf(
                        "isApproved" to true,
                        "approved" to true,
                        "mentorNames" to mentorNames,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    firestoreService.getUserDocument(userId).update(updates).await()

                    // Verify again
                    val updatedDoc = firestoreService.getUserDocument(userId).get().await()
                    updateSuccess =
                        updatedDoc.getBoolean("isApproved") ?: updatedDoc.getBoolean("approved")
                                ?: false
                }

                if (updateSuccess) {
                    // Send notification only if update was successful
                    try {
                        val userDoc = firestoreService.getUserDocument(userId).get().await()
                        val userName = userDoc.getString("name") ?: "Member"
                        notificationTriggerUseCase.notifyMemberOfRequestApproval(
                            memberId = userId,
                            memberName = userName,
                            requestType = "membership"
                        )
                    } catch (e: Exception) {
                        // Log error but don't fail the operation
                        Log.e("MemberRepository", "Error sending approval notification", e)
                    }
                }
            }

            updateSuccess
        } catch (e: Exception) {
            Log.e("MemberRepository", "Error in getMemberById", e)
            false
        }
    }

    override suspend fun rejectMember(userId: String, reason: String?): Boolean {
        return try {
            // Get user data before rejection (since it will be deleted from users collection)
            val userDoc = firestoreService.getUserDocument(userId).get().await()
            val userName = userDoc.getString("name") ?: "Member"
            
            // Perform rejection (moves user to rejectedMembers collection and deletes from users)
            val ok = firestoreService.rejectMember(userId, reason)
            
            if (ok) {
                try {
                    // Send notification after successful rejection
                    notificationTriggerUseCase.notifyMemberOfRequestRejection(
                        memberId = userId,
                        memberName = userName,
                        requestType = "membership",
                        reason = reason
                    )
                } catch (e: Exception) {
                    Log.e("MemberRepository", "Error sending rejection notification", e)
                }
            }
            
            ok
        } catch (e: Exception) {
            Log.e("MemberRepository", "Error in rejectMember", e)
            false
        }
    }

    override suspend fun updateMember(user: User): Boolean {
        return try {
            val updates = hashMapOf<String, Any>(
                "mentorNames" to user.mentorNames,
                "role" to user.role.toString(),
                "vpEducation" to (user.role == com.bntsoft.toastmasters.domain.models.UserRole.VP_EDUCATION),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            firestoreService.getUserDocument(user.id).update(updates).await()
            true
        } catch (e: Exception) {
            Log.e("MemberRepository", "Error in getMemberById", e)
            false
        }
    }

    override fun getAllMembers(includePending: Boolean): Flow<List<User>> = flow {
        if (includePending) {
            // Get all users including pending approvals
            val pendingUsers = firestoreService.getPendingApprovals()
                .map { documents ->
                    documents.mapNotNull { doc ->
                        UserDeserializer.fromDocument(doc)
                    }
                }
                .catch { e ->
                    Log.e("MemberRepository", "Error in getMemberById", e)
                    emit(emptyList())
                }

            val approvedUsers = firestoreService.getApprovedMembers()
                .map { documents ->
                    documents.mapNotNull { doc ->
                        UserDeserializer.fromDocument(doc)
                    }
                }
                .catch { e ->
                    Log.e("MemberRepository", "Error in getMemberById", e)
                    emit(emptyList())
                }

            val combined = combine(pendingUsers, approvedUsers) { pending, approved ->
                (pending + approved).distinctBy { it.id }
            }

            combined.collect { users ->
                emit(users)
            }
        } else {
            // Get only approved users (both members and mentors)
            firestoreService.getApprovedMembers()
                .map { documents ->
                    documents.mapNotNull { doc ->
                        UserDeserializer.fromDocument(doc)
                    }
                }
                .catch { e ->
                    Log.e("MemberRepository", "Error in getMemberById", e)
                    emit(emptyList())
                }
                .collect { users ->
                    emit(users)
                }
        }
    }

    override fun getMentors(): Flow<List<User>> {
        return firestoreService.getMentors().map { documents ->
            documents.mapNotNull { doc ->
                UserDeserializer.fromDocument(doc)
            }
        }
    }
    override suspend fun getMemberById(userId: String): User? {
        return try {
            val userDoc = firestoreService.getUserDocument(userId).get().await()
            if (userDoc.exists()) {
                UserDeserializer.fromDocument(userDoc)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MemberRepository", "Error in getMemberById", e)
            null
        }
    }

    override suspend fun getMemberByEmail(email: String): User? {
        return try {
            val querySnapshot = firestoreService.getUserByEmail(email)
            val firstDoc = querySnapshot.documents.firstOrNull() ?: return null
            UserDeserializer.fromDocument(firstDoc)
        } catch (e: Exception) {
            Log.e("MemberRepository", "Error in getMemberByEmail", e)
            null
        }
    }

    override suspend fun getMemberByPhone(phoneNumber: String): User? {
        return try {
            val querySnapshot = firestoreService.getUserByPhone(phoneNumber)
            val firstDoc = querySnapshot.documents.firstOrNull() ?: return null
            UserDeserializer.fromDocument(firstDoc)
        } catch (e: Exception) {
            Log.e("MemberRepository", "Error in getMemberByPhone", e)
            null
        }
    }

    override fun observeMember(userId: String): Flow<User?> {
        return firestoreService.observeUser(userId)
            .map { document ->
                if (document.exists()) {
                    UserDeserializer.fromDocument(document)
                } else {
                    null
                }
            }
            .catch { e ->
                Log.e("MemberRepository", "Error observing user: $userId", e)
                emit(null)
            }
    }
}
