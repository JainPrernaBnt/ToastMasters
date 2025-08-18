package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.data.remote.dto.MemberResponseDto

interface FirebaseMemberResponseDataSource {

    suspend fun getResponse(meetingId: Int, memberId: String): MemberResponseDto?

    suspend fun getResponsesForMeeting(meetingId: Int): List<MemberResponseDto>

    suspend fun getResponsesByMember(memberId: String): List<MemberResponseDto>

    suspend fun saveResponse(response: MemberResponseDto)

    suspend fun deleteResponse(meetingId: Int, memberId: String)

    fun observeResponse(meetingId: Int, memberId: String): kotlinx.coroutines.flow.Flow<MemberResponseDto?>

    fun observeResponsesForMeeting(meetingId: Int): kotlinx.coroutines.flow.Flow<List<MemberResponseDto>>
}
