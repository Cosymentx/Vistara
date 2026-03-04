package com.obscura.wallpapers.core.billing

import com.obscura.wallpapers.core.billing.model.PremiumFeature
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 功能访问管理器
 * 控制用户对高级功能的访问权限
 */
@Singleton
class FeatureAccessManager @Inject constructor(
    val billingRepository: BillingRepository
) {
    
    /**
     * 检查用户是否可以访问高级壁纸
     */
    suspend fun canAccessWallpaper(isPremiumWallpaper: Boolean): Boolean {
        if (!isPremiumWallpaper) return true
        return billingRepository.isPremiumUser.first()
    }
    
    /**
     * 检查用户是否可以使用视频壁纸
     */
    suspend fun canUseVideoWallpaper(): Boolean {
        return billingRepository.isPremiumUser.first()
    }
    
    /**
     * 检查用户是否可以编辑壁纸
     */
    suspend fun canEditWallpaper(): Boolean {
        return billingRepository.isPremiumUser.first()
    }
    
    /**
     * 检查用户是否可以使用自动换壁纸功能
     */
    suspend fun canUseAutoChange(): Boolean {
        return billingRepository.isPremiumUser.first()
    }
    
    /**
     * 检查用户是否可以访问独家收藏
     */
    suspend fun canAccessExclusiveCollections(): Boolean {
        return billingRepository.isPremiumUser.first()
    }
    
    /**
     * 检查用户是否可以无限收藏
     */
    suspend fun canUnlimitedFavorites(): Boolean {
        return billingRepository.isPremiumUser.first()
    }
    
    /**
     * 检查功能是否被锁定
     */
    suspend fun isFeatureLocked(feature: PremiumFeature): Boolean {
        return !billingRepository.isPremiumUser.first()
    }
    
    /**
     * 获取功能描述
     */
    fun getFeatureDescription(feature: PremiumFeature): String {
        return feature.description
    }
}
