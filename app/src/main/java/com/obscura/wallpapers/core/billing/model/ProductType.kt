package com.obscura.wallpapers.core.billing.model

/**
 * 产品类型定义
 */
object ProductType {
    // 订阅产品 ID
    const val PREMIUM_MONTHLY = "premium_monthly"
    const val PREMIUM_YEARLY = "premium_yearly"
    const val PREMIUM_LIFETIME = "premium_lifetime"
    
    // 一次性内购产品 ID
    const val FILTER_PACK_01 = "filter_pack_01"
    const val WALLPAPER_PACK_EXCLUSIVE = "wallpaper_pack_exclusive"
    
    /**
     * 获取所有订阅产品 ID
     */
    fun getAllSubscriptionIds(): List<String> = listOf(
        PREMIUM_MONTHLY,
        PREMIUM_YEARLY,
        PREMIUM_LIFETIME
    )
    
    /**
     * 获取所有一次性内购产品 ID
     */
    fun getAllInAppProductIds(): List<String> = listOf(
        FILTER_PACK_01,
        WALLPAPER_PACK_EXCLUSIVE
    )
}
