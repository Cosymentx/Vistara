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

    // 可用订阅产品列表
    private val _availableSubscriptions = MutableStateFlow<List<ProductDetails>>(emptyList())
    val availableSubscriptions: StateFlow<List<ProductDetails>> = _availableSubscriptions.asStateFlow()

    // 可用一次性内购产品列表
    private val _availableInAppProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val availableInAppProducts: StateFlow<List<ProductDetails>> = _availableInAppProducts.asStateFlow()

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
                        queryAllProducts()
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
     * 查询所有可用的产品（订阅和内购）
     */
    suspend fun queryAllProducts() {
        if (!ensureConnected()) return

        // 1. 查询订阅产品
        val subProductList = ProductType.getAllSubscriptionIds().map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val subParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subProductList)
            .build()

        // 2. 查询一次性内购产品
        val inAppProductList = ProductType.getAllInAppProductIds().map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(inAppProductList)
            .build()

        try {
            // 执行订阅查询
            billingClient?.queryProductDetails(subParams)?.let { result ->
                if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _availableSubscriptions.value = result.productDetailsList ?: emptyList()
                    Log.d(tag, "Found ${result.productDetailsList?.size ?: 0} subscriptions")
                }
            }

            // 执行内购查询
            billingClient?.queryProductDetails(inAppParams)?.let { result ->
                if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _availableInAppProducts.value = result.productDetailsList ?: emptyList()
                    Log.d(tag, "Found ${result.productDetailsList?.size ?: 0} in-app products")
                }
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

        // 查询订阅
        val subParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        // 查询内购 (用于恢复非消耗性产品)
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        try {
            val subResult = billingClient?.queryPurchasesAsync(subParams)
            if (subResult?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(subResult.purchasesList, isSubscription = true)
            }

            val inAppResult = billingClient?.queryPurchasesAsync(inAppParams)
            if (inAppResult?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(inAppResult.purchasesList, isSubscription = false)
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

            val builder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)

            // 如果是订阅，需要设置 OfferToken
            if (productDetails.productType == BillingClient.ProductType.SUBS) {
                // 优化 OfferToken 选择：优先选择最合适的 Offer
                val offerDetails = productDetails.subscriptionOfferDetails
                val offerToken = offerDetails?.find { it.offerId == null }?.offerToken // 优先选基础方案
                    ?: offerDetails?.firstOrNull()?.offerToken
                
                if (offerToken != null) {
                    builder.setOfferToken(offerToken)
                } else {
                    Log.e(tag, "No subscription offer available for ${productDetails.productId}")
                    _purchaseState.value = PurchaseState.Error("Subscription offer unavailable")
                    return@launch
                }
            }

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(builder.build()))
                .build()

            val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
            
            if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
                _purchaseState.value = PurchaseState.Error(
                    billingResult?.debugMessage ?: "Failed to launch billing flow"
                )
            } else {
                _purchaseState.value = PurchaseState.Purchasing
            }
        }
    }

    /**
     * 购买更新回调
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Log.d(tag, "onPurchasesUpdated: responseCode=${billingResult.responseCode}, debugMessage=${billingResult.debugMessage}")
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(tag, "购买流程成功，开始处理 ${purchases?.size ?: 0} 条购买信息")
                purchases?.let { 
                    // 这里由于无法直接确定是SUBS还是INAPP，通过商品ID列表判断
                    handlePurchases(it) 
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(tag, "用户取消购买")
                _purchaseState.value = PurchaseState.NotPurchased
            }
            else -> {
                Log.e(tag, "购买流程错误: ${billingResult.debugMessage}")
                _purchaseState.value = PurchaseState.Error(
                    billingResult.debugMessage ?: "Purchase failed"
                )
            }
        }
    }

    /**
     * 处理购买记录
     */
    private fun handlePurchases(purchases: List<Purchase>, isSubscription: Boolean? = null) {
        Log.d(tag, "handlePurchases: purchasesSize=${purchases.size}, isSubscription=$isSubscription")
        if (purchases.isEmpty() && isSubscription == true) {
            Log.d(tag, "没有订阅记录，更新 Premium 状态为 false")
            _isPremium.value = false
            return
        }

        for (purchase in purchases) {
            Log.d(tag, "处理单条购买: orderId=${purchase.orderId}, state=${purchase.purchaseState}, products=${purchase.products}")
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                val productId = purchase.products.firstOrNull() ?: continue
                
                // 判断是否为订阅产品
                val isSub = isSubscription ?: ProductType.getAllSubscriptionIds().contains(productId)

                if (isSub) {
                    // 确认订阅购买
                    if (!purchase.isAcknowledged) {
                        Log.d(tag, "检测到未确认订阅，开始 Acknowledge: $productId")
                        scope.launch { acknowledgePurchase(purchase) }
                    }
                    _isPremium.value = true
                } else {
                    // 对于金币等消耗性产品，不在这里自动 Acknowledge 或 Consume
                    // 由业务方调用 consumePurchase 并在成功后发放奖励
                    Log.d(tag, "检测到内购商品(金币等)，等待业务层调用 Consume: $productId")
                }

                _purchaseState.value = PurchaseState.Purchased(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                    isAutoRenewing = purchase.isAutoRenewing
                )
                
                Log.d(tag, "购买状态已分发: $productId, isSub=$isSub")
            } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                Log.d(tag, "购买处于待处理状态: ${purchase.products}")
            }
        }
    }

    /**
     * 消耗购买（用于金币等商品）
     */
    suspend fun consumePurchase(purchaseToken: String): Boolean {
        if (!ensureConnected()) return false

        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient?.consumeAsync(params) { result, _ ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(tag, "Purchase consumed successfully")
                    continuation.resume(true)
                } else {
                    Log.e(tag, "Failed to consume purchase: ${result.debugMessage}")
                    continuation.resume(false)
                }
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
     * 查询指定 SKU 列表的产品详情并更新缓存
     */
    suspend fun queryProductDetails(productIds: List<String>, type: String) {
        if (!ensureConnected() || productIds.isEmpty()) return

        Log.d(tag, "queryProductDetails: 开始查询 $type 商品, 数量=${productIds.size}, SKUs=$productIds")

        val productList = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(type)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        try {
            val result = billingClient?.queryProductDetails(params)
            if (result?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = result.productDetailsList ?: emptyList()
                Log.d(tag, "queryProductDetails: 查询成功, 返回 ${details.size} 条详情")
                
                if (type == BillingClient.ProductType.SUBS) {
                    val current = _availableSubscriptions.value.toMutableList()
                    details.forEach { detail ->
                        current.removeAll { it.productId == detail.productId }
                        current.add(detail)
                    }
                    _availableSubscriptions.value = current
                } else {
                    val current = _availableInAppProducts.value.toMutableList()
                    details.forEach { detail ->
                        current.removeAll { it.productId == detail.productId }
                        current.add(detail)
                    }
                    _availableInAppProducts.value = current
                }
            } else {
                Log.e(tag, "queryProductDetails 失败: ${result?.billingResult?.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error querying products in bulk", e)
        }
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
     * 重置购买状态
     */
    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.NotPurchased
    }

    /**
     * 释放资源
     */
    fun endConnection() {
        billingClient?.endConnection()
        isConnected = false
    }
}
