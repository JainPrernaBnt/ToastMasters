package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.remote.dto.MemberResponseDto

interface FirebaseMemberResponseDataSource {

    suspend fun getResponse(meetingId: String, memberId: String): MemberResponseDto?

    suspend fun getResponsesForMeeting(meetingId: String): List<MemberResponseDto>

    suspend fun getResponsesByMember(memberId: String): List<MemberResponseDto>

    suspend fun saveResponse(response: MemberResponseDto)

    suspend fun deleteResponse(meetingId: String, memberId: String)

    fun observeResponse(
        meetingId: String,
        memberId: String
    ): kotlinx.coroutines.flow.Flow<MemberResponseDto?>

    fun observeResponsesForMeeting(meetingId: String): kotlinx.coroutines.flow.Flow<List<MemberResponseDto>>
}
