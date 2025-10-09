package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.data.model.GemMemberData
import com.bntsoft.toastmasters.data.model.Winner
import kotlinx.coroutines.flow.Flow

interface GemOfMonthRepository {
    
    suspend fun getMemberPerformanceData(
        year: Int,
        month: Int
    ): Flow<List<GemMemberData>>
    
    suspend fun getMeetingsForMonth(
        year: Int,
        month: Int
    ): List<String>
    
    suspend fun getAttendanceForMember(
        userId: String,
        meetingIds: List<String>
    ): GemMemberData.AttendanceData
    
    suspend fun getRoleDataForMember(
        userId: String,
        meetingIds: List<String>
    ): GemMemberData.RoleData
    
    suspend fun getAwardsForMember(
        userId: String,
        meetingIds: List<String>
    ): List<GemMemberData.Award>
    
    suspend fun getGemHistoryForMember(
        userId: String
    ): List<String>
    
    suspend fun saveGemOfTheMonth(
        meetingId: String,
        userId: String,
        year: Int,
        month: Int
    ): Result<Unit>
    
    suspend fun getGemOfTheMonth(
        year: Int,
        month: Int
    ): com.bntsoft.toastmasters.data.model.GemOfTheMonth?
    
    suspend fun getExternalActivitiesForMember(
        userId: String,
        year: Int,
        month: Int
    ): List<GemMemberData.ExternalActivity>

}
