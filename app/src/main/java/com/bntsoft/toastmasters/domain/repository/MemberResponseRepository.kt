package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.Flow

interface MemberResponseRepository {

    fun getMemberResponse(meetingId: String, memberId: String): Flow<MemberResponse?>

    fun getResponsesForMeeting(meetingId: String): Flow<List<MemberResponse>>

    fun getResponsesByMember(memberId: String): Flow<List<MemberResponse>>

    suspend fun saveResponse(response: MemberResponse): Result<Unit>

    suspend fun deleteResponse(meetingId: String, memberId: String): Result<Unit>

    suspend fun syncResponses(memberId: String): Result<Unit>
    
    suspend fun getBackoutMembers(meetingId: String): List<Pair<String, Long>>
}
