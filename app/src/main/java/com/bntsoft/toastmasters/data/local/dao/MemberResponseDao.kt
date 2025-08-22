package com.bntsoft.toastmasters.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bntsoft.toastmasters.data.local.entity.MemberResponseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberResponseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResponse(response: MemberResponseEntity)

    @Query("SELECT * FROM member_responses WHERE meetingId = :meetingId AND memberId = :memberId")
    suspend fun getResponse(meetingId: String, memberId: String): MemberResponseEntity?

    @Query("SELECT * FROM member_responses WHERE meetingId = :meetingId")
    fun getResponsesForMeeting(meetingId: String): Flow<List<MemberResponseEntity>>

    @Query("SELECT * FROM member_responses WHERE memberId = :memberId")
    fun getResponsesByMember(memberId: String): Flow<List<MemberResponseEntity>>

    @Delete
    suspend fun deleteResponse(response: MemberResponseEntity)

    @Query("DELETE FROM member_responses WHERE meetingId = :meetingId")
    suspend fun deleteResponsesForMeeting(meetingId: String)

    @Query("DELETE FROM member_responses WHERE meetingId = :meetingId AND memberId = :memberId")
    suspend fun deleteResponse(meetingId: String, memberId: String)

    @Query("SELECT lastUpdated FROM member_responses WHERE meetingId = :meetingId AND memberId = :memberId")
    suspend fun getLastUpdated(meetingId: String, memberId: String): Long?
}
