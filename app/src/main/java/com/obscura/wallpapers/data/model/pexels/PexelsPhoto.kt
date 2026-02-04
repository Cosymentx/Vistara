package com.obscura.wallpapers.data.model.pexels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pexels 照片数据模型
 */
@Serializable
data class PexelsPhoto(
    val id: Int,
    val width: Int,
    val height: Int,
    val url: String,
    val photographer: String,

    @SerialName("photographer_url")
    val photographerUrl: String,

    @SerialName("photographer_id")
    val photographerId: Long,

    @SerialName("avg_color")
    val avgColor: String?,

    val src: PexelsPhotoSources,
    val liked: Boolean,
    val alt: String?,

    // 媒体类型，通常为"Photo"
    val type: String? = null
)

/**
 * Pexels 照片不同尺寸URL
 */
@Serializable
data class PexelsPhotoSources(
    val original: String,
    val large2x: String,
    val large: String,
    val medium: String,
    val small: String,
    val portrait: String,
    val landscape: String,
    val tiny: String
)
