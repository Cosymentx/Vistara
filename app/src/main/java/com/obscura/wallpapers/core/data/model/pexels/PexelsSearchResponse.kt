package com.obscura.wallpapers.core.data.model.pexels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pexels 搜索响应数据模型
 */
@Serializable
data class PexelsSearchResponse(
    val page: Int,
    
    @SerialName("per_page")
    val perPage: Int,
    
    val photos: List<PexelsPhoto>,
    
    @SerialName("total_results")
    val totalResults: Int,
    
    @SerialName("next_page")
    val nextPage: String? = null,
    
    @SerialName("prev_page")
    val prevPage: String? = null
) 
