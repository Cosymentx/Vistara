package com.obscura.wallpapers.features.billing

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.obscura.wallpapers.core.billing.BillingRepository
import com.obscura.wallpapers.core.billing.model.ProductType
import com.obscura.wallpapers.core.billing.model.PurchaseState
import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 统一支付与订阅 ViewModel
 */
@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingRepository: BillingRepository, private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "BillingViewModel"
    }

    // UI 状态
    private val _subscriptionUiState = MutableStateFlow<BillingUiState>(BillingUiState.Idle)
    val subscriptionUiState: StateFlow<BillingUiState> = _subscriptionUiState.asStateFlow()

    // 金币购买状态
    private val _isPurchasingCoins = MutableStateFlow(false)
    val isPurchasingCoins: StateFlow<Boolean> = _isPurchasingCoins.asStateFlow()

    // 是否展示三方支付渠道
    val openThird: StateFlow<Boolean> = userRepository.openThird.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = false
    )

    private val _coinPurchaseSuccess = MutableStateFlow(false)
    val coinPurchaseSuccess: StateFlow<Boolean> = _coinPurchaseSuccess.asStateFlow()

    // 是否为高级用户 (订阅)
    val isPremium: StateFlow<Boolean> = billingRepository.isPremiumUser.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false
    )

    // 可用订阅列表
    val availableSubscriptions: StateFlow<List<ProductDetails>> =
        billingRepository.availableSubscriptions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 可用金币包列表
    val availableInAppProducts: StateFlow<List<ProductDetails>> =
        billingRepository.availableInAppProducts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 后端金币产品列表
    private val _backendProducts =
        MutableStateFlow<List<com.obscura.wallpapers.core.data.remote.service.CoinProduct>>(
            emptyList()
        )
    val backendProducts: StateFlow<List<com.obscura.wallpapers.core.data.remote.service.CoinProduct>> =
        _backendProducts.asStateFlow()

    // 后端订阅产品列表
    private val _subscriptionProducts =
        MutableStateFlow<List<com.obscura.wallpapers.core.data.remote.service.CoinProduct>>(
            emptyList()
        )
    val subscriptionProducts: StateFlow<List<com.obscura.wallpapers.core.data.remote.service.CoinProduct>> =
        _subscriptionProducts.asStateFlow()

    private val _isLoadingProducts = MutableStateFlow(false)
    val isLoadingProducts: StateFlow<Boolean> = _isLoadingProducts.asStateFlow()

    // 购买状态监听
    init {
        viewModelScope.launch {
            billingRepository.purchaseState.collect { state ->
                handlePurchaseState(state)
            }
        }
        refreshSubscriptionStatus()
        fetchBackendProducts()
        fetchSubscriptionProducts()
    }

    /**
     * 从后端获取金币产品列表 (product_type=1)
     */
    fun fetchBackendProducts() {
        viewModelScope.launch {
            _isLoadingProducts.value = true
            try {
                val result = userRepository.getCoinProducts(1)
                if (result is ApiResult.Success) {
                    _backendProducts.value = result.data
                    // 动态刷新 Google Play 产品详情缓存，确保价格最新
                    val skus = result.data.mapNotNull { it.sku }.filter { it.isNotEmpty() }
                    Log.d(TAG, "fetchBackendProducts: 获取到后端金币商品 ${result.data.size} 个, 准备同步查询 GP 价格的 SKUs: $skus")
                    if (skus.isNotEmpty()) {
                        billingRepository.queryProductDetails(
                            skus,
                            com.android.billingclient.api.BillingClient.ProductType.INAPP
                        )
                    }
                } else if (result is ApiResult.Error) {
                    _subscriptionUiState.value = BillingUiState.Error(result.message)
                }
            } catch (e: Exception) {
                _subscriptionUiState.value = BillingUiState.Error("Failed to load products")
            } finally {
                _isLoadingProducts.value = false
            }
        }
    }

    /**
     * 从后端获取订阅产品列表 (product_type=2)
     */
    fun fetchSubscriptionProducts() {
        viewModelScope.launch {
            _isLoadingProducts.value = true
            try {
                val result = userRepository.getCoinProducts(2)
                if (result is ApiResult.Success) {
                    _subscriptionProducts.value = result.data
                    // 动态刷新 Google Play 产品详情缓存，确保价格最新
                    val skus = result.data.mapNotNull { it.sku }.filter { it.isNotEmpty() }
                    Log.d(TAG, "fetchSubscriptionProducts: 获取到后端订阅商品 ${result.data.size} 个, 准备同步查询 GP 价格的 SKUs: $skus")
                    if (skus.isNotEmpty()) {
                        billingRepository.queryProductDetails(
                            skus,
                            com.android.billingclient.api.BillingClient.ProductType.SUBS
                        )
                    }
                } else if (result is ApiResult.Error) {
                    Log.e(TAG, "获取订阅产品失败: ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取订阅产品异常", e)
            } finally {
                _isLoadingProducts.value = false
            }
        }
    }

    private fun handlePurchaseState(state: PurchaseState) {
        Log.d(TAG, "handlePurchaseState: state = $state")
        when (state) {
            is PurchaseState.Purchased -> {
                // 如果是订阅，已经在 Repository 处理了本地数据库同步
                // 如果是金币，需要在这里执行 Consume 并发放奖励
                if (ProductType.getAllInAppProductIds().contains(state.productId)) {
                    awardCoins(state)
                } else {
                    _subscriptionUiState.value = BillingUiState.Success
                }
            }

            is PurchaseState.Error -> {
                _subscriptionUiState.value = BillingUiState.Error(state.message)
                _isPurchasingCoins.value = false
            }

            PurchaseState.Purchasing -> {
                _subscriptionUiState.value = BillingUiState.Loading
            }

            else -> {}
        }
    }

    private fun awardCoins(purchase: PurchaseState.Purchased) {
        viewModelScope.launch {
            _isPurchasingCoins.value = true
            try {
                Log.d(TAG, "开始发放金币奖励: productId = ${purchase.productId}")
                // 1. 调用 BillingManager 消耗商品
                val consumed = billingRepository.consumePurchase(purchase.purchaseToken)
                Log.d(TAG, "消耗商品结果: consumed = $consumed")
                if (consumed) {
                    // 2. 根据 ID 发放金币
                    val amount = ProductType.getCoinAmount(purchase.productId)
                    if (amount > 0) {
                        Log.d(TAG, "发放金币数量: $amount")
                        userRepository.addCoins(amount)
                        _coinPurchaseSuccess.value = true
                    }
                    // 3. 消耗成功后重置状态，防止重复处理
                    billingRepository.resetPurchaseState()
                } else {
                    _subscriptionUiState.value = BillingUiState.Error("Failed to consume purchase")
                }
            } catch (e: Exception) {
                Log.e(TAG, "发放奖励异常", e)
                _subscriptionUiState.value = BillingUiState.Error("Reward distribution failed")
            } finally {
                _isPurchasingCoins.value = false
            }
        }
    }

    /**
     * 刷新订阅状态 (静默刷新，不阻塞 UI)
     */
    fun refreshSubscriptionStatus() {
        viewModelScope.launch {
            try {
                billingRepository.refreshSubscriptionStatus()
            } catch (e: Exception) {
                Log.e(TAG, "refreshSubscriptionStatus failed", e)
            }
        }
    }

    /**
     * 发起订阅购买 (直接拉起 GP，不通过后端订单，用于保底)
     */
    fun purchaseSubscription(activity: Activity, productDetails: ProductDetails) {
        viewModelScope.launch {
            Log.d(TAG, "直接拉起订阅(保底模式): sku = ${productDetails.productId}")
            _subscriptionUiState.value = BillingUiState.Loading
            try {
                billingRepository.purchaseProduct(activity, productDetails)
            } catch (e: Exception) {
                _subscriptionUiState.value = BillingUiState.Error(e.message ?: "Purchase failed")
            }
        }
    }

    /**
     * 发起金币购买 (内购) (直接拉起 GP，不通过后端订单，用于保底)
     */
    fun purchaseCoins(activity: Activity, productDetails: ProductDetails) {
        viewModelScope.launch {
            Log.d(TAG, "直接拉起金币购买(保底模式): sku = ${productDetails.productId}")
            _isPurchasingCoins.value = true
            try {
                billingRepository.purchaseProduct(activity, productDetails)
            } catch (e: Exception) {
                _isPurchasingCoins.value = false
                _subscriptionUiState.value = BillingUiState.Error(e.message ?: "Purchase failed")
            }
        }
    }

    /**
     * 统一支付流程入口
     * 根据 openThird 状态自动决定：是直接下单(GP) 还是 弹出渠道选择
     */
    fun startPurchaseFlow(
        activity: Activity,
        product: com.obscura.wallpapers.core.data.remote.service.CoinProduct,
        availablePlayProducts: List<ProductDetails>,
        onShowChannelDialog: (com.obscura.wallpapers.core.data.remote.service.CoinProduct) -> Unit
    ) {
        viewModelScope.launch {
            val openThirdValue = userRepository.openThird.first()
            Log.d(
                TAG,
                "startPurchaseFlow (强制从 Repo 读取): productId=${product.id}, sku=${product.sku}, openThird=$openThirdValue"
            )

            // 1. 寻找匹配的 Google Play 产品详情
            val playProduct = availablePlayProducts.find {
                it.productId == product.sku || it.productId == product.id?.toString()
            }
            Log.d(TAG, "匹配到的 GooglePlay 产品: ${playProduct?.productId ?: "未找到"}")

            if (openThirdValue) {
                // 情况 A: 开启了多渠道
                val channels = product.channels
                if (!channels.isNullOrEmpty()) {
                    Log.d(TAG, "多渠道模式: 渠道数量=${channels.size}")

                    if (channels.size == 1 && channels[0].channel == "1") {
                        Log.d(TAG, "多渠道模式: 仅有 Google Play 渠道，直接下单")
                        purchaseWithChannel(activity, product, channels[0])
                    } else {
                        Log.d(TAG, "多渠道模式: 显示渠道选择弹窗")
                        onShowChannelDialog(product)
                    }
                } else {
                    Log.w(TAG, "多渠道模式: 后端未返回渠道，尝试直接拉起 GP 支付(保底)")
                    playProduct?.let {
                        if (it.productType == com.android.billingclient.api.BillingClient.ProductType.SUBS) {
                            purchaseSubscription(activity, it)
                        } else {
                            purchaseCoins(activity, it)
                        }
                    } ?: Log.e(TAG, "多渠道模式: 保底失败，未找到对应的 GooglePlay 产品")
                }
            } else {
                // 情况 B: 关闭三方支付，强制 Google Play
                Log.d(TAG, "强制 GP 模式")
                if (playProduct != null) {
                    Log.d(TAG, "强制 GP 模式: 走后端下单流程")
                    purchaseWithOrder(
                        activity = activity, product = product, productDetails = playProduct
                    )
                } else {
                    Log.e(TAG, "强制 GP 模式: 未找到关联产品，且无可用 GooglePlay 产品详情")
                }
            }
        }
    }

    /**
     * 发起渠道支付 (带后端下单)
     */
    fun purchaseWithChannel(
        activity: Activity,
        product: com.obscura.wallpapers.core.data.remote.service.CoinProduct,
        channel: com.obscura.wallpapers.core.data.remote.service.PayChannel
    ) {
        viewModelScope.launch {
            val isCoin = ProductType.getAllInAppProductIds()
                .contains(product.sku) || ProductType.getAllInAppProductIds()
                .contains(product.id?.toString())

            Log.d(
                TAG,
                "purchaseWithChannel: channelCode=${channel.channel}, payType=${channel.payType}, isCoin=$isCoin"
            )

            if (isCoin) _isPurchasingCoins.value = true
            _subscriptionUiState.value = BillingUiState.Loading

            val productId = product.id ?: 0
            val payType = channel.payType ?: 0
            val channelCode = channel.channel ?: ""

            try {
                Log.d(
                    TAG,
                    "正在创建后端订单: productId=$productId, payType=$payType, channel=$channelCode"
                )
                val result = userRepository.createOrder(
                    productId = productId, payType = payType, channel = channelCode
                )
                if (result is ApiResult.Success) {
                    val orderResponse = result.data
                    Log.d(
                        TAG,
                        "后端下单成功: orderId=${orderResponse.id}, isGooglePay=${orderResponse.apiIsGooglePay()}"
                    )

                    if (orderResponse.apiIsGooglePay()) {
                        val playProduct = if (isCoin) {
                            availableInAppProducts.value.find { it.productId == product.sku || it.productId == product.id?.toString() }
                        } else {
                            availableSubscriptions.value.find { it.productId == product.sku || it.productId == product.id?.toString() }
                        }

                        if (playProduct != null) {
                            Log.d(TAG, "拉起 Google Play 支付: sku=${playProduct.productId}")
                            billingRepository.purchaseProduct(activity, playProduct)
                        } else {
                            Log.e(TAG, "下单成功但未找到对应的 Play Store 产品: sku=${product.sku}")
                            _subscriptionUiState.value =
                                BillingUiState.Error("Play Store product not found")
                            _isPurchasingCoins.value = false
                        }
                    } else if (!orderResponse.payUrl.isNullOrEmpty()) {
                        Log.d(TAG, "跳转三方支付 URL: ${orderResponse.payUrl}")
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(orderResponse.payUrl)
                        )
                        activity.startActivity(intent)
                        _subscriptionUiState.value = BillingUiState.Success
                        _isPurchasingCoins.value = false
                    } else {
                        Log.e(TAG, "后端响应异常: 无 payUrl 且非 GP 支付")
                        _subscriptionUiState.value =
                            BillingUiState.Error("Invalid payment response")
                        _isPurchasingCoins.value = false
                    }
                } else if (result is ApiResult.Error) {
                    Log.e(TAG, "创建后端订单失败: ${result.message}")
                    _subscriptionUiState.value = BillingUiState.Error(result.message)
                    _isPurchasingCoins.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "创建订单流程异常", e)
                _subscriptionUiState.value = BillingUiState.Error(e.message ?: "Purchase failed")
                _isPurchasingCoins.value = false
            }
        }
    }

    /**
     * 发起 Google Play 支付 (带后端下单)
     */
    fun purchaseWithOrder(
        activity: Activity,
        product: com.obscura.wallpapers.core.data.remote.service.CoinProduct,
        productDetails: ProductDetails
    ) {
        viewModelScope.launch {
            val isCoin =
                productDetails.productType == com.android.billingclient.api.BillingClient.ProductType.INAPP
            Log.d(TAG, "purchaseWithOrder: sku=${productDetails.productId}, isCoin=$isCoin")

            if (isCoin) _isPurchasingCoins.value = true
            _subscriptionUiState.value = BillingUiState.Loading

            val productId = product.id ?: 0

            try {
                Log.d(TAG, "正在创建 GP 模式后端订单: productId=$productId")
                val result = userRepository.createOrder(
                    productId = productId, payType = 1, channel = "1"
                )
                if (result is ApiResult.Success) {
                    Log.d(TAG, "GP 模式后端下单成功，拉起 Google Play")
                    billingRepository.purchaseProduct(activity, productDetails)
                } else if (result is ApiResult.Error) {
                    Log.e(TAG, "GP 模式后端下单失败: ${result.message}")
                    _subscriptionUiState.value = BillingUiState.Error(result.message)
                    _isPurchasingCoins.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "GP 模式下单异常", e)
                _isPurchasingCoins.value = false
                _subscriptionUiState.value = BillingUiState.Error(e.message ?: "Purchase failed")
            }
        }
    }

    fun resetCoinPurchaseStatus() {
        _coinPurchaseSuccess.value = false
    }

    /**
     * 恢复购买
     */
    fun restorePurchases() {
        viewModelScope.launch {
            Log.d(TAG, "开始恢复购买...")
            _subscriptionUiState.value = BillingUiState.Loading
            try {
                val restored = billingRepository.restorePurchases()
                Log.d(TAG, "恢复购买结果: $restored")
                _subscriptionUiState.value = if (restored) {
                    BillingUiState.Success
                } else {
                    BillingUiState.Error("No purchases found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "恢复购买异常", e)
                _subscriptionUiState.value = BillingUiState.Error(e.message ?: "Restore failed")
            }
        }
    }
}

/**
 * 计费 UI 状态
 */
sealed class BillingUiState {
    object Idle : BillingUiState()
    object Loading : BillingUiState()
    object Success : BillingUiState()
    data class Error(val message: String) : BillingUiState()
}

data class CoinPackage(
    val amount: Int, val price: String, val bonus: Int
)
