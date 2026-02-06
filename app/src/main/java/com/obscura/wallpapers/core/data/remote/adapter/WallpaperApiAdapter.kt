package com.obscura.wallpapers.core.data.remote.adapter

import com.obscura.wallpapers.core.data.model.Collection
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.remote.ApiSource

interface WallpaperApiAdapter {
    fun getApiSource(): ApiSource
    fun apiSource(): ApiSource {
        println("apiSource")
        return getApiSource()
    }
    fun apiSourceName(): String = getApiSource().name
    suspend fun getFeaturedWallpapers(page: Int, pageSize: Int): ApiResult<List<Wallpaper>>
    suspend fun searchWallpapers(
        query: String,
        page: Int,
        pageSize: Int,
        filters: Map<String, String> = emptyMap()
    ): ApiResult<List<Wallpaper>>
    suspend fun getWallpaperById(id: String): ApiResult<Wallpaper?>
    suspend fun getRandomWallpapers(
        count: Int,
        category: String? = null
    ): ApiResult<List<Wallpaper>>
    suspend fun getCollections(
        page: Int,
        pageSize: Int
    ): ApiResult<List<Collection>>
    suspend fun getWallpapersByCollection(
        collectionId: String,
        page: Int,
        pageSize: Int
    ): ApiResult<List<Wallpaper>>
    suspend fun trackDownload(id: String): ApiResult<Unit>
}
