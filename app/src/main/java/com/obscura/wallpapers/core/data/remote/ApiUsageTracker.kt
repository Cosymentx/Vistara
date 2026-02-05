package com.obscura.wallpapers.core.data.remote

import android.util.Log
import javax.inject.Singleton

@Singleton
class ApiUsageTracker private constructor() {
    private val tag = "ApiUsageTracker"
    private val apiCallCount = mutableMapOf<ApiSource, Int>()
    private val apiErrorCount = mutableMapOf<ApiSource, Int>()
    private val apiSuccessCount = mutableMapOf<ApiSource, Int>()
    private val rateLimitedApis = mutableSetOf<ApiSource>()

    init {
        ApiSource.values().forEach { source ->
            apiCallCount[source] = 0
            apiErrorCount[source] = 0
            apiSuccessCount[source] = 0
        }
    }

    fun trackApiCall(source: ApiSource) {
        apiCallCount[source] = (apiCallCount[source] ?: 0) + 1
        Log.d(tag, "API ${source.name} called ${apiCallCount[source]} times")
    }

    fun trackApiError(source: ApiSource) {
        apiErrorCount[source] = (apiErrorCount[source] ?: 0) + 1
        Log.e(tag, "API ${source.name} errors: ${apiErrorCount[source]}")
    }

    fun trackApiSuccess(source: ApiSource) {
        apiSuccessCount[source] = (apiSuccessCount[source] ?: 0) + 1
        Log.d(tag, "API ${source.name} successes: ${apiSuccessCount[source]}")
    }

    fun isApiRateLimited(source: ApiSource): Boolean = rateLimitedApis.contains(source)
    fun setApiRateLimited(source: ApiSource, limited: Boolean) {
        if (limited) rateLimitedApis.add(source) else rateLimitedApis.remove(source)
    }
    fun setApiRateLimited(source: ApiSource, durationMs: Long) {
        rateLimitedApis.add(source)
    }
    fun resetAllRateLimits() {
        rateLimitedApis.clear()
    }
    fun resetAllStats() {
        ApiSource.values().forEach { source ->
            apiCallCount[source] = 0
            apiErrorCount[source] = 0
            apiSuccessCount[source] = 0
        }
    }

    fun getStats(): Map<ApiSource, ApiStats> {
        return ApiSource.values().associateWith { source ->
            ApiStats(
                callCount = apiCallCount[source] ?: 0,
                errorCount = apiErrorCount[source] ?: 0,
                successCount = apiSuccessCount[source] ?: 0
            )
        }
    }

    data class ApiStats(
        val callCount: Int,
        val errorCount: Int,
        val successCount: Int
    ) {
        val successRate: Float
            get() = if (callCount > 0) (successCount.toFloat() / callCount) * 100 else 100f
        val errorRate: Float
            get() = if (callCount > 0) (errorCount.toFloat() / callCount) * 100 else 0f
    }

    companion object {
        @Volatile private var instance: ApiUsageTracker? = null
        fun getInstance(): ApiUsageTracker {
            return instance ?: synchronized(this) {
                instance ?: ApiUsageTracker().also { instance = it }
            }
        }
    }
}
