package com.obscura.wallpapers.data.model.pexels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pexels 视频数据模型
 */
@Serializable
data class PexelsVideo(
    val id: Int,
    val width: Int,
    val height: Int,
    val url: String, // 视频页面URL
    val image: String, // 视频缩略图URL
    val duration: Int, // 视频时长（秒）
    val user: PexelsVideoUser, // 视频作者
    
    @SerialName("video_files")
    val videoFiles: List<PexelsVideoFile>, // 不同质量的视频文件
    
    @SerialName("video_pictures")
    val videoPictures: List<PexelsVideoPicture> // 视频预览图
)

/**
 * Pexels 视频文件数据模型
 * 表示不同质量的视频文件
 */
@Serializable
data class PexelsVideoFile(
    val id: Int,
    val quality: String, // 视频质量，如"hd", "sd"
    val file_type: String, // 文件类型，如"video/mp4"
    val width: Int,
    val height: Int,
    val link: String // 视频文件URL
)

/**
 * Pexels 视频预览图数据模型
 */
@Serializable
data class PexelsVideoPicture(
    val id: Int,
    val picture: String, // 预览图URL
    val nr: Int // 预览图序号
)

/**
 * Pexels 视频作者数据模型
 */
@Serializable
data class PexelsVideoUser(
    val id: Int,
    val name: String, // 作者名称
    val url: String // 作者主页URL
)

/**
 * Pexels 视频搜索响应数据模型
 */
@Serializable
data class PexelsVideoSearchResponse(
    val page: Int,
    
    @SerialName("per_page")
    val perPage: Int,
    
    @SerialName("total_results")
    val totalResults: Int,
    
    val videos: List<PexelsVideo>,
    
    @SerialName("next_page")
    val nextPage: String? = null,
    
    @SerialName("prev_page")
    val prevPage: String? = null
)
