package com.obscura.wallpapers.features.recharge

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.billing.BillingConnectionState
import com.obscura.wallpapers.billing.BillingManager
import com.obscura.wallpapers.billing.PurchaseState
import com.obscura.wallpapers.core.data.model.DiamondProduct
import com.obscura.wallpapers.core.data.model.DiamondTransaction
import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.remote.service.CreateOrderResponse
import com.obscura.wallpapers.core.data.remote.service.PaymentMethod
import com.obscura.wallpapers.core.data.repository.DiamondRepository
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.di.ActivityScopeHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RechargeViewModel @Inject constructor(
    private val application: Application,
    private val userRepository: UserRepository,
    private val diamondRepository: DiamondRepository,
    private val billingManager: BillingManager,
    private val activityScopeHolder: ActivityScopeHolder,
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "RechargeViewModel"
    }

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

    init {
        loadData()
        observeBillingState()
    }

    private fun loadData() {
        viewModelScope.launch {
            diamondRepository.getDiamondBalance().collectLatest { balance ->
                _diamondBalance.value = balance
                Log.d(TAG, "Diamond balance updated: $balance")
            }
        }

        viewModelScope.launch {
            try {
                _apiProductsLoading.value = true
                val products = diamondRepository.getDiamondProducts()
                products?.onSuccess {
                    _diamondProducts.value = it.filter { it.priceType == "1" }
                }

                if (products.getOrNull()?.isNotEmpty() == true && _selectedProduct.value == null) {
                }
            } catch (e: Exception) {
                _apiProductsError.value = e.message
                Log.e(TAG, "Error loading diamond products: ${e.message}", e)
            } finally {
                _apiProductsLoading.value = false
            }
        }
    }

    private fun refreshDiamondBalance() {
        viewModelScope.launch {
            try {
                val balance = diamondRepository.getDiamondBalanceValue()
                _diamondBalance.value = balance
                Log.d(TAG, "Diamond balance refreshed: $balance")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh diamond balance", e)
            }
        }
    }

    private fun observeBillingState() {
        viewModelScope.launch {
            billingManager.connectionState.collectLatest { state ->
                _billingConnectionState.value = state
                if (state == BillingConnectionState.CONNECTED) {
                    updateProductPrices()
                }
            }
        }

        viewModelScope.launch {
            billingManager.purchaseState.collectLatest { state ->
                _purchaseState.value = state
                if (state == PurchaseState.Completed) {
                    refreshDiamondBalance()
                    delay(500)
                    refreshDiamondBalance()
                }
            }
        }
    }

    private fun updateProductPrices() {
        val priceMap = mutableMapOf<String, String>()

        BillingManager.DIAMOND_SKUS.forEach { productId ->
            val price = billingManager.getProductPrice(productId)
            priceMap[productId] = price
        }

        _productPrices.value = priceMap

        _diamondProducts.value.forEach { product ->
            val productId = product.productId
            val priceFromProductId = productId?.let { priceMap[it] }
            val apiPrice = "${product.currency} ${product.price}"

            Log.d(
                TAG,
                "Product ${product.id}: " + "productId=$productId, " + "price from productId=$priceFromProductId, " + "API price=$apiPrice"
            )

            if (productId != null) {
                val isInSkuList = BillingManager.DIAMOND_SKUS.contains(productId)
            }
        }

        val currentProducts = _diamondProducts.value
        if (currentProducts.isNotEmpty() && (_selectedProduct.value == null || !currentProducts.contains(
                _selectedProduct.value
            ))
        ) {
        }
    }

    fun selectProduct(product: DiamondProduct) {
        _selectedProduct.value = product
    }

    fun purchaseDiamond(activity: Activity?) {
        val product = _selectedProduct.value ?: return
        val billingProductId = product.productId
        if (billingProductId == null) {
            Log.e(TAG, "Cannot purchase product ${product.id}: no valid product ID")
            return
        }
        Log.d(
            TAG,
            "Launching billing flow for product ${product.id} with billing ID: $billingProductId"
        )
        billingManager.launchBillingFlow(activity, billingProductId)
    }

    fun connectBillingService() {
        billingManager.connectToPlayBilling()
    }

    fun loadPaymentMethods() {
        viewModelScope.launch {
            try {
                _paymentMethodsLoading.value = true
                _paymentMethodsError.value = null

                val result =
                    diamondRepository.getPaymentMethods(_selectedProduct.value?.itemName ?: "")
                result.onSuccess { methods ->
                    _paymentMethods.value = methods
                    Log.d(TAG, "Payment methods loaded: ${methods.size}")
                }.onError { code, errorMsg, source ->
                    _paymentMethodsError.value = errorMsg
                    Log.e(TAG, "Failed to load payment methods: $errorMsg")
                }
            } catch (e: Exception) {
                _paymentMethodsError.value = e.message
                Log.e(TAG, "Error loading payment methods: ${e.message}", e)
            } finally {
                _paymentMethodsLoading.value = false
            }
        }
    }

    fun showPaymentDialog() {
        viewModelScope.launch {
            _showPaymentDialog.value = userRepository.getCachedUserProfile()?.isWhitelisted == true
            if (_showPaymentDialog.value) {
                loadPaymentMethods()
            } else {
                handlePaymentMethodSelected()
            }
        }
    }

    fun hidePaymentDialog() {
        _showPaymentDialog.value = false
    }

    fun handlePaymentMethodSelected(paymentMethodId: String? = null) {
        val product = _selectedProduct.value ?: return

        Log.d(
            TAG, "Selected payment method: ${product.id} ${product.productId}"
        )

        _orderCreationState.value = OrderCreationState.Loading

        viewModelScope.launch {
            try {
                val result = diamondRepository.createOrder(
                    productId = paymentMethodId ?: product.id, paymentMethodId = product.id
                )

                result.onSuccess { orderResponse ->
                    _orderCreationState.value = OrderCreationState.Success(orderResponse)
                    when {
                        orderResponse.isGooglePay -> {
                            hidePaymentDialog()
                            val activity = activityScopeHolder.current()
                            if (activity != null) {
                                purchaseDiamond(activity)
                            } else {
                                _orderCreationState.value =
                                    OrderCreationState.Error("无法启动支付，请重试")
                            }
                        }
                        else -> {
                            hidePaymentDialog()
                            _paymentUrl.value = orderResponse.payUrl
                            Log.d(TAG, "Payment URL: ${orderResponse.payUrl}")
                        }
                    }
                }.onError { code, errorMsg, source ->
                    _orderCreationState.value = OrderCreationState.Error(errorMsg)
                    Log.e(TAG, "Failed to create order: $errorMsg")
                }
            } catch (e: Exception) {
                _orderCreationState.value = OrderCreationState.Error(e.message ?: "Unknown error")
                Log.e(TAG, "Error creating order: ${e.message}", e)
            }
        }
    }

    fun clearPaymentUrl() {
        _paymentUrl.value = null
    }

    fun loadTransactions() {
        viewModelScope.launch {
            try {
                _transactionsLoading.value = true
                _transactionsError.value = null

                val result = diamondRepository.getRemoteTransactions()

                if (result is ApiResult.Success) {
                    _transactions.value = result.data
                    Log.d(TAG, "Transactions loaded from API: ${result.data.size}")
                    _transactionsLoading.value = false
                } else if (result is ApiResult.Error) {
                    _transactionsError.value = result.message
                    Log.e(TAG, "Failed to load transactions from API: ${result.message}")

                    try {
                        val localTransactions = diamondRepository.getTransactions().first()
                        _transactions.value = localTransactions
                        Log.d(TAG, "Transactions loaded from local DB: ${localTransactions.size}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading local transactions: ${e.message}", e)
                    } finally {
                        _transactionsLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _transactionsError.value = e.message
                Log.e(TAG, "Error loading transactions: ${e.message}", e)

                try {
                    val localTransactions = diamondRepository.getTransactions().first()
                    _transactions.value = localTransactions
                    Log.d(TAG, "Transactions loaded from local DB after error: ${localTransactions.size}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading local transactions: ${e.message}", e)
                } finally {
                    _transactionsLoading.value = false
                }
            }
        }
    }
}

sealed class OrderCreationState {
    object Idle : OrderCreationState()
    object Loading : OrderCreationState()
    data class Success(val orderResponse: CreateOrderResponse) : OrderCreationState()

    data class Error(val message: String) : OrderCreationState()
}
