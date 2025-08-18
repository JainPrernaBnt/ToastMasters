package com.bntsoft.toastmasters.domain.repository

import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.Flow

interface MemberResponseRepository {

    fun getMemberResponse(meetingId: Int, memberId: String): Flow<MemberResponse?>

    fun getResponsesForMeeting(meetingId: Int): Flow<List<MemberResponse>>

    fun getResponsesByMember(memberId: String): Flow<List<MemberResponse>>

    suspend fun saveResponse(response: MemberResponse): Result<Unit>

    suspend fun deleteResponse(meetingId: Int, memberId: String): Result<Unit>

    suspend fun syncResponses(memberId: String): Result<Unit>
}
