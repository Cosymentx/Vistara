package com.obscura.wallpapers.features.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.obscura.wallpapers.core.billing.BillingRepository
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
    
    // 是否为高级用户 (订阅)
    val isPremium: StateFlow<Boolean> = billingRepository.isPremiumUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // 可用产品列表 (订阅/内购)
    val availableProducts: StateFlow<List<ProductDetails>> = billingRepository.availableProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // UI 状态 (订阅)
    private val _subscriptionUiState = MutableStateFlow<BillingUiState>(BillingUiState.Idle)
    val subscriptionUiState: StateFlow<BillingUiState> = _subscriptionUiState.asStateFlow()

    // 金币购买状态
    private val _isPurchasingCoins = MutableStateFlow(false)
    val isPurchasingCoins: StateFlow<Boolean> = _isPurchasingCoins.asStateFlow()

    private val _coinPurchaseSuccess = MutableStateFlow(false)
    val coinPurchaseSuccess: StateFlow<Boolean> = _coinPurchaseSuccess.asStateFlow()
    
    init {
        refreshSubscriptionStatus()
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
                _subscriptionUiState.value = BillingUiState.Success
            } catch (e: Exception) {
                _subscriptionUiState.value = BillingUiState.Error(e.message ?: "Purchase failed")
            }
        }
    }

    /**
     * 发起金币购买 (内购)
     */
    fun purchaseCoins(diamondPackage: CoinPackage) {
        viewModelScope.launch {
            _isPurchasingCoins.value = true
            try {
                // TODO: 替换为实际的 Google Play Billing 逻辑
                // 这里暂时模拟
                kotlinx.coroutines.delay(2000)
                
                val currentBalance = userRepository.diamondBalance.first()
                userRepository.updateDiamondBalance(currentBalance + diamondPackage.amount + diamondPackage.bonus)
                
                _coinPurchaseSuccess.value = true
            } catch (e: Exception) {
                // 处理错误
            } finally {
                _isPurchasingCoins.value = false
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
