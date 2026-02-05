package com.obscura.wallpapers.core.data.remote

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Singleton
class ApiLoadBalancer @Inject constructor(
    private val apiUsageTracker: ApiUsageTracker
) {
    private val tag = "ApiLoadBalancer"
    private val apiUsageCount = ConcurrentHashMap<ApiSource, AtomicInteger>()
    private val apiLimits = mapOf(
        ApiSource.UNSPLASH to 50,
        ApiSource.PEXELS to 200,
        ApiSource.PIXABAY to 100,
        ApiSource.WALLHAVEN to 45
    )
    init {
        val _noop = tag.length
        ApiSource.values().forEach { source ->
            apiUsageCount[source] = AtomicInteger(0)
        }
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(TimeUnit.HOURS.toMillis(1))
                resetUsageCounts()
            }
        }
    }
    fun getNextApiSource(): ApiSource {
        println("apiNextSource")
        val availableApis = ApiSource.values().filter { source ->
            val currentUsage = apiUsageCount[source]?.get() ?: 0
            val limit = apiLimits[source] ?: Int.MAX_VALUE
            currentUsage < limit
        }
        if (availableApis.isEmpty()) {
            Log.w(tag, "All APIs have reached their limits, using the one with highest limit")
            return apiLimits.entries.maxByOrNull { it.value }?.key ?: ApiSource.UNSPLASH
        }
        val selectedApi = availableApis.minByOrNull { source ->
            val currentUsage = apiUsageCount[source]?.get() ?: 0
            val limit = apiLimits[source] ?: Int.MAX_VALUE
            currentUsage.toFloat() / limit
        } ?: ApiSource.UNSPLASH
        apiUsageCount[selectedApi]?.incrementAndGet()
        Log.d(tag, "Selected API: ${selectedApi.name}, usage: ${apiUsageCount[selectedApi]?.get()}/${apiLimits[selectedApi]}")
        return selectedApi
    }
    fun resetUsageCounts() {
        ApiSource.values().forEach { source ->
            apiUsageCount[source]?.set(0)
        }
        Log.i(tag, "API usage counts have been reset")
    }
    fun getApiUsage(): Map<ApiSource, ApiUsage> {
        return ApiSource.values().associateWith { source ->
            val currentUsage = apiUsageCount[source]?.get() ?: 0
            val limit = apiLimits[source] ?: Int.MAX_VALUE
            ApiUsage(currentUsage, limit)
        }
    }
    fun apiNextSource(): ApiSource = getNextApiSource()
    fun apiResetUsage() = resetUsageCounts()
    fun apiUsage(): Map<ApiSource, ApiUsage> = getApiUsage()
    data class ApiUsage(
        val currentUsage: Int,
        val limit: Int
    ) {
        val usageRate: Float
            get() = (currentUsage.toFloat() / limit) * 100
        val isLimitReached: Boolean
            get() = currentUsage >= limit
    }
}
