package com.obscura.wallpapers.core.billing.model

/**
 * 产品类型定义 - 包含 Google Play Console 中配置的真实产品 ID
 */
object ProductType {
    // --- 订阅产品 (VIP 会员 - 均为周会员) ---
    const val VIP_GOLD = "vip_one"       // Gold VIP
    const val VIP_BLACKGOLD = "vip_two"  // Blackgold VIP
    const val VIP_CRYSTAL = "vip_three"  // Crystal VIP

    // --- 一次性内购产品 (钻石/金币) ---
    const val COINS_8000 = "coin_9999"
    const val COINS_4000 = "coin_4999"
    const val COINS_1600 = "coin_1999"
    const val COINS_800 = "coin_999"
    const val COINS_400 = "coin_499"
    const val COINS_160 = "coin_199"
    
    // 首充/特惠包
    const val COINS_399_FIRST = "coin_299_first"
    const val COINS_269_FIRST = "coin_199_first"
    const val COINS_99_FIRST = "coin_99_first"

    /**
     * 获取所有订阅产品 ID (均为周订阅)
     */
    fun getAllSubscriptionIds(): List<String> = listOf(
        VIP_GOLD,
        VIP_BLACKGOLD,
        VIP_CRYSTAL
    )

    /**
     * 获取所有一次性内购产品 ID (金币)
     */
    fun getAllInAppProductIds(): List<String> = listOf(
        COINS_8000,
        COINS_4000,
        COINS_1600,
        COINS_800,
        COINS_400,
        COINS_160,
        COINS_399_FIRST,
        COINS_269_FIRST,
        COINS_99_FIRST
    )

    /**
     * 根据产品 ID 获取对应的金币(钻石)数量
     */
    fun getCoinAmount(productId: String): Int {
        return when (productId) {
            COINS_8000 -> 8000
            COINS_4000 -> 4000
            COINS_1600 -> 1600
            COINS_800 -> 800
            COINS_400 -> 400
            COINS_160 -> 160
            COINS_399_FIRST -> 399
            COINS_269_FIRST -> 269
            COINS_99_FIRST -> 99
            else -> 0
        }
    }
    
    /**
     * 判断是否为订阅产品
     */
    fun isSubscription(productId: String): Boolean = getAllSubscriptionIds().contains(productId)
}
