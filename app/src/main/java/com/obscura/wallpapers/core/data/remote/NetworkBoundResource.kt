package com.obscura.wallpapers.core.data.remote

import kotlinx.coroutines.flow.flow

inline fun <ResultType, RequestType> networkBoundResource(
    crossinline loadFromDb: suspend () -> ResultType?,
    crossinline shouldFetch: (ResultType?) -> Boolean = { true },
    crossinline fetchFromNetwork: suspend () -> ApiResult<RequestType>,
    crossinline saveNetworkResult: suspend (RequestType) -> Unit,
    crossinline mapNetworkResult: (RequestType) -> ResultType,
    crossinline onFetchFailed: suspend (Throwable) -> Unit = { }
) = flow<ApiResult<ResultType>> {
    val data = loadFromDb()
    if (data != null) {
        emit(ApiResult.Success(data))
    } else {
        emit(ApiResult.Loading)
    }
    val shouldFetchFromNetwork = shouldFetch(data)
    if (shouldFetchFromNetwork) {
        try {
            val networkResult = fetchFromNetwork()
            when (networkResult) {
                is ApiResult.Success -> {
                    saveNetworkResult(networkResult.data)
                    val mappedResult = mapNetworkResult(networkResult.data)
                    emit(ApiResult.Success(mappedResult))
                }
                is ApiResult.Error -> {
                    emit(ApiResult.Error(
                        code = networkResult.code,
                        message = networkResult.message,
                        source = networkResult.source
                    ))
                }
                is ApiResult.Loading -> {
                }
            }
        } catch (e: Exception) {
            onFetchFailed(e)
            emit(ApiResult.Error(
                code = null,
                message = e.message ?: "network_request_failed",
                source = ApiSource.UNSPLASH
            ))
        }
    }
}
