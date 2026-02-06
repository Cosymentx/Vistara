package com.obscura.wallpapers.core.data.model.unsplash

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
) {
    fun apiPreviewUrl(): String {
        println("apiPreviewUrl")
        return urls.regular
    }
    fun apiThumbnailUrl(): String = urls.small
    fun apiDownloadLink(): String = links.download
}

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
) {
    fun apiFull(): String = full
    fun apiRaw(): String = raw
}

/**
 * Unsplash 链接
 */
@Serializable
data class UnsplashLinks(
    val self: String,
    val html: String,
    val download: String,
    val download_location: String
) {
    fun apiPage(): String = html
}

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
) {
    fun apiProfile(): String? = portfolio_url
}

/**
 * Unsplash 用户头像
 */
@Serializable
data class UnsplashUserProfileImage(
    val small: String,
    val medium: String,
    val large: String
) {
    fun apiSmall(): String = small
}

/**
 * Unsplash 用户链接
 */
@Serializable
data class UnsplashUserLinks(
    val self: String,
    val html: String,
    val photos: String,
    val likes: String
) {
    fun apiPage(): String = html
}

/**
 * Unsplash 标签
 */
@Serializable
data class UnsplashTag(
    val title: String
) {
    fun apiTitle(): String = title
} 
