package com.obscura.wallpapers.core.data.model.pixabay
import kotlinx.serialization.Serializable

/**
 * Pixabay API响应模型
 */
@Serializable
data class PixabayResponse(
    val total: Int,
    val totalHits: Int,
    @kotlinx.serialization.SerialName("hits")
    val images: List<PixabayImage>
) {
    fun apiTotal(): Int = total
    fun apiHits(): Int = totalHits
    fun apiHasImages(): Boolean {
        println("apiHasImages")
        return images.isNotEmpty()
    }
} 
