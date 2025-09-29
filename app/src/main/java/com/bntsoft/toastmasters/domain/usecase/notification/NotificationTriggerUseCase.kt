package com.bntsoft.toastmasters.domain.usecase.notification

import com.bntsoft.toastmasters.data.model.NotificationData
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.User
import com.bntsoft.toastmasters.domain.repository.NotificationRepository
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.bntsoft.toastmasters.utils.NotificationHelper
import com.bntsoft.toastmasters.utils.Result
import javax.inject.Inject

class NotificationTriggerUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) {

    suspend fun notifyVPEducationOfNewMemberSignup(newMember: User): Result<Unit> {
        return Result.runCatching {
            android.util.Log.d("NotificationTriggerUseCase", "Creating notification for VP Education about new member: ${newMember.name}")
            
            val notification = NotificationData(
                title = "New Member Signup",
                message = "${newMember.name} has signed up and is awaiting approval",
                type = NotificationHelper.TYPE_NEW_MEMBER_SIGNUP,
                data = mapOf(
                    "userId" to newMember.id,
                    "userName" to newMember.name,
                    "userEmail" to newMember.email
                )
            )

            android.util.Log.d("NotificationTriggerUseCase", "Sending notification to VP_EDUCATION role")
            val success = notificationRepository.sendNotificationToRole("VP_EDUCATION", notification)
            android.util.Log.d("NotificationTriggerUseCase", "Notification send result: $success")
            
            if (!success) {
                throw Exception("Failed to notify VP Education of new member signup")
            }
        }
    }

    suspend fun notifyVPEducationOfMemberBackout(
        meetingId: String,
        meetingTheme: String,
        memberName: String,
        roleName: String,
        memberId: String
    ): Result<Unit> {
        return Result.runCatching {
            val notification = NotificationData(
                title = "Member Backed Out",
                message = "$memberName has backed out from the role '$roleName' for meeting '$meetingTheme'",
                type = NotificationHelper.TYPE_MEMBER_BACKOUT,
                data = mapOf(
                    "meetingId" to meetingId,
                    "memberId" to memberId,
                    "memberName" to memberName,
                    "roleName" to roleName,
                    "meetingTheme" to meetingTheme
                )
            )

            val success = notificationRepository.sendNotificationToRole("VP_EDUCATION", notification)
            if (!success) {
                throw Exception("Failed to notify VP Education of member backout")
            }
        }
    }

    suspend fun notifyMemberOfRequestApproval(
        memberId: String,
        memberName: String,
        requestType: String = "membership"
    ): Result<Unit> {
        return Result.runCatching {
            val notification = NotificationData(
                title = "Request Approved",
                message = "Congratulations! Your $requestType request has been approved by VP Education",
                type = NotificationHelper.TYPE_REQUEST_APPROVED,
                receiverId = memberId,
                data = mapOf(
                    "requestType" to requestType,
                    "memberName" to memberName
                )
            )

            val success = notificationRepository.sendNotificationToUser(memberId, notification)
            if (!success) {
                throw Exception("Failed to notify member of request approval")
            }
        }
    }

    suspend fun notifyMemberOfRequestRejection(
        memberId: String,
        memberName: String,
        requestType: String = "membership",
        reason: String? = null
    ): Result<Unit> {
        return Result.runCatching {
            val message = if (reason != null) {
                "Your $requestType request has been rejected. Reason: $reason"
            } else {
                "Your $requestType request has been rejected by VP Education"
            }

            val notification = NotificationData(
                title = "Request Rejected",
                message = message,
                type = NotificationHelper.TYPE_REQUEST_REJECTED,
                receiverId = memberId,
                data = mapOf(
                    "requestType" to requestType,
                    "memberName" to memberName,
                    "reason" to (reason ?: "")
                )
            )

            val success = notificationRepository.sendNotificationToUser(memberId, notification)
            if (!success) {
                throw Exception("Failed to notify member of request rejection")
            }
        }
    }

    suspend fun notifyAllMembersOfNewMeeting(meeting: Meeting): Result<Unit> {
        return Result.runCatching {
            val notification = NotificationData(
                title = "New Meeting Created",
                message = "A new meeting '${meeting.theme}' has been scheduled for ${meeting.dateTime}",
                type = NotificationHelper.TYPE_MEETING_CREATED,
                data = mapOf(
                    "meetingId" to meeting.id,
                    "meetingTitle" to meeting.theme,
                    "meetingDate" to meeting.dateTime.toString(),
                    "meetingLocation" to (meeting.location ?: "")
                )
            )

            // Send only to MEMBER role users, not VP_EDUCATION
            val success = notificationRepository.sendNotificationToRole("MEMBER", notification)
            if (!success) {
                throw Exception("Failed to notify members of new meeting")
            }
        }
    }

    suspend fun notifyAllMembersOfMeetingUpdate(meeting: Meeting): Result<Unit> {
        return Result.runCatching {
            val notification = NotificationData(
                title = "Meeting Updated",
                message = "Meeting '${meeting.theme}' has been updated. Please check the latest details",
                type = NotificationHelper.TYPE_MEETING_UPDATED,
                data = mapOf(
                    "meetingId" to meeting.id,
                    "meetingTitle" to meeting.theme,
                    "meetingDate" to meeting.dateTime.toString(),
                    "meetingLocation" to (meeting.location ?: "")
                )
            )

            // Send only to MEMBER role users, not VP_EDUCATION
            val success = notificationRepository.sendNotificationToRole("MEMBER", notification)
            if (!success) {
                throw Exception("Failed to notify members of meeting update")
            }
        }
    }
}
