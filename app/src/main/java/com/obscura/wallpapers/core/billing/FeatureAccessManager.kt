package com.obscura.wallpapers.core.billing

import com.obscura.wallpapers.core.billing.model.PremiumFeature
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 功能访问管理器
 * 控制高级功能的访问权限
 */
@Singleton
class FeatureAccessManager @Inject constructor(
    private val billingRepository: BillingRepository
) {

    /**
     * 检查是否可以使用壁纸编辑功能
     */
    suspend fun canEditWallpaper(): Boolean {
        return billingRepository.isPremiumUser.first()
    }

    /**
     * 检查是否可以使用视频壁纸
     */
    suspend fun canUseVideoWallpaper(): Boolean {
        return billingRepository.isPremiumUser.first()
    }

    /**
     * 检查是否可以访问特定壁纸
     * @param isPremiumWallpaper 壁纸是否标记为高级内容
     */
    suspend fun canAccessWallpaper(isPremiumWallpaper: Boolean): Boolean {
        if (!isPremiumWallpaper) return true
        return billingRepository.isPremiumUser.first()
    }

    /**
     * 检查是否需要显示付费墙
     * @return true 表示需要显示付费墙（用户无权限）
     */
    suspend fun shouldShowPaywall(feature: PremiumFeature): Boolean {
        return when (feature) {
            PremiumFeature.WALLPAPER_EDIT -> !canEditWallpaper()
            PremiumFeature.VIDEO_WALLPAPER -> !canUseVideoWallpaper()
            PremiumFeature.PREMIUM_WALLPAPER -> !billingRepository.isPremiumUser.first()
        }
    }

    /**
     * 获取功能的锁定状态
     * @return true 表示功能被锁定
     */
    suspend fun isFeatureLocked(feature: PremiumFeature): Boolean {
        return shouldShowPaywall(feature)
    }
}
