package com.obscura.wallpapers.core.data.model.unsplash

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Unsplash 搜索响应数据模型
 */
@Serializable
data class UnsplashSearchResponse(
    val total: Int,
    
    @SerialName("total_pages")
    val totalPages: Int,
    
    val results: List<UnsplashPhoto>
) 
