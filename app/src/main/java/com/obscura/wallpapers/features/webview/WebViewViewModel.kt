package com.obscura.wallpapers.features.webview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.repository.DiamondRepository
import com.obscura.wallpapers.core.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val diamondRepository: DiamondRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    companion object {
        private const val TAG = "WebViewViewModel"
    }

    private val _paymentProcessingState = MutableStateFlow<PaymentProcessingState>(PaymentProcessingState.Idle)
    val paymentProcessingState: StateFlow<PaymentProcessingState> = _paymentProcessingState.asStateFlow()

    fun handlePaymentSuccessUrl(url: String): Boolean {
        if (url.contains("status=SUCCESS", ignoreCase = true)) {
            Log.d(TAG, "Payment success URL detected: $url")

            val outTradeNo = extractOutTradeNo(url)
            if (outTradeNo != null) {
                Log.d(TAG, "Extracted outTradeNo: $outTradeNo")
                checkOrderStatus(outTradeNo)
                return true
            } else {
                Log.e(TAG, "Failed to extract outTradeNo from URL: $url")
            }
        }
        return false
    }

    private fun extractOutTradeNo(url: String): String? {
        val regex = "outTradeNo=([^&]+)".toRegex()
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.getOrNull(1)
    }

    private fun checkOrderStatus(outTradeNo: String) {
        viewModelScope.launch {
            try {
                _paymentProcessingState.value = PaymentProcessingState.CheckingOrder

                val result = diamondRepository.checkOrder(outTradeNo)

                if (result is ApiResult.Success) {
                    Log.d(TAG, "Order check successful: ${result.data}")
                    _paymentProcessingState.value = PaymentProcessingState.OrderCheckSuccess

                    refreshUserProfile()
                } else if (result is ApiResult.Error) {
                    Log.e(TAG, "Order check failed: ${result.message}")
                    _paymentProcessingState.value = PaymentProcessingState.OrderCheckFailed(result.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking order: ${e.message}", e)
                _paymentProcessingState.value = PaymentProcessingState.OrderCheckFailed(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun refreshUserProfile() {
        try {
            _paymentProcessingState.value = PaymentProcessingState.RefreshingUserProfile

            userRepository.refreshUserProfile()

            _paymentProcessingState.value = PaymentProcessingState.RefreshUserProfileSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing user profile: ${e.message}", e)
            _paymentProcessingState.value = PaymentProcessingState.RefreshUserProfileFailed(e.message ?: "Unknown error")
        }
    }

    fun resetPaymentProcessingState() {
        _paymentProcessingState.value = PaymentProcessingState.Idle
    }
}

sealed class PaymentProcessingState {
    object Idle : PaymentProcessingState()
    object CheckingOrder : PaymentProcessingState()
    object OrderCheckSuccess : PaymentProcessingState()
    data class OrderCheckFailed(val error: String) : PaymentProcessingState()
    object RefreshingUserProfile : PaymentProcessingState()
    object RefreshUserProfileSuccess : PaymentProcessingState()
    data class RefreshUserProfileFailed(val error: String) : PaymentProcessingState()
}
