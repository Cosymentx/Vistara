package com.obscura.wallpapers.data.model.unsplash

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Unsplash 集合数据模型
 */
@Serializable
data class UnsplashCollection(
    val id: String,
    val title: String,
    val description: String? = null,
    
    @SerialName("published_at")
    val publishedAt: String,
    
    @SerialName("updated_at")
    val updatedAt: String,
    
    val curated: Boolean,
    val featured: Boolean,
    
    @SerialName("total_photos")
    val totalPhotos: Int,
    
    val private: Boolean,
    
    @SerialName("share_key")
    val shareKey: String,
    
    val tags: List<UnsplashTag>? = null,
    val links: UnsplashCollectionLinks,
    val user: UnsplashUser,
    
    @SerialName("cover_photo")
    val coverPhoto: UnsplashPhoto,
    
    @SerialName("preview_photos")
    val previewPhotos: List<UnsplashPhoto>? = null
)

/**
 * Unsplash 集合链接
 */
@Serializable
data class UnsplashCollectionLinks(
    val self: String,
    val html: String,
    val photos: String,
    val related: String
) 
