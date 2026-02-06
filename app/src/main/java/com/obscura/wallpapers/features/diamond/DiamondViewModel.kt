package com.obscura.wallpapers.features.diamond

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.obscura.wallpapers.billing.BillingConnectionState
import com.obscura.wallpapers.billing.BillingManager
import com.obscura.wallpapers.billing.PurchaseState
import com.obscura.wallpapers.core.data.model.DiamondProduct
import com.obscura.wallpapers.core.data.model.DiamondTransaction
import com.obscura.wallpapers.core.data.remote.service.CreateOrderResponse
import com.obscura.wallpapers.core.data.remote.service.PaymentMethod
import com.obscura.wallpapers.core.data.repository.DiamondRepository
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.di.ActivityScopeHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DiamondViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository,
    private val diamondRepository: DiamondRepository,
    private val billingManager: BillingManager,
    private val activityScopeHolder: ActivityScopeHolder,
) : AndroidViewModel(application) {

    private val _diamondBalance = MutableStateFlow(0)
    val diamondBalance: StateFlow<Int> = _diamondBalance.asStateFlow()

    private val _diamondProducts = MutableStateFlow<List<DiamondProduct>>(emptyList())
    val diamondProducts: StateFlow<List<DiamondProduct>> = _diamondProducts.asStateFlow()

    private val _transactions = MutableStateFlow<List<DiamondTransaction>>(emptyList())
    val transactions: StateFlow<List<DiamondTransaction>> = _transactions.asStateFlow()

    private val _transactionsLoading = MutableStateFlow(false)
    val transactionsLoading: StateFlow<Boolean> = _transactionsLoading.asStateFlow()

    private val _transactionsError = MutableStateFlow<String?>(null)
    val transactionsError: StateFlow<String?> = _transactionsError.asStateFlow()

    private val _selectedProduct = MutableStateFlow<DiamondProduct?>(null)
    val selectedProduct: StateFlow<DiamondProduct?> = _selectedProduct.asStateFlow()

    private val _billingConnectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    val billingConnectionState: StateFlow<BillingConnectionState> =
        _billingConnectionState.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _productPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    val productPrices: StateFlow<Map<String, String>> = _productPrices.asStateFlow()

    private val _apiProductsLoading = MutableStateFlow(false)
    val apiProductsLoading: StateFlow<Boolean> = _apiProductsLoading.asStateFlow()

    private val _apiProductsError = MutableStateFlow<String?>(null)
    val apiProductsError: StateFlow<String?> = _apiProductsError.asStateFlow()

    private val _paymentMethods = MutableStateFlow<List<PaymentMethod>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethod>> = _paymentMethods.asStateFlow()

    private val _paymentMethodsLoading = MutableStateFlow(false)
    val paymentMethodsLoading: StateFlow<Boolean> = _paymentMethodsLoading.asStateFlow()

    private val _paymentMethodsError = MutableStateFlow<String?>(null)
    val paymentMethodsError: StateFlow<String?> = _paymentMethodsError.asStateFlow()

    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog: StateFlow<Boolean> = _showPaymentDialog.asStateFlow()

    private val _orderCreationState = MutableStateFlow<OrderCreationState>(OrderCreationState.Idle)
    val orderCreationState: StateFlow<OrderCreationState> = _orderCreationState.asStateFlow()

    private val _paymentUrl = MutableStateFlow<String?>(null)
    val paymentUrl: StateFlow<String?> = _paymentUrl.asStateFlow()

    fun bindBilling() {}
    fun chooseProduct(product: DiamondProduct) { _selectedProduct.value = product }
    fun presentPaymentDialog() { _showPaymentDialog.value = true }
    fun dismissPaymentDialog() { _showPaymentDialog.value = false }
    fun applyPaymentMethod(paymentMethodId: String? = null) { _orderCreationState.value = OrderCreationState.Error("Not implemented") }
    fun initiatePurchase(activity: Activity?) {}
    fun resetPaymentUrl() { _paymentUrl.value = null }
    fun refreshTransactions() { _transactionsLoading.value = false }
}


sealed class OrderCreationState {
    object Idle : OrderCreationState()
    object Loading : OrderCreationState()
    data class Success(val orderResponse: CreateOrderResponse) : OrderCreationState()
    data class Error(val message: String) : OrderCreationState()
}

sealed class DiamondPurchaseResult {
    data class Success(val message: String) : DiamondPurchaseResult()
    data class Error(val message: String) : DiamondPurchaseResult()
}
