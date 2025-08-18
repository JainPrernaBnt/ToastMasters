package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.local.dao.MemberResponseDao
import com.bntsoft.toastmasters.data.mapper.MemberResponseMapper
import com.bntsoft.toastmasters.data.remote.FirebaseMemberResponseDataSource
import com.bntsoft.toastmasters.data.remote.dto.MemberResponseDto
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.repository.MemberResponseRepository
import com.bntsoft.toastmasters.utils.Resource
import com.bntsoft.toastmasters.utils.Result
import com.bntsoft.toastmasters.utils.networkBoundResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberResponseRepositoryImpl @Inject constructor(
    private val localDataSource: MemberResponseDao,
    private val remoteDataSource: FirebaseMemberResponseDataSource,
    private val mapper: MemberResponseMapper
) : MemberResponseRepository {

    override fun getMemberResponse(meetingId: Int, memberId: String): Flow<MemberResponse?> {
        return localDataSource.getResponse(meetingId, memberId).map { entity ->
            entity?.let { mapper.mapToDomain(it) }
        }
    }

    override fun getResponsesForMeeting(meetingId: Int): Flow<List<MemberResponse>> {
        return localDataSource.getResponsesForMeeting(meetingId).map { entities ->
            entities.map { mapper.mapToDomain(it) }
        }
    }

    override fun getResponsesByMember(memberId: String): Flow<List<MemberResponse>> {
        return localDataSource.getResponsesByMember(memberId).map { entities ->
            entities.map { mapper.mapToDomain(it) }
        }
    }

    override suspend fun saveResponse(response: MemberResponse): Result<Unit> {
        return try {
            // Convert domain model to DTO for remote and entity for local
            val dto = mapper.mapToDto(response)
            val entity = mapper.mapToEntity(response)

            // Save to local database first for immediate UI update
            localDataSource.upsertResponse(entity)

            // Then save to remote database
            remoteDataSource.saveResponse(dto)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteResponse(meetingId: Int, memberId: String): Result<Unit> {
        return try {
            // Delete from remote
            remoteDataSource.deleteResponse(meetingId, memberId)

            // Delete from local
            val response = localDataSource.getResponse(meetingId, memberId).firstOrNull()
            response?.let { localDataSource.deleteResponse(it) }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun syncResponses(memberId: String): Result<Unit> {
        return Result.runCatching {
            // Get the latest responses from remote
            val remoteResponses = remoteDataSource.getResponsesByMember(memberId)

            // Update local database
            remoteResponses.forEach { remoteResponse ->
                val localLastUpdated = localDataSource.getLastUpdated(
                    remoteResponse.meetingId,
                    remoteResponse.memberId
                )

                // Only update if the remote is newer or if we don't have a local copy
                if (localLastUpdated == null || remoteResponse.lastUpdated > localLastUpdated) {
                    val entity = mapper.mapToEntity(mapper.mapToDomain(remoteResponse))
                    localDataSource.upsertResponse(entity)
                }

            }

            // TODO: Handle conflicts (local changes not pushed to remote)
            // This would require tracking changes that haven't been synced yet
        }
    }

    // Network-bound resource implementation for automatic data refresh
    private fun getResponseWithRefresh(
        meetingId: Int,
        memberId: String,
        forceRefresh: Boolean = false
    ): Flow<Resource<MemberResponse>> {
        return networkBoundResource(
            query = {
                localDataSource.getResponse(meetingId, memberId)
                    .map { entity -> entity?.let { mapper.mapToDomain(it) } }
            },
            fetch = { remoteDataSource.getResponse(meetingId, memberId) },
            saveFetchResult = { responseDto ->
                responseDto?.let { dto ->
                    val entity = mapper.mapToEntity(mapper.mapToDomain(dto))
                    localDataSource.upsertResponse(entity)
                }
            },
            shouldFetch = { cached ->
                forceRefresh || cached == null
            },
            mapResult = { cached: MemberResponse? ->
                cached ?: throw IllegalStateException("Cached response cannot be null")
            }
        )
    }
}
