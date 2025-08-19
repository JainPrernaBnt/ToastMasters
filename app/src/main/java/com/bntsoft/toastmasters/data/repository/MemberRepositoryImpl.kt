package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.remote.FirestoreService
import com.bntsoft.toastmasters.data.model.NotificationData
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.MemberRepository
import com.bntsoft.toastmasters.domain.repository.NotificationRepository
import com.bntsoft.toastmasters.utils.NotificationHelper
import kotlinx.coroutines.flow.Flow
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

    override suspend fun approveMember(userId: String, mentorNames: List<String>, isNewMember: Boolean): Boolean {
        val ok = firestoreService.approveMember(userId, mentorNames, isNewMember)
        if (ok) {
            val notification = NotificationData(
                title = "Membership approved",
                message = "Your account has been approved. You can now sign in and use the app.",
                type = NotificationHelper.TYPE_MEMBER_APPROVAL,
                data = mapOf(NotificationHelper.EXTRA_USER_ID to userId)
            )
            try { notificationRepository.sendNotificationToUser(userId, notification) } catch (_: Exception) {}
        }
        return ok
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
            try { notificationRepository.sendNotificationToUser(userId, notification) } catch (_: Exception) {}
        }
        return ok
    }

    override fun getAllMembers(includePending: Boolean): Flow<List<User>> = flow {
        try {
            // Create a flow that gets all users or only approved users
            if (includePending) {
                // Get all users - we'll need to add this method to FirestoreService
                // For now, combine pending approvals and mentors
                val pendingUsers = mutableListOf<User>()
                firestoreService.getPendingApprovals().collect { documents ->
                    pendingUsers.addAll(documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    })
                }
                firestoreService.getMentors().collect { documents ->
                    pendingUsers.addAll(documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    })
                }
                emit(pendingUsers.distinctBy { it.id })
            } else {
                // Get only approved users (mentors)
                firestoreService.getMentors().collect { documents ->
                    val users = documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
                    emit(users)
                }
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getMentors(): Flow<List<User>> {
        return firestoreService.getMentors().map { documents ->
            documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }
        }
    }

    override suspend fun updateMember(user: User): Boolean {
        return try {
            firestoreService.setUserDocument(user.id, user)
            true
        } catch (e: Exception) {
            false
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
