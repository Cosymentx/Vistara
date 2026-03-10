package com.obscura.wallpapers.features.billing

import android.app.Activity
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

    // UI 状态
    private val _subscriptionUiState = MutableStateFlow<BillingUiState>(BillingUiState.Idle)
    val subscriptionUiState: StateFlow<BillingUiState> = _subscriptionUiState.asStateFlow()

    // 金币购买状态
    private val _isPurchasingCoins = MutableStateFlow(false)
    val isPurchasingCoins: StateFlow<Boolean> = _isPurchasingCoins.asStateFlow()

    // 是否展示三方支付渠道
    val openThird: StateFlow<Boolean> = userRepository.openThird.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _coinPurchaseSuccess = MutableStateFlow(false)
    val coinPurchaseSuccess: StateFlow<Boolean> = _coinPurchaseSuccess.asStateFlow()

    // 是否为高级用户 (订阅)
    val isPremium: StateFlow<Boolean> = billingRepository.isPremiumUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
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
                } else if (result is ApiResult.Error) {
                    android.util.Log.e("BillingViewModel", "获取订阅产品失败: ${result.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("BillingViewModel", "获取订阅产品异常", e)
            } finally {
                _isLoadingProducts.value = false
            }
        }
    }

    private fun handlePurchaseState(state: PurchaseState) {
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
                // 1. 调用 BillingManager 消耗商品
                val consumed = billingRepository.consumePurchase(purchase.purchaseToken)
                if (consumed) {
                    // 2. 根据 ID 发放金币
                    val amount = ProductType.getCoinAmount(purchase.productId)
                    if (amount > 0) {
                        userRepository.addCoins(amount)
                        _coinPurchaseSuccess.value = true
                    }
                    // 3. 消耗成功后重置状态，防止重复处理
                    billingRepository.resetPurchaseState()
                } else {
                    _subscriptionUiState.value = BillingUiState.Error("Failed to consume purchase")
                }
            } catch (e: Exception) {
                _subscriptionUiState.value = BillingUiState.Error("Reward distribution failed")
            } finally {
                _isPurchasingCoins.value = false
            }
        }
    }

    /**
     * 刷新订阅状态
     */
    fun refreshSubscriptionStatus() {
        viewModelScope.launch {
            _subscriptionUiState.value = BillingUiState.Loading
            try {
                billingRepository.refreshSubscriptionStatus()
                _subscriptionUiState.value = BillingUiState.Success
            } catch (e: Exception) {
                _subscriptionUiState.value = BillingUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 发起订阅购买
     */
    fun purchaseSubscription(activity: Activity, productDetails: ProductDetails) {
        viewModelScope.launch {
            _subscriptionUiState.value = BillingUiState.Loading
            try {
                billingRepository.purchaseProduct(activity, productDetails)
            } catch (e: Exception) {
                _subscriptionUiState.value = BillingUiState.Error(e.message ?: "Purchase failed")
            }
        }
    }

    /**
     * 发起金币购买 (内购)
     */
    fun purchaseCoins(activity: Activity, productDetails: ProductDetails) {
        viewModelScope.launch {
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
     * 发起渠道支付 (带后端下单)
     */
    fun purchaseWithChannel(
        activity: Activity,
        product: com.obscura.wallpapers.core.data.remote.service.CoinProduct,
        channel: com.obscura.wallpapers.core.data.remote.service.PayChannel
    ) {
        viewModelScope.launch {
            // 根据产品类型设置加载状态
            val isCoin = ProductType.getAllInAppProductIds().contains(product.sku) || 
                         ProductType.getAllInAppProductIds().contains(product.id?.toString())
            if (isCoin) _isPurchasingCoins.value = true
            _subscriptionUiState.value = BillingUiState.Loading
            
            val productId = product.id ?: 0
            val payType = channel.payType ?: 0
            val channelCode = channel.channel ?: ""
            
            android.util.Log.d("BillingViewModel", "开始渠道支付下单: productId=$productId, payType=$payType, channel=$channelCode")
            
            try {
                // 1. 调用后端接口创建订单 (使用新的 CheckoutRequest 结构)
                val result = userRepository.createOrder(
                    productId = productId,
                    payType = payType,
                    channel = channelCode
                )
                if (result is ApiResult.Success) {
                    val orderResponse = result.data
                    android.util.Log.d("BillingViewModel", "下单成功: orderId=${orderResponse.id}, isGooglePay=${orderResponse.apiIsGooglePay()}")
                    
                    if (orderResponse.apiIsGooglePay()) {
                        // 如果是 Google Play 渠道，查找对应的 ProductDetails 并发起支付
                        val playProduct = if (ProductType.getAllInAppProductIds().contains(product.sku)) {
                             availableInAppProducts.value.find { it.productId == product.sku }
                        } else {
                             availableSubscriptions.value.find { it.productId == product.sku }
                        }
                        
                        if (playProduct != null) {
                            billingRepository.purchaseProduct(activity, playProduct)
                        } else {
                            val errorMsg = "Play Store product not found for SKU: ${product.sku}"
                            _subscriptionUiState.value = BillingUiState.Error(errorMsg)
                            _isPurchasingCoins.value = false
                        }
                    } else if (!orderResponse.payUrl.isNullOrEmpty()) {
                        // 如果是第三方渠道且有支付链接
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(orderResponse.payUrl))
                        activity.startActivity(intent)
                        _subscriptionUiState.value = BillingUiState.Success
                        _isPurchasingCoins.value = false
                    } else {
                        _subscriptionUiState.value = BillingUiState.Error("Invalid payment response: No payUrl")
                        _isPurchasingCoins.value = false
                    }
                } else if (result is ApiResult.Error) {
                    android.util.Log.e("BillingViewModel", "下单失败: ${result.message}")
                    _subscriptionUiState.value = BillingUiState.Error(result.message)
                    _isPurchasingCoins.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("BillingViewModel", "下单异常", e)
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
            val isCoin = ProductType.getAllInAppProductIds().contains(product.sku) || 
                         ProductType.getAllInAppProductIds().contains(product.id?.toString())
            if (isCoin) _isPurchasingCoins.value = true
            _subscriptionUiState.value = BillingUiState.Loading
            
            val productId = product.id ?: 0
            // Google Play 默认 payType=1, channel="1"
            android.util.Log.d("BillingViewModel", "开始 GooglePlay 模式下单: productId=$productId")
            
            try {
                val result = userRepository.createOrder(
                    productId = productId,
                    payType = 1,
                    channel = "1"
                )
                if (result is ApiResult.Success) {
                    android.util.Log.d("BillingViewModel", "GooglePlay 模式下单成功")
                    billingRepository.purchaseProduct(activity, productDetails)
                } else if (result is ApiResult.Error) {
                    android.util.Log.e("BillingViewModel", "GooglePlay 模式下单失败: ${result.message}")
                    _subscriptionUiState.value = BillingUiState.Error(result.message)
                    _isPurchasingCoins.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("BillingViewModel", "GooglePlay 模式下单异常", e)
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
            _subscriptionUiState.value = BillingUiState.Loading
            try {
                val restored = billingRepository.restorePurchases()
                _subscriptionUiState.value = if (restored) {
                    BillingUiState.Success
                } else {
                    BillingUiState.Error("No purchases found")
                }
            } catch (e: Exception) {
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
