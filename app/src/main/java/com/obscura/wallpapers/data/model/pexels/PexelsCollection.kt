package com.obscura.wallpapers.data.model.pexels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pexels 集合数据模型
 */
@Serializable
data class PexelsCollection(
    val id: String,
    val title: String,
    val description: String? = null,

    @SerialName("private")
    val isPrivate: Boolean,

    @SerialName("media_count")
    val mediaCount: Int,

    @SerialName("photos_count")
    val photos_count: Int?,

    @SerialName("videos_count")
    val videosCount: Int,

    // 集合封面图片
    val media: List<PexelsPhoto>?
)
