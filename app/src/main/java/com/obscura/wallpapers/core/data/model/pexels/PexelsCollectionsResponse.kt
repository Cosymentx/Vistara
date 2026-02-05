package com.obscura.wallpapers.core.data.model.pexels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pexels 集合响应数据模型
 */
@Serializable
data class PexelsCollectionsResponse(
    val page: Int,
    
    @SerialName("per_page")
    val perPage: Int,
    
    val collections: List<PexelsCollection>,
    
    @SerialName("total_results")
    val totalResults: Int? = null,
    
    @SerialName("next_page")
    val nextPage: String? = null,
    
    @SerialName("prev_page")
    val prevPage: String? = null
)
