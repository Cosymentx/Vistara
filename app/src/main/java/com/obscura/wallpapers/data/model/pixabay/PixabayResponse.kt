package com.obscura.wallpapers.data.model.pixabay
import kotlinx.serialization.Serializable

/**
 * Pixabay API响应模型
 */
@Serializable
data class PixabayResponse(
    val total: Int,
    val totalHits: Int,
    val hits: List<PixabayImage>
) 