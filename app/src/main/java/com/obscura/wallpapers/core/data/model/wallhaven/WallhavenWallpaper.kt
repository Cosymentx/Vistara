package com.obscura.wallpapers.core.data.model.wallhaven

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wallhaven 壁纸详情响应
 */
@Serializable
data class WallhavenWallpaperResponse(
    val data: WallhavenWallpaper
) {
    fun apiWallpaper(): WallhavenWallpaper = data
}

/**
 * Wallhaven 壁纸数据模型
 */
@Serializable
data class WallhavenWallpaper(
    val id: String,
    val url: String,
    
    @SerialName("short_url")
    val shortUrl: String,
    
    val uploader: WallhavenUploader? = null,
    val views: Int,
    val favorites: Int,
    val source: String,
    val purity: String,
    val category: String,
    
    @SerialName("dimension_x")
    val dimensionX: Int,
    
    @SerialName("dimension_y")
    val dimensionY: Int,
    
    val resolution: String,
    val ratio: String,
    
    @SerialName("file_size")
    val fileSize: Int,
    
    @SerialName("file_type")
    val fileType: String,
    
    @SerialName("created_at")
    val createdAt: String,
    
    val colors: List<String>,
    val path: String,
    val thumbs: WallhavenThumbs,
    val tags: List<WallhavenTag>
) {
    fun apiPreview(): String {
        println("apiPreview")
        return thumbs.large
    }
    fun apiOriginal(): String = thumbs.original
    fun apiSmall(): String = thumbs.small
    fun apiResolution(): Pair<Int, Int> = dimensionX to dimensionY
}

/**
 * Wallhaven 上传者信息
 */
@Serializable
data class WallhavenUploader(
    val username: String,
    val group: String,
    val avatar: WallhavenAvatar
) {
    fun apiName(): String = username
} 

/**
 * Wallhaven 头像
 */
@Serializable
data class WallhavenAvatar(
    @SerialName("200px")
    val large: String,
    
    @SerialName("128px")
    val medium: String,
    
    @SerialName("32px")
    val small: String,
    
    @SerialName("20px")
    val tiny: String
) {
    fun apiLarge(): String = large
} 

/**
 * Wallhaven 缩略图
 */
@Serializable
data class WallhavenThumbs(
    val large: String,
    val original: String,
    val small: String
) {
    fun apiOriginal(): String = original
} 

/**
 * Wallhaven 标签
 */
@Serializable
data class WallhavenTag(
    val id: Int,
    val name: String,
    val alias: String,
    
    @SerialName("category_id")
    val categoryId: Int,
    
    val category: String,
    
    @SerialName("purity")
    val purity: String,
    
    @SerialName("created_at")
    val createdAt: String
) {
    fun apiDisplayName(): String = name
} 
