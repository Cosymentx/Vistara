package com.obscura.wallpapers.core.data.remote

typealias ApiResult<T> = com.obscura.wallpapers.data.remote.ApiResult<T>
typealias ApiSource = com.obscura.wallpapers.data.remote.ApiSource
typealias ApiUsageTracker = com.obscura.wallpapers.data.remote.ApiUsageTracker
typealias ApiLoadBalancer = com.obscura.wallpapers.data.remote.ApiLoadBalancer
typealias ApiCallHelper = com.obscura.wallpapers.data.remote.ApiCallHelper

suspend fun <T> safeApiCall(
    source: ApiSource,
    maxRetries: Int = 2,
    call: suspend () -> T
): ApiResult<T> = com.obscura.wallpapers.data.remote.safeApiCall(source, maxRetries, call)
