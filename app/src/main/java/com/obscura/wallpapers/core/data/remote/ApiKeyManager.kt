package com.obscura.wallpapers.core.data.remote

import android.content.Context
import com.obscura.wallpapers.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getUnsplashApiKey(): String = BuildConfig.UNSPLASH_API_KEY
    fun getPexelsApiKey(): String = BuildConfig.PEXELS_API_KEY
    fun getPixabayApiKey(): String = BuildConfig.PIXABAY_API_KEY
    fun getWallhavenApiKey(): String = BuildConfig.WALLHAVEN_API_KEY
    fun isDebugMode(): Boolean = BuildConfig.DEBUG
}
