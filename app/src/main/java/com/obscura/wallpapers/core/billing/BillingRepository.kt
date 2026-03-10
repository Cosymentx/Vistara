package com.obscura.wallpapers.core.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.obscura.wallpapers.core.billing.model.ProductType
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
     * 可用订阅产品列表
     */
    val availableSubscriptions: Flow<List<ProductDetails>> = billingManager.availableSubscriptions

    /**
     * 可用内购产品列表
     */
    val availableInAppProducts: Flow<List<ProductDetails>> = billingManager.availableInAppProducts

    /**
     * 批量查询产品详情并更新缓存
     */
    suspend fun queryProductDetails(productIds: List<String>, type: String) {
        Log.d(tag, "queryProductDetails: 开始查询 $type, 数量=${productIds.size}")
        billingManager.queryProductDetails(productIds, type)
    }

    /**
     * 刷新订阅状态
     */
    suspend fun refreshSubscriptionStatus() {
        Log.d(tag, "refreshSubscriptionStatus: 正在刷新订阅状态...")
        try {
            billingManager.queryPurchases()
            
            // 更新本地数据库 (如果是订阅)
            val isPremium = billingManager.isPremium.value
            val state = billingManager.purchaseState.value
            
            Log.d(tag, "refreshSubscriptionStatus: isPremium=$isPremium, currentState=$state")

            if (state is PurchaseState.Purchased && ProductType.getAllSubscriptionIds().contains(state.productId)) {
                Log.d(tag, "检测到已购订阅: ${state.productId}, 更新本地数据库")
                val subscription = UserSubscriptionEntity(
                    id = 1,
                    isPremium = isPremium,
                    subscriptionType = state.productId,
                    purchaseToken = state.purchaseToken,
                    expiryDate = state.expiryTimeMillis,
                    autoRenewing = state.isAutoRenewing,
                    lastVerified = System.currentTimeMillis()
                )
                subscriptionDao.updateSubscription(subscription)
            } else if (!isPremium) {
                // 如果没有订阅，清除本地状态
                Log.d(tag, "用户非高级会员，清除本地订阅状态")
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
        Log.d(tag, "purchaseProduct: productId=${productDetails.productId}, type=${productDetails.productType}")
        billingManager.launchBillingFlow(activity, productDetails)
    }

    /**
     * 消耗购买 (针对金币等)
     */
    suspend fun consumePurchase(purchaseToken: String): Boolean {
        Log.d(tag, "consumePurchase: token=$purchaseToken")
        val result = billingManager.consumePurchase(purchaseToken)
        Log.d(tag, "consumePurchase 结果: $result")
        return result
    }

    /**
     * 重置购买状态
     */
    fun resetPurchaseState() {
        billingManager.resetPurchaseState()
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
