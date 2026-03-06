package com.obscura.wallpapers.features.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.obscura.wallpapers.core.billing.BillingRepository
import com.obscura.wallpapers.core.billing.model.ProductType
import com.obscura.wallpapers.core.billing.model.PurchaseState
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
    private val billingRepository: BillingRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    // UI 状态
    private val _subscriptionUiState = MutableStateFlow<BillingUiState>(BillingUiState.Idle)
    val subscriptionUiState: StateFlow<BillingUiState> = _subscriptionUiState.asStateFlow()

    // 金币购买状态
    private val _isPurchasingCoins = MutableStateFlow(false)
    val isPurchasingCoins: StateFlow<Boolean> = _isPurchasingCoins.asStateFlow()

    private val _coinPurchaseSuccess = MutableStateFlow(false)
    val coinPurchaseSuccess: StateFlow<Boolean> = _coinPurchaseSuccess.asStateFlow()

    // 是否为高级用户 (订阅)
    val isPremium: StateFlow<Boolean> = billingRepository.isPremiumUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // 可用订阅列表
    val availableSubscriptions: StateFlow<List<ProductDetails>> = billingRepository.availableSubscriptions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 可用金币包列表
    val availableInAppProducts: StateFlow<List<ProductDetails>> = billingRepository.availableInAppProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 购买状态监听
    init {
        viewModelScope.launch {
            billingRepository.purchaseState.collect { state ->
                handlePurchaseState(state)
            }
        }
        refreshSubscriptionStatus()
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
    val amount: Int,
    val price: String,
    val bonus: Int
)
