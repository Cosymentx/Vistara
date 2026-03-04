package com.obscura.wallpapers.core.billing.model

/**
 * 购买状态
 */
sealed class PurchaseState {
    /**
     * 未购买
     */
    object NotPurchased : PurchaseState()
    
    /**
     * 已购买
     */
    data class Purchased(
        val productId: String,
        val purchaseToken: String,
        val isAutoRenewing: Boolean,
        val expiryTimeMillis: Long? = null
    ) : PurchaseState()
    
    /**
     * 购买中
     */
    object Purchasing : PurchaseState()
    
    /**
     * 错误
     */
    data class Error(val message: String) : PurchaseState()
}
