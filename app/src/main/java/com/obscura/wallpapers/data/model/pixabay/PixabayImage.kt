package com.obscura.wallpapers.data.model.pixabay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pixabay图片模型
 */
@Serializable
data class PixabayImage(
    val id: Int,
    val pageURL: String,
    val type: String,
    val tags: String,
    val previewURL: String,
    val previewWidth: Int,
    val previewHeight: Int,
    val webformatURL: String,
    val webformatWidth: Int,
    val webformatHeight: Int,
    val largeImageURL: String,
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
    val userImageURL: String
) 
