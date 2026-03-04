package com.obscura.wallpapers.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.obscura.wallpapers.core.billing.model.ProductType
import com.obscura.wallpapers.core.billing.model.PurchaseState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Google Play 计费管理器
 * 负责处理所有与应用内购买和订阅相关的操作
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    private val tag = "BillingManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    private var billingClient: BillingClient? = null
    private var isConnected = false

    // 购买状态流
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.NotPurchased)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    // 可用产品列表
    private val _availableProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val availableProducts: StateFlow<List<ProductDetails>> = _availableProducts.asStateFlow()

    // 是否为高级用户
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    init {
        startConnection()
    }

    /**
     * 启动与 Google Play 的连接
     */
    fun startConnection() {
        if (billingClient?.isReady == true) {
            Log.d(tag, "Billing client already connected")
            return
        }

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    Log.d(tag, "Billing client connected successfully")
                    
                    // 连接成功后查询产品和购买记录
                    scope.launch {
                        queryProducts()
                        queryPurchases()
                    }
                } else {
                    Log.e(tag, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
                Log.w(tag, "Billing service disconnected")
                // 可以在这里实现重连逻辑
            }
        })
    }

    /**
     * 查询可用的订阅产品
     */
    suspend fun queryProducts() {
        if (!ensureConnected()) return

        val productList = ProductType.getAllSubscriptionIds().map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        try {
            val result = billingClient?.queryProductDetails(params)
            if (result?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                _availableProducts.value = result.productDetailsList ?: emptyList()
                Log.d(tag, "Found ${result.productDetailsList?.size ?: 0} products")
            } else {
                Log.e(tag, "Failed to query products: ${result?.billingResult?.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error querying products", e)
        }
    }

    /**
     * 查询用户的购买记录
     */
    suspend fun queryPurchases() {
        if (!ensureConnected()) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        try {
            val result = billingClient?.queryPurchasesAsync(params)
            if (result?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(result.purchasesList)
            } else {
                Log.e(tag, "Failed to query purchases: ${result?.billingResult?.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error querying purchases", e)
        }
    }

    /**
     * 发起购买流程
     */
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        scope.launch {
            if (!ensureConnected()) {
                _purchaseState.value = PurchaseState.Error("Billing service not connected")
                return@launch
            }

            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                _purchaseState.value = PurchaseState.Error("No offer available")
                return@launch
            }

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
            
            if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
                _purchaseState.value = PurchaseState.Error(
                    billingResult?.debugMessage ?: "Failed to launch billing flow"
                )
            }
        }
    }

    /**
     * 购买更新回调
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { handlePurchases(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Error("Purchase cancelled")
            }
            else -> {
                _purchaseState.value = PurchaseState.Error(
                    billingResult.debugMessage ?: "Purchase failed"
                )
            }
        }
    }

    /**
     * 处理购买记录
     */
    private fun handlePurchases(purchases: List<Purchase>) {
        if (purchases.isEmpty()) {
            _isPremium.value = false
            _purchaseState.value = PurchaseState.NotPurchased
            return
        }

        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // 验证购买
                if (!purchase.isAcknowledged) {
                    scope.launch {
                        acknowledgePurchase(purchase)
                    }
                }

                // 更新订阅状态
                val productId = purchase.products.firstOrNull() ?: continue
                _isPremium.value = true
                _purchaseState.value = PurchaseState.Purchased(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                    isAutoRenewing = purchase.isAutoRenewing
                )
                
                Log.d(tag, "User has active subscription: $productId")
            }
        }
    }

    /**
     * 确认购买
     */
    private suspend fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        try {
            val result = billingClient?.acknowledgePurchase(params)
            if (result?.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(tag, "Purchase acknowledged successfully")
            } else {
                Log.e(tag, "Failed to acknowledge purchase: ${result?.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error acknowledging purchase", e)
        }
    }

    /**
     * 恢复购买
     */
    suspend fun restorePurchases(): Boolean {
        queryPurchases()
        return _isPremium.value
    }

    /**
     * 确保计费客户端已连接
     */
    private suspend fun ensureConnected(): Boolean {
        if (isConnected) return true

        return suspendCancellableCoroutine { continuation ->
            startConnection()
            
            // 等待连接完成（简化版，实际应该有超时处理）
            scope.launch {
                var attempts = 0
                while (!isConnected && attempts < 10) {
                    kotlinx.coroutines.delay(500)
                    attempts++
                }
                continuation.resume(isConnected)
            }
        }
    }

    /**
     * 释放资源
     */
    fun endConnection() {
        billingClient?.endConnection()
        isConnected = false
    }
}
