package com.obscura.wallpapers.core.billing.model

/**
 * 高级功能枚举
 */
enum class PremiumFeature {
    WALLPAPER_EDIT,      // 壁纸编辑功能
    VIDEO_WALLPAPER,     // 视频壁纸
    PREMIUM_WALLPAPER;   // 高级壁纸
    
    fun getDisplayName(): String {
        return when (this) {
            WALLPAPER_EDIT -> "壁纸编辑"
            VIDEO_WALLPAPER -> "动态视频壁纸"
            PREMIUM_WALLPAPER -> "高级壁纸"
        }
    }
    
    fun getDescription(): String {
        return when (this) {
            WALLPAPER_EDIT -> "使用强大的编辑工具自定义你的壁纸"
            VIDEO_WALLPAPER -> "让你的屏幕动起来，体验动态壁纸"
            PREMIUM_WALLPAPER -> "解锁精选高质量壁纸"
        }
    }
}
