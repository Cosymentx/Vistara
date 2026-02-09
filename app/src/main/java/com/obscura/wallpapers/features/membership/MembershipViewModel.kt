package com.obscura.wallpapers.features.membership

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.obscura.wallpapers.R
import com.obscura.wallpapers.billing.BillingConnectionState
import com.obscura.wallpapers.billing.BillingManager
import com.obscura.wallpapers.billing.PurchaseState
import com.obscura.wallpapers.core.data.model.DiamondProduct
import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.remote.service.PaymentMethod
import com.obscura.wallpapers.core.data.repository.DiamondRepository
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.settings.ThemeManager
import com.obscura.wallpapers.features.membership.OrderCreationState
import com.obscura.wallpapers.di.ActivityScopeHolder
import com.obscura.wallpapers.core.common.AppConstants.PRIVACY_POLICY_URL
import com.obscura.wallpapers.core.common.AppConstants.TERMS_OF_SERVICE_URL
import com.obscura.wallpapers.core.common.StringProvider
import com.obscura.wallpapers.core.data.remote.service.CreateOrderResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UpgradeResult {
    data class Success(val message: String) : UpgradeResult()
    data class Error(val message: String) : UpgradeResult()
}

sealed class OrderCreationState {
    object Idle : OrderCreationState()
    object Loading : OrderCreationState()
    data class Success(val orderResponse: CreateOrderResponse) : OrderCreationState()
    data class Error(val message: String) : OrderCreationState()
}

enum class PremiumPlan {
    WEEKLY, MONTHLY, QUARTERLY, YEARLY, LIFETIME
}

