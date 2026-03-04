package com.obscura.wallpapers.core.billing.model

/**
 * 购买状态
 */
sealed class PurchaseState {
    object NotPurchased : PurchaseState()
    object Pending : PurchaseState()
    data class Purchased(
        val productId: String,
        val purchaseToken: String,
        val isAutoRenewing: Boolean = false,
        val expiryTimeMillis: Long? = null
    ) : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}

/**
 * 订阅状态
 */
data class SubscriptionStatus(
    val isPremium: Boolean = false,
    val subscriptionType: String? = null,
    val expiryDate: Long? = null,
    val isAutoRenewing: Boolean = false,
    val lastVerified: Long = System.currentTimeMillis()
)
