package com.bntsoft.toastmasters.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class GemOfTheMonth(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val memberName: String = "",
    val year: Int = 0,
    val month: Int = 0,
    val performanceScore: Int = 0,
    val attendanceData: AttendanceData = AttendanceData(),
    val roleData: RoleData = RoleData(),
    val awards: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    data class AttendanceData(
        val attendedMeetings: Int = 0,
        val totalMeetings: Int = 0
    )
    
    data class RoleData(
        val speakerCount: Int = 0,
        val evaluatorCount: Int = 0,
        val otherRolesCount: Int = 0,
        val recentRoles: List<String> = emptyList()
    )
    
    fun getMonthYearString(): String {
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return "${monthNames[month - 1]} $year"
    }
}
