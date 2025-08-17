package com.bntsoft.toastmasters.utils

import com.bntsoft.toastmasters.data.remote.dto.MemberResponseDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

abstract class NetworkBoundResource<ResultType, RequestType> {

    private val result = MutableStateFlow<Resource<ResultType>>(Resource.Loading())

    fun asFlow(): Flow<Resource<ResultType>> = result.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            result.value = Resource.Loading()

            val dbValue = loadFromDb()
            dbValue?.let { setValue(Resource.Success(it)) }

            if (shouldFetch(dbValue)) {
                fetchFromNetwork(dbValue)
            }
        }
    }

    protected open fun onFetchFailed() {}

    protected abstract suspend fun saveNetworkResult(item: RequestType)

    protected abstract fun shouldFetch(data: ResultType?): Boolean

    protected abstract suspend fun loadFromDb(): ResultType?

    protected abstract suspend fun createCall(): Flow<ApiResponse<RequestType>>

    private fun setValue(newValue: Resource<ResultType>) {
        if (result.value != newValue) {
            result.value = newValue
        }
    }

    private suspend fun fetchFromNetwork(dbValue: ResultType?) {
        setValue(Resource.Loading(dbValue))

        try {
            createCall()
                .catch { e ->
                    setValue(Resource.Error(e.message ?: "Unknown error", dbValue))
                    onFetchFailed()
                }
                .collect { apiResponse ->
                    when (apiResponse) {
                        is ApiSuccessResponse -> {
                            saveNetworkResult(apiResponse.body)
                            val newData = loadFromDb()
                            newData?.let { setValue(Resource.Success(it)) }
                        }
                        is ApiEmptyResponse -> {
                            dbValue?.let { setValue(Resource.Success(it)) }
                        }
                        is ApiErrorResponse -> {
                            onFetchFailed()
                            setValue(Resource.Error(apiResponse.errorMessage, dbValue))
                        }
                    }
                }
        } catch (e: Exception) {
            setValue(Resource.Error(e.message ?: "Unknown error", dbValue))
            onFetchFailed()
        }
    }
}

inline fun <ResultType, RequestType, DomainType> networkBoundResource(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> RequestType,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType) -> Boolean = { true },
    crossinline mapResult: (ResultType) -> DomainType
): Flow<Resource<DomainType>> = flow {
    emit(Resource.Loading())

    val data = query().first()
    emit(Resource.Success(mapResult(data)))

    if (shouldFetch(data)) {
        emit(Resource.Loading(mapResult(data)))
        try {
            val response = fetch()
            saveFetchResult(response)
            val updatedData = query().first()
            emit(Resource.Success(mapResult(updatedData)))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error", mapResult(data)))
        }
    } else {
        emit(Resource.Success(mapResult(data)))
    }
}
