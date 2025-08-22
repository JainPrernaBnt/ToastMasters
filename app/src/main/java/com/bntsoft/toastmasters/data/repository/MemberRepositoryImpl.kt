package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.model.NotificationData
import com.bntsoft.toastmasters.data.remote.FirestoreService
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.domain.repository.NotificationRepository
import com.bntsoft.toastmasters.utils.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val notificationRepository: NotificationRepository
) : MemberRepository {

    override fun getPendingApprovals(): Flow<List<User>> {
        return firestoreService.getPendingApprovals().map { documents ->
            documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
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
                    val notification = NotificationData(
                        title = "Membership approved",
                        message = "Your account has been approved. You can now sign in and use the app.",
                        type = NotificationHelper.TYPE_MEMBER_APPROVAL,
                        data = mapOf(NotificationHelper.EXTRA_USER_ID to userId)
                    )
                    try {
                        notificationRepository.sendNotificationToUser(userId, notification)
                    } catch (e: Exception) {
                        // Log error but don't fail the operation
                        e.printStackTrace()
                    }
                }
            }

            updateSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun rejectMember(userId: String, reason: String?): Boolean {
        val ok = firestoreService.rejectMember(userId, reason)
        if (ok) {
            val msg = if (!reason.isNullOrBlank()) {
                "Your account has been rejected. Reason: $reason"
            } else {
                "Your account has been rejected. Please contact club officers."
            }
            val notification = NotificationData(
                title = "Membership rejected",
                message = msg,
                type = NotificationHelper.TYPE_MEMBER_APPROVAL,
                data = mapOf(NotificationHelper.EXTRA_USER_ID to userId)
            )
            try {
                notificationRepository.sendNotificationToUser(userId, notification)
            } catch (_: Exception) {
            }
        }
        return ok
    }

    override suspend fun updateMember(user: User): Boolean {
        return try {
            val updates = mapOf(
                "mentorNames" to user.mentorNames,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            firestoreService.getUserDocument(user.id).update(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getAllMembers(includePending: Boolean): Flow<List<User>> = flow {
        if (includePending) {
            // Get all users including pending approvals
            val pendingUsers = firestoreService.getPendingApprovals()
                .map { documents ->
                    documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
                }
                .catch { e ->
                    e.printStackTrace()
                    emit(emptyList())
                }

            val approvedUsers = firestoreService.getApprovedMembers()
                .map { documents ->
                    documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
                }
                .catch { e ->
                    e.printStackTrace()
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
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
                }
                .catch { e ->
                    e.printStackTrace()
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
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }
        }
    }

    override suspend fun getMemberById(userId: String): User? {
        return try {
            val userDoc = firestoreService.getUserDocument(userId).get().await()
            userDoc.toObject(User::class.java)?.copy(id = userId)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getMemberByEmail(email: String): User? {
        return try {
            val querySnapshot = firestoreService.getUserByEmail(email)
            val firstDoc = querySnapshot.documents.firstOrNull()
            firstDoc?.toObject(User::class.java)?.copy(id = firstDoc.id)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getMemberByPhone(phoneNumber: String): User? {
        return try {
            val querySnapshot = firestoreService.getUserByPhone(phoneNumber)
            val firstDoc = querySnapshot.documents.firstOrNull()
            firstDoc?.toObject(User::class.java)?.copy(id = firstDoc.id)
        } catch (e: Exception) {
            null
        }
    }

    override fun observeMember(userId: String): Flow<User?> {
        return firestoreService.observeUser(userId).map { document ->
            document.toObject(User::class.java)?.copy(id = userId)
        }
    }
}
