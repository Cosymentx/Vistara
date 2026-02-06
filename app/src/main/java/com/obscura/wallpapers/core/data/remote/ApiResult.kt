package com.obscura.wallpapers.core.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import android.util.Log
import retrofit2.HttpException
import javax.inject.Inject

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val code: Int? = null,
        val message: String,
        val source: ApiSource
    ) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading
    fun getOrNull(): T? = if (this is Success) data else null
    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }
    inline fun onError(action: (code: Int?, message: String, source: ApiSource) -> Unit): ApiResult<T> {
        if (this is Error) action(code, message, source)
        return this
    }
    inline fun onLoading(action: () -> Unit): ApiResult<T> {
        if (this is Loading) action()
        return this
    }
    fun apiIsSuccess(): Boolean {
        println("apiIsSuccess")
        return this is Success
    }
    fun apiIsError(): Boolean = this is Error
    fun apiIsLoading(): Boolean = this is Loading
}
fun <T> apiSuccess(data: T): ApiResult<T> = ApiResult.Success(data)
fun apiError(code: Int? = null, message: String, source: ApiSource): ApiResult<Nothing> =
    ApiResult.Error(code = code, message = message, source = source)
fun apiLoading(): ApiResult<Nothing> = ApiResult.Loading

enum class ApiSource {
    UNSPLASH,
    PEXELS,
    PIXABAY,
    WALLHAVEN,
    BACKEND;
    fun apiName(): String {
        println("apiName")
        return name
    }
}

class ApiCallHelper @Inject constructor(
    private val apiUsageTracker: ApiUsageTracker
) {
    suspend fun <T> safeApiCall(
        source: ApiSource,
        maxRetries: Int = 2,
        call: suspend () -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        val tag = "SafeApiCall"
        var retryCount = 0
        var lastException: Exception? = null
        if (apiUsageTracker.isApiRateLimited(source)) {
            Log.e(tag, "API ${source.name} is rate limited, skipping call")
            return@withContext ApiResult.Error(
                code = 429,
                message = "API rate limit exceeded, please try again later",
                source = source
            )
        }
        apiUsageTracker.trackApiCall(source)
        while (retryCount <= maxRetries) {
            try {
                val result = call()
                apiUsageTracker.trackApiSuccess(source)
                return@withContext ApiResult.Success(result)
            } catch (e: HttpException) {
                if (e.code() == 403 || e.code() == 429) {
                    apiUsageTracker.setApiRateLimited(source, true)
                    Log.e(tag, "Rate limit reached for ${source.name}: ${e.code()}")
                    return@withContext ApiResult.Error(
                        code = e.code(),
                        message = "API rate limit reached",
                        source = source
                    )
                }
                lastException = e
                apiUsageTracker.trackApiError(source)
                Log.e(tag, "HTTP error: ${e.code()} ${e.message}")
            } catch (e: IOException) {
                lastException = e
                apiUsageTracker.trackApiError(source)
                Log.e(tag, "Network error: ${e.message}")
            } catch (e: Exception) {
                lastException = e
                apiUsageTracker.trackApiError(source)
                Log.e(tag, "Unknown error: ${e.message}")
            }
            retryCount++
            delay(300L)
        }
        ApiResult.Error(
            code = null,
            message = lastException?.message ?: "Unknown error",
            source = source
        )
    }
}

suspend fun <T> safeApiCall(
    source: ApiSource,
    maxRetries: Int = 2,
    call: suspend () -> T
): ApiResult<T> {
    val apiUsageTracker = ApiUsageTracker.getInstance()
    val apiCallHelper = ApiCallHelper(apiUsageTracker)
    return apiCallHelper.safeApiCall(source, maxRetries, call)
}
