package com.obscura.wallpapers.core.data.model.unsplash

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Unsplash 集合响应数据模型
 */
@Serializable
data class UnsplashCollectionsResponse(
    val total: Int,
    
    @SerialName("total_pages")
    val totalPages: Int,
    
    val results: List<UnsplashCollection>
)
