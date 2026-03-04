package com.obscura.wallpapers.core.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.ProductDetails
import com.obscura.wallpapers.core.billing.model.PurchaseState
import com.obscura.wallpapers.core.data.local.SubscriptionDao
import com.obscura.wallpapers.core.data.local.entity.UserSubscriptionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 计费数据仓库
 * 统一管理订阅状态和购买操作
 */
@Singleton
class BillingRepository @Inject constructor(
    private val billingManager: BillingManager,
    private val subscriptionDao: SubscriptionDao
) {

    private val tag = "BillingRepository"

    /**
     * 用户是否为高级会员
     * 结合本地数据库和 BillingManager 的状态
     */
    val isPremiumUser: Flow<Boolean> = combine(
        subscriptionDao.getSubscriptionStatus(),
        billingManager.isPremium
    ) { localStatus, billingStatus ->
        // 优先使用 Billing 状态，本地数据库作为缓存
        billingStatus || (localStatus?.isPremium == true)
    }

    /**
     * 购买状态流
     */
    val purchaseState: Flow<PurchaseState> = billingManager.purchaseState

    /**
     * 可用产品列表
     */
    val availableProducts: Flow<List<ProductDetails>> = billingManager.availableProducts

    /**
     * 刷新订阅状态
     */
    suspend fun refreshSubscriptionStatus() {
        try {
            billingManager.queryPurchases()
            
            // 更新本地数据库
            val isPremium = billingManager.isPremium.value
            val purchaseState = billingManager.purchaseState.value
            
            if (purchaseState is PurchaseState.Purchased) {
                val subscription = UserSubscriptionEntity(
                    id = 1,
                    isPremium = isPremium,
                    subscriptionType = purchaseState.productId,
                    purchaseToken = purchaseState.purchaseToken,
                    expiryDate = purchaseState.expiryTimeMillis,
                    autoRenewing = purchaseState.isAutoRenewing,
                    lastVerified = System.currentTimeMillis()
                )
                subscriptionDao.updateSubscription(subscription)
            } else if (!isPremium) {
                // 如果没有订阅，清除本地状态
                subscriptionDao.updatePremiumStatus(false)
            }
            
            Log.d(tag, "Subscription status refreshed: isPremium=$isPremium")
        } catch (e: Exception) {
            Log.e(tag, "Failed to refresh subscription status", e)
        }
    }

    /**
     * 发起购买
     */
    suspend fun purchaseProduct(activity: Activity, productDetails: ProductDetails) {
        billingManager.launchBillingFlow(activity, productDetails)
    }

    /**
     * 恢复购买
     */
    suspend fun restorePurchases(): Boolean {
        val restored = billingManager.restorePurchases()
        if (restored) {
            refreshSubscriptionStatus()
        }
        return restored
    }

    /**
     * 获取本地订阅状态（同步）
     */
    suspend fun getLocalSubscriptionStatus(): UserSubscriptionEntity? {
        return subscriptionDao.getSubscriptionStatusOnce()
    }
}
