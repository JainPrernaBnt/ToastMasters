package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.domain.model.ExternalClubActivity
import kotlinx.coroutines.flow.Flow

interface ExternalClubActivityRepository {
    suspend fun addActivity(activity: ExternalClubActivity): Result<String>
    suspend fun updateActivity(activity: ExternalClubActivity): Result<Unit>
    suspend fun deleteActivity(activityId: String): Result<Unit>
    suspend fun getActivityById(activityId: String): Result<ExternalClubActivity?>
    fun getAllActivities(): Flow<List<ExternalClubActivity>>
    fun getUserActivities(userId: String): Flow<List<ExternalClubActivity>>
}
