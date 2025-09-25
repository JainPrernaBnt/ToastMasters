package com.bntsoft.toastmasters.data.model

import com.bntsoft.toastmasters.domain.models.WinnerCategory

data class GemMemberData(
    val user: User,
    val attendanceData: AttendanceData,
    val roleData: RoleData,
    val awards: List<Award>,
    val gemHistory: List<String>,
    val performanceScore: Int
) {
    data class AttendanceData(
        val attendedMeetings: Int,
        val totalMeetings: Int
    ) {
        val attendancePercentage: Float
            get() = if (totalMeetings > 0) (attendedMeetings.toFloat() / totalMeetings) * 100 else 0f

    }
    
    data class RoleData(
        val recentRoles: List<String>,
        val speakerCount: Int,
        val evaluatorCount: Int,
        val otherRolesCount: Int,
        val allRoles: List<RoleAssignment>
    ) {
        val totalRoles: Int
            get() = speakerCount + evaluatorCount + otherRolesCount
    }
    
    data class RoleAssignment(
        val meetingId: String,
        val roleName: String,
        val meetingDate: String
    )
    
    data class Award(
        val category: WinnerCategory,
        val meetingId: String,
        val meetingDate: String
    ) {
        val displayName: String
            get() = category.displayName
    }
    
    val isEligible: Boolean
        get() = attendanceData.attendedMeetings > 0
}
