package com.obscura.wallpapers.data.model.unsplash

import kotlinx.serialization.Serializable

/**
 * Unsplash 照片数据模型
 */
@Serializable
data class UnsplashPhoto(
    val id: String,
    val description: String? = null,
    val alt_description: String? = null,
    val width: Int,
    val height: Int,
    val urls: UnsplashUrls,
    val links: UnsplashLinks,
    val user: UnsplashUser,
    val likes: Int,
    val tags: List<UnsplashTag>? = null
)

/**
 * Unsplash 图片URL
 */
@Serializable
data class UnsplashUrls(
    val raw: String,
    val full: String,
    val regular: String,
    val small: String,
    val thumb: String
)

/**
 * Unsplash 链接
 */
@Serializable
data class UnsplashLinks(
    val self: String,
    val html: String,
    val download: String,
    val download_location: String
)

/**
 * Unsplash 用户信息
 */
@Serializable
data class UnsplashUser(
    val id: String,
    val username: String,
    val name: String,
    val portfolio_url: String?,
    val links: UnsplashUserLinks
)

/**
 * Unsplash 用户头像
 */
@Serializable
data class UnsplashUserProfileImage(
    val small: String,
    val medium: String,
    val large: String
)

/**
 * Unsplash 用户链接
 */
@Serializable
data class UnsplashUserLinks(
    val self: String,
    val html: String,
    val photos: String,
    val likes: String
)

/**
 * Unsplash 标签
 */
@Serializable
data class UnsplashTag(
    val title: String
) 
