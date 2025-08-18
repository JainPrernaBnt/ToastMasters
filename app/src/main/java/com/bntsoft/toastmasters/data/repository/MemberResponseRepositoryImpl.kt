package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.mapper.MemberResponseMapper
import com.bntsoft.toastmasters.data.remote.FirebaseMemberResponseDataSource
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberResponseRepositoryImpl @Inject constructor(
    private val remoteDataSource: FirebaseMemberResponseDataSource,
    private val mapper: MemberResponseMapper
) : MemberResponseRepository {

    override fun getMemberResponse(meetingId: Int, memberId: String): Flow<MemberResponse?> {
        return remoteDataSource.observeResponse(meetingId, memberId).map { dto ->
            dto?.let { mapper.mapToDomain(it) }
        }
    }

    override fun getResponsesForMeeting(meetingId: Int): Flow<List<MemberResponse>> {
        return remoteDataSource.observeResponsesForMeeting(meetingId).map { dtos ->
            dtos.map { mapper.mapToDomain(it) }
        }
    }

    override fun getResponsesByMember(memberId: String): Flow<List<MemberResponse>> {
        return kotlinx.coroutines.flow.flow {
            val dtos = remoteDataSource.getResponsesByMember(memberId)
            emit(dtos.map { mapper.mapToDomain(it) })
        }
    }

    override suspend fun saveResponse(response: MemberResponse): Result<Unit> {
        return try {
            val dto = mapper.mapToDto(response)
            remoteDataSource.saveResponse(dto)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteResponse(meetingId: Int, memberId: String): Result<Unit> {
        return try {
            remoteDataSource.deleteResponse(meetingId, memberId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun syncResponses(memberId: String): Result<Unit> {
        // No local cache to sync, so this does nothing.
        return Result.Success(Unit)
    }
}
