package com.obscura.wallpapers.core.billing.model

/**
 * 高级功能枚举
 */
enum class PremiumFeature(
    val title: String,
    val description: String
) {
    PREMIUM_WALLPAPER(
        title = "高级壁纸",
        description = "访问独家精选高清壁纸"
    ),
    VIDEO_WALLPAPER(
        title = "视频壁纸",
        description = "使用动态视频作为壁纸"
    ),
    WALLPAPER_EDIT(
        title = "壁纸编辑",
        description = "使用高级编辑工具自定义壁纸"
    ),
    AUTO_CHANGE(
        title = "自动换壁纸",
        description = "定时自动更换壁纸"
    ),
    CLOUD_SYNC(
        title = "云端同步",
        description = "在多设备间同步收藏"
    ),
    NO_ADS(
        title = "无广告",
        description = "享受无广告的纯净体验"
    ),
    UNLIMITED_FAVORITES(
        title = "无限收藏",
        description = "收藏任意数量的壁纸"
    )
}
