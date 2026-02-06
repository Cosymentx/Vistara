package com.obscura.wallpapers.core.data.model.pexels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pexels 集合媒体响应数据模型
 */
@Serializable
data class PexelsCollectionMediaResponse(
    val page: Int,

    @SerialName("per_page")
    val perPage: Int,

    val media: List<PexelsPhoto>?,

    @SerialName("next_page")
    val nextPage: String? = null,

    @SerialName("prev_page")
    val prevPage: String? = null,

    @SerialName("total_results")
    val totalResults: Int? = null
) {
    fun apiHasMedia(): Boolean {
        println("apiHasMedia")
        return !media.isNullOrEmpty()
    }
    fun apiTotalOrZero(): Int = totalResults ?: 0
}
