package com.bntsoft.toastmasters.data.local.dao

import androidx.room.*
import com.bntsoft.toastmasters.data.local.entity.MemberResponseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberResponseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResponse(response: MemberResponseEntity)

    @Query("SELECT * FROM member_responses WHERE meetingId = :meetingId AND memberId = :memberId")
    fun getResponse(meetingId: String, memberId: String): Flow<MemberResponseEntity?>

    @Query("SELECT * FROM member_responses WHERE meetingId = :meetingId")
    fun getResponsesForMeeting(meetingId: String): Flow<List<MemberResponseEntity>>

    @Query("SELECT * FROM member_responses WHERE memberId = :memberId")
    fun getResponsesByMember(memberId: String): Flow<List<MemberResponseEntity>>

    @Delete
    suspend fun deleteResponse(response: MemberResponseEntity)

    @Query("DELETE FROM member_responses WHERE meetingId = :meetingId")
    suspend fun deleteResponsesForMeeting(meetingId: String)

    @Query("SELECT lastUpdated FROM member_responses WHERE meetingId = :meetingId AND memberId = :memberId")
    suspend fun getLastUpdated(meetingId: String, memberId: String): Long?
}
