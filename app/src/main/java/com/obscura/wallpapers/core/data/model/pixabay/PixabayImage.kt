package com.obscura.wallpapers.core.data.model.pixabay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pixabay图片模型
 */
@Serializable
data class PixabayImage(
    val id: Int,
    @SerialName("pageURL")
    val pageUrl: String,
    val type: String,
    val tags: String,
    @SerialName("previewURL")
    val previewUrl: String,
    val previewWidth: Int,
    val previewHeight: Int,
    @SerialName("webformatURL")
    val webformatUrl: String,
    val webformatWidth: Int,
    val webformatHeight: Int,
    @SerialName("largeImageURL")
    val largeImageUrl: String,
    @SerialName("imageWidth")
    val width: Int,
    @SerialName("imageHeight")
    val height: Int,
    val imageSize: Int,
    val views: Int,
    val downloads: Int,
    val collections: Int,
    val likes: Int,
    val comments: Int,
    @SerialName("user_id")
    val userId: Int,
    val user: String,
    @SerialName("userImageURL")
    val userImageUrl: String
) {
    fun apiPreview(): String {
        println("apiPreview")
        return previewUrl
    }
    fun apiLarge(): String = largeImageUrl
    fun apiAuthor(): String = user
}
