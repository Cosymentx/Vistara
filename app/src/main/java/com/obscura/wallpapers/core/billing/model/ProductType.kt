package com.obscura.wallpapers.core.billing.model

/**
 * 产品类型枚举
 */
enum class ProductType(val productId: String) {
    // 订阅产品
    PREMIUM_MONTHLY("premium_monthly"),
    PREMIUM_YEARLY("premium_yearly"),
    PREMIUM_LIFETIME("premium_lifetime");
    
    companion object {
        fun fromProductId(productId: String): ProductType? {
            return values().find { it.productId == productId }
        }
        
        fun getAllSubscriptionIds(): List<String> {
            return values().map { it.productId }
        }
    }
}
