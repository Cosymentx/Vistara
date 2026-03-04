package com.obscura.wallpapers.features.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.obscura.wallpapers.core.billing.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 订阅页面 ViewModel
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {
    
    // 是否为高级用户
    val isPremium: StateFlow<Boolean> = billingRepository.isPremiumUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // 可用产品列表
    val availableProducts: StateFlow<List<ProductDetails>> = billingRepository.availableProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // UI 状态
    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Idle)
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()
    
    init {
        refreshSubscriptionStatus()
    }
    
    /**
     * 刷新订阅状态
     */
    fun refreshSubscriptionStatus() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            try {
                billingRepository.refreshSubscriptionStatus()
                _uiState.value = SubscriptionUiState.Success
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 发起购买
     */
    fun purchaseProduct(activity: Activity, productDetails: ProductDetails) {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            try {
                billingRepository.purchaseProduct(activity, productDetails)
                // 购买结果会通过 BillingManager 的回调处理
                _uiState.value = SubscriptionUiState.Success
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Purchase failed")
            }
        }
    }
    
    /**
     * 恢复购买
     */
    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            try {
                val restored = billingRepository.restorePurchases()
                _uiState.value = if (restored) {
                    SubscriptionUiState.Success
                } else {
                    SubscriptionUiState.Error("No purchases found")
                }
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState.Error(e.message ?: "Restore failed")
            }
        }
    }
}

/**
 * 订阅 UI 状态
 */
sealed class SubscriptionUiState {
    object Idle : SubscriptionUiState()
    object Loading : SubscriptionUiState()
    object Success : SubscriptionUiState()
    data class Error(val message: String) : SubscriptionUiState()
}
