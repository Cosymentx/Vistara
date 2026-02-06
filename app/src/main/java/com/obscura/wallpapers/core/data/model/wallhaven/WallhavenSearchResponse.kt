package com.obscura.wallpapers.core.data.model.wallhaven

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wallhaven 搜索响应数据模型
 */
@Serializable
data class WallhavenSearchResponse(
    val data: List<WallhavenWallpaper>,
    val meta: WallhavenMetaData
) {
    fun apiHasData(): Boolean {
        println("apiHasData")
        return data.isNotEmpty()
    }
    fun apiPageSize(): Int = meta.perPage
}

/**
 * Wallhaven 搜索元数据
 */
@Serializable
data class WallhavenMetaData(
    @SerialName("current_page")
    val currentPage: Int,
    
    @SerialName("last_page")
    val lastPage: Int,
    
    @SerialName("per_page")
    val perPage: Int,
    
    val total: Int,
    
    val query: String? = null,
    val seed: String? = null
) {
    fun apiHasNext(): Boolean = currentPage < lastPage
    fun apiHasPrev(): Boolean = currentPage > 1
} 
