package com.obscura.wallpapers.features.diamond

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.core.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiamondPurchaseViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _purchaseSuccess = MutableStateFlow(false)
    val purchaseSuccess: StateFlow<Boolean> = _purchaseSuccess.asStateFlow()

    fun purchasePackage(diamondPackage: DiamondPackage) {
        viewModelScope.launch {
            _isPurchasing.value = true
            try {
                // TODO: 替换为实际的支付逻辑 (Google Play Billing)
                // 这里模拟一个成功的支付流程
                kotlinx.coroutines.delay(2000)
                
                val currentBalance = userRepository.diamondBalance.first()
                userRepository.updateDiamondBalance(currentBalance + diamondPackage.amount + diamondPackage.bonus)
                
                _purchaseSuccess.value = true
            } catch (e: Exception) {
                // 处理错误
            } finally {
                _isPurchasing.value = false
            }
        }
    }

    fun resetPurchaseStatus() {
        _purchaseSuccess.value = false
    }
}