@HiltViewModel
class MembershipViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val billingManager: BillingManager,
    private val stringProvider: StringProvider,
    private val themeManager: ThemeManager,
    private val diamondRepository: DiamondRepository,
    private val activityScopeHolder: ActivityScopeHolder,
) : ViewModel() {

    private var navController: NavController? = null

    private val _darkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _canPayment = MutableStateFlow(false)
    val canPayment: StateFlow<Boolean> = _canPayment.asStateFlow()

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    private val _isUpgrading = MutableStateFlow(false)
    val isUpgrading: StateFlow<Boolean> = _isUpgrading.asStateFlow()

    private val _upgradeResult = MutableStateFlow<UpgradeResult?>(null)
    val upgradeResult: StateFlow<UpgradeResult?> = _upgradeResult.asStateFlow()

    private val _selectedPlan = MutableStateFlow<PremiumPlan?>(null)
    val selectedPlan: StateFlow<PremiumPlan?> = _selectedPlan.asStateFlow()

    private val _billingConnectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    val billingConnectionState: StateFlow<BillingConnectionState> = _billingConnectionState.asStateFlow()

    private val _productPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    val productPrices: StateFlow<Map<String, String>> = _productPrices.asStateFlow()

    private val _subscriptionProducts = MutableStateFlow<List<DiamondProduct>>(emptyList())
    val subscriptionProducts: StateFlow<List<DiamondProduct>> = _subscriptionProducts.asStateFlow()

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
        _upgradeResult.value = null
        checkPremiumStatus()
        observePaymentStatus()
        observeBillingState()
        observePurchaseState()
        observeDarkTheme()
        loadSubscriptionProducts()
    }

    fun setNavController(nav: NavController) {
        navController = nav
    }

    private fun observePaymentStatus() {
        viewModelScope.launch {
            _canPayment.value = userRepository.getCachedUserProfile()?.isWhitelisted == true ||
                (!_isPremiumUser.value && !_isUpgrading.value &&
                    _billingConnectionState.value == BillingConnectionState.CONNECTED &&
                    _orderCreationState.value != OrderCreationState.Loading)
        }
    }

    private fun observeDarkTheme() {
        viewModelScope.launch {
            _darkTheme.value = themeManager.isDarkTheme().first()
        }
    }

    private fun checkPremiumStatus() {
        viewModelScope.launch {
            try {
                val isPremium = userRepository.isPremiumUser.first()
                _isPremiumUser.value = isPremium
                Log.d("MembershipViewModel", "Premium status: $isPremium")
            } catch (e: Exception) {
                Log.e("MembershipViewModel", "Error checking premium status: ${e.message}")
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
    }

    private fun observePurchaseState() {
        viewModelScope.launch {
            billingManager.purchaseState.collectLatest { state ->
                when (state) {
                    is PurchaseState.Pending -> _isUpgrading.value = true
                    is PurchaseState.Completed -> {
                        _isUpgrading.value = false
                        _isPremiumUser.value = true
                        _upgradeResult.value = UpgradeResult.Success(stringProvider.getString(R.string.upgrade_success))
                    }
                    is PurchaseState.Failed -> {
                        _isUpgrading.value = false
                        _upgradeResult.value = UpgradeResult.Error(stringProvider.getString(R.string.upgrade_failed, state.message))
                    }
                    is PurchaseState.Cancelled -> {
                        _isUpgrading.value = false
                        _upgradeResult.value = UpgradeResult.Error(stringProvider.getString(R.string.upgrade_cancelled))
                    }
                    is PurchaseState.Restoring -> _isUpgrading.value = true
                    else -> _isUpgrading.value = false
                }
            }
        }
    }

    private fun updateProductPrices() {
        val prices = mutableMapOf<String, String>()
        prices[BillingManager.SUBSCRIPTION_WEEKLY] = billingManager.getProductPrice(BillingManager.SUBSCRIPTION_WEEKLY)
        prices[BillingManager.SUBSCRIPTION_MONTHLY] = billingManager.getProductPrice(BillingManager.SUBSCRIPTION_MONTHLY)
        prices[BillingManager.SUBSCRIPTION_QUARTERLY] = billingManager.getProductPrice(BillingManager.SUBSCRIPTION_QUARTERLY)
        _productPrices.value = prices
    }

    private fun loadSubscriptionProducts() {
        viewModelScope.launch {
            try {
                _apiProductsLoading.value = true
                _apiProductsError.value = null
                val result = diamondRepository.getDiamondProducts()
                result.onSuccess { products ->
                    val subscriptionProducts = products.filter { it.priceType == "2" }
                    val sortedProducts = subscriptionProducts.sortedBy { product ->
                        when {
                            product.productId?.contains("vistara_sub_week", ignoreCase = true) == true -> 0
                            product.productId?.contains("vistara_sub_month", ignoreCase = true) == true -> 1
                            product.productId?.contains("vistara_sub_quarter", ignoreCase = true) == true -> 2
                            else -> 3
                        }
                    }
                    _subscriptionProducts.value = sortedProducts
                    sortedProducts.firstOrNull()?.let { _selectedPlan.value = PremiumPlan.WEEKLY }
                }.onError { _, message, _ ->
                    _apiProductsError.value = message
                    createDefaultSubscriptionProducts()
                }
            } catch (e: Exception) {
                _apiProductsError.value = e.message
                createDefaultSubscriptionProducts()
            } finally {
                _apiProductsLoading.value = false
            }
        }
    }

    private fun createDefaultSubscriptionProducts() {
        val defaultProducts = listOf(
            DiamondProduct(
                id = "subscription_weekly",
                name = stringProvider.getString(R.string.membership_plan_title_week),
                itemName = stringProvider.getString(R.string.membership_plan_title_week),
                diamondAmount = 0,
                price = 19.99,
                currency = "¥",
                productId = BillingManager.SUBSCRIPTION_WEEKLY,
                discount = 0,
                payMethodId = 0,
                dollarPrice = null,
                googlePlayProductId = null
            ),
            DiamondProduct(
                id = "subscription_monthly",
                name = stringProvider.getString(R.string.membership_plan_title_month),
                itemName = stringProvider.getString(R.string.membership_plan_title_month),
                diamondAmount = 0,
                price = 49.99,
                currency = "¥",
                productId = BillingManager.SUBSCRIPTION_MONTHLY,
                discount = 0,
                payMethodId = 0,
                dollarPrice = null,
                googlePlayProductId = null
            ),
            DiamondProduct(
                id = "subscription_quarterly",
                name = stringProvider.getString(R.string.membership_plan_title_quarter),
                itemName = stringProvider.getString(R.string.membership_plan_title_quarter),
                diamondAmount = 0,
                price = 129.99,
                currency = "¥",
                productId = BillingManager.SUBSCRIPTION_QUARTERLY,
                discount = 0,
                payMethodId = 0,
                dollarPrice = null,
                googlePlayProductId = null
            )
        )
        _subscriptionProducts.value = defaultProducts
        _selectedPlan.value = PremiumPlan.WEEKLY
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            _paymentMethodsLoading.value = true
            _paymentMethodsError.value = null
            try {
                if (_selectedPlan.value == null) _selectedPlan.value = PremiumPlan.MONTHLY
                val productId = when (_selectedPlan.value) {
                    PremiumPlan.WEEKLY -> BillingManager.SUBSCRIPTION_WEEKLY
                    PremiumPlan.MONTHLY -> BillingManager.SUBSCRIPTION_MONTHLY
                    PremiumPlan.QUARTERLY -> BillingManager.SUBSCRIPTION_QUARTERLY
                    PremiumPlan.YEARLY -> BillingManager.SUBSCRIPTION_MONTHLY
                    PremiumPlan.LIFETIME -> BillingManager.SUBSCRIPTION_QUARTERLY
                    null -> BillingManager.SUBSCRIPTION_MONTHLY
                }
                val itemName = subscriptionProducts.value.find { it.productId == productId }?.itemName ?: ""
                val result = diamondRepository.getPaymentMethods(itemName)
                if (result is ApiResult.Success) {
                    _paymentMethods.value = result.data
                } else if (result is ApiResult.Error) {
                    _paymentMethodsError.value = result.message
                }
            } catch (e: Exception) {
                _paymentMethodsError.value = e.message
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
                if (_billingConnectionState.value != BillingConnectionState.CONNECTED) {
                    _upgradeResult.value = UpgradeResult.Error(stringProvider.getString(R.string.payment_service_not_connected))
                } else {
                    handlePaymentMethodSelected()
                }
            }
        }
    }

    fun hidePaymentDialog() {
        _showPaymentDialog.value = false
    }

    fun selectPlan(plan: PremiumPlan) {
        _selectedPlan.value = plan
    }

    fun upgrade(activity: Activity?) {
        if (_isPremiumUser.value) {
            _upgradeResult.value = UpgradeResult.Error(stringProvider.getString(R.string.membership_already_premium))
            return
        }
        showPaymentDialog()
    }

    fun handlePaymentMethodSelected(paymentMethodId: String? = null) {
        if (_selectedPlan.value == null) _selectedPlan.value = PremiumPlan.MONTHLY
        val productId = when (_selectedPlan.value) {
            PremiumPlan.WEEKLY -> BillingManager.SUBSCRIPTION_WEEKLY
            PremiumPlan.MONTHLY -> BillingManager.SUBSCRIPTION_MONTHLY
            PremiumPlan.QUARTERLY -> BillingManager.SUBSCRIPTION_QUARTERLY
            PremiumPlan.YEARLY -> BillingManager.SUBSCRIPTION_MONTHLY
            PremiumPlan.LIFETIME -> BillingManager.SUBSCRIPTION_QUARTERLY
            null -> BillingManager.SUBSCRIPTION_MONTHLY
        }
        _orderCreationState.value = OrderCreationState.Loading
        viewModelScope.launch {
            try {
                val result = diamondRepository.createOrder(
                    productId = paymentMethodId ?: subscriptionProducts.value.find { it.productId == productId }?.id ?: "",
                    paymentMethodId = productId
                )
                result.onSuccess { orderResponse ->
                    _orderCreationState.value = OrderCreationState.Success(orderResponse)
                    if (orderResponse.isGooglePay) {
                        _showPaymentDialog.value = false
                    } else {
                        _paymentUrl.value = orderResponse.payUrl
                    }
                }.onError { _, message, _ ->
                    _orderCreationState.value = OrderCreationState.Error(message)
                }
            } catch (e: Exception) {
                _orderCreationState.value = OrderCreationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clearUpgradeResult() { _upgradeResult.value = null }
    fun clearPaymentUrl() { _paymentUrl.value = null }
    fun openTermsOfService() { context.startActivity(Intent(Intent.ACTION_VIEW, TERMS_OF_SERVICE_URL.toUri())) }
    fun openPrivacyPolicy() { context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri())) }
    fun openSubscriptionManagementPage(activity: Activity?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
        activity?.startActivity(intent)
    }
}
