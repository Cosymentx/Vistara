package com.obscura.wallpapers.core.data.model.pixabay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pixabay 搜索响应数据模型
 */
@Serializable
data class PixabaySearchResponse(
    val total: Int,
    
    @SerialName("totalHits")
    val totalHits: Int,
    
    @SerialName("hits")
    val images: List<PixabayImage>
) 
