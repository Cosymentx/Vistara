package com.obscura.wallpapers.features.test

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.billing.BillingRepository
import com.obscura.wallpapers.core.billing.model.ProductType
import com.obscura.wallpapers.core.billing.model.PurchaseState
import com.obscura.wallpapers.core.data.local.SubscriptionDao
import com.obscura.wallpapers.core.data.local.entity.UserSubscriptionEntity
import com.obscura.wallpapers.core.data.mapper.PexelsMapper
import com.obscura.wallpapers.core.data.mapper.UnsplashMapper
import com.obscura.wallpapers.core.data.model.Collection
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.remote.ApiResult.Success
import com.obscura.wallpapers.core.data.remote.adapter.PexelsApiAdapter
import com.obscura.wallpapers.core.data.remote.adapter.UnsplashApiAdapter
import com.obscura.wallpapers.core.data.remote.service.ApiService
import com.obscura.wallpapers.core.data.remote.service.LoginRequest
import com.obscura.wallpapers.core.data.remote.service.PexelsApiService
import com.obscura.wallpapers.core.data.remote.service.UnsplashApiService
import com.obscura.wallpapers.core.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiTestViewModel @Inject constructor(
    private val apiService: ApiService,
    private val pexelsApiService: PexelsApiService,
    private val pexelsMapper: PexelsMapper,
    private val unsplashApiService: UnsplashApiService,
    private val unsplashMapper: UnsplashMapper,
    private val billingRepository: BillingRepository,
    private val userRepository: UserRepository,
    private val subscriptionDao: SubscriptionDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ApiTest"
    }

    private val _testResults = MutableStateFlow<List<String>>(emptyList())
    val testResults: StateFlow<List<String>> = _testResults.asStateFlow()

    private val _isTestingPexels = MutableStateFlow(false)
    val isTestingPexels: StateFlow<Boolean> = _isTestingPexels.asStateFlow()

    private val _isTestingUnsplash = MutableStateFlow(false)
    val isTestingUnsplash: StateFlow<Boolean> = _isTestingUnsplash.asStateFlow()

    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage.asStateFlow()

    // 计费相关状态
    val isPremium = billingRepository.isPremiumUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val coinBalance = userRepository.coinBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val availableSubscriptions = billingRepository.availableSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCoins = billingRepository.availableInAppProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openThird = userRepository.openThird
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isTestingSystemApi = MutableStateFlow(false)
    val isTestingSystemApi: StateFlow<Boolean> = _isTestingSystemApi.asStateFlow()

    fun testSystemApis() {
        viewModelScope.launch {
            _isTestingSystemApi.value = true
            try {
                addTestResult("🚀 开始测试系统接口 (含模拟登录)...")

                // 1. 模拟登录获取 Token (如果是基于 Token 的认证)
                try {
                    val loginRequest = LoginRequest(
                        nickname = "TestUser",
                        email = "test@example.com",
                        avatar = "https://example.com/avatar.png",
                        googleToken = "mock_google_token_${System.currentTimeMillis()}",
                        authId = "mock_auth_id_123456",
                        authType = 1, // 1表示Google
                        authToken = "mock_auth_token_${System.currentTimeMillis()}",
                    )
                    val loginResponse = apiService.login(loginRequest)
                    if (loginResponse.isSuccess && loginResponse.data != null) {
                        val token = loginResponse.data.accessToken
                        addTestResult("✅ login 成功: Token=${token.take(10)}...")
                        // 保存 Token 到 UserRepository，触发 AuthInterceptor
                        userRepository.saveServerToken(token)
                        userRepository.updateLoginStatus(true)
                    } else {
                        addTestResult("❌ login 失败: ${loginResponse.apiMsg}")
                    }
                } catch (e: Exception) {
                    addTestResult("❌ login 抛出异常: ${e.message}")
                }

                // 2. 获取用户信息
                try {
                    val profile = apiService.getProfile()
                    logSystemResult("getProfile", profile)
                } catch (e: Exception) {
                    addTestResult("❌ getProfile 抛出异常: ${e.message}")
                }

                // 3. 获取商品列表
                var firstProductId: String? = null
                try {
                    val products = apiService.getProducts(1)
                    logSystemResult("getProducts", products)
                    firstProductId = products.data?.goldGoods?.firstOrNull()?.id.toString()
                } catch (e: Exception) {
                    addTestResult("❌ getProducts 抛出异常: ${e.message}")
                }

                // 4. 获取支付方式 (基于第一个商品)
                if (firstProductId != null) {
                    try {
                        val methods = apiService.getPaymentMethods(firstProductId)
                        logSystemResult("getPaymentMethods", methods)
                        
                        // 5. 尝试下单 (使用第一个支付方式)
                        val firstMethod = methods.data?.firstOrNull()
                        if (firstMethod != null) {
                            val orderRequest = com.obscura.wallpapers.core.data.remote.service.CheckoutRequest(
                                productId = firstProductId.toIntOrNull() ?: 287,
                                payType = firstMethod.payMethodId,
                                channel = firstMethod.id
                            )
                            val orderResponse = apiService.createOrder(orderRequest)
                            logSystemResult("createOrder", orderResponse)
                        }
                    } catch (e: Exception) {
                        addTestResult("❌ 支付/下单流程抛出异常: ${e.message}")
                    }
                }

                // 6. 获取订单列表
                try {
                    val orders = apiService.getOrders()
                    if (orders.isSuccess) {
                        addTestResult("✅ getOrders 成功, 数量: ${orders.rows?.size ?: 0}")
                    } else {
                        addTestResult("❌ getOrders 失败: ${orders.apiMsg}")
                    }
                } catch (e: Exception) {
                    addTestResult("❌ getOrders 抛出异常: ${e.message}")
                }

                _resultMessage.value = "系统完整业务流测试完成"
            } catch (e: Exception) {
                addTestResult("❌ 系统接口测试发生重大错误: ${e.message}")
            } finally {
                _isTestingSystemApi.value = false
            }
        }
    }

    private fun <T> logSystemResult(methodName: String, response: com.obscura.wallpapers.core.data.remote.service.ApiResponse<T>) {
        if (response.isSuccess) {
            addTestResult("✅ $methodName 成功: ${response.data}")
        } else {
            addTestResult("❌ $methodName 失败: ${response.apiMsg}")
        }
    }

    fun testPexelsApi() {
        viewModelScope.launch {
            _isTestingPexels.value = true
            try {
                val pexelsApiAdapter = PexelsApiAdapter(pexelsApiService, pexelsMapper)

                val featuredResult = pexelsApiAdapter.getFeaturedWallpapers(1, 10)
                logApiResult("getFeaturedWallpapers", featuredResult)

                val searchResult = pexelsApiAdapter.searchWallpapers("nature", 1, 10, emptyMap())
                logApiResult("searchWallpapers", searchResult)

                val randomResult = pexelsApiAdapter.getRandomWallpapers(5)
                logApiResult("getRandomWallpapers", randomResult)

                val collectionsResult = pexelsApiAdapter.getCollections(1, 10)
                logApiResult("getCollections", collectionsResult)

                run {
                    val successCollections = collectionsResult as? Success<List<Collection>>
                    if (successCollections != null && successCollections.data.isNotEmpty()) {
                        try {
                            val collectionId = successCollections.data.first().id.split("_")[1]
                            Log.d(TAG, "测试集合ID: $collectionId")
                            val collectionWallpapersResult =
                                pexelsApiAdapter.getWallpapersByCollection(collectionId, 1, 10)
                            logApiResult("getWallpapersByCollection", collectionWallpapersResult)
                        } catch (e: Exception) {
                            Log.e(TAG, "获取集合壁纸失败", e)
                            addTestResult("❌ 获取集合壁纸失败: ${e.message}")
                        }
                    }
                }

                _resultMessage.value = context.getString(R.string.test_pexels_api_complete)
            } catch (e: Exception) {
                Log.e(TAG, "测试过程中发生错误", e)
                _resultMessage.value = context.getString(R.string.test_failed, e.message)
            } finally {
                _isTestingPexels.value = false
            }
        }
    }

    fun testUnsplashApi() {
        viewModelScope.launch {
            _isTestingUnsplash.value = true
            try {
                val unsplashApiAdapter = UnsplashApiAdapter(unsplashApiService, unsplashMapper)

                val featuredResult = unsplashApiAdapter.getFeaturedWallpapers(1, 10)
                logApiResult("getFeaturedWallpapers", featuredResult)

                val searchResult = unsplashApiAdapter.searchWallpapers("nature", 1, 10, emptyMap())
                logApiResult("searchWallpapers", searchResult)

                val randomResult = unsplashApiAdapter.getRandomWallpapers(5)
                logApiResult("getRandomWallpapers", randomResult)

                val collectionsResult = unsplashApiAdapter.getCollections(1, 10)
                logApiResult("getCollections", collectionsResult)

                run {
                    val successCollections = collectionsResult as? Success<List<Collection>>
                    if (successCollections != null && successCollections.data.isNotEmpty()) {
                        try {
                            val collectionId = successCollections.data.first().id.split("_")[1]
                            Log.d(TAG, "测试集合ID: $collectionId")
                            val collectionWallpapersResult =
                                unsplashApiAdapter.getWallpapersByCollection(collectionId, 1, 10)
                            logApiResult("getWallpapersByCollection", collectionWallpapersResult)
                        } catch (e: Exception) {
                            Log.e(TAG, "获取集合壁纸失败", e)
                            addTestResult("❌ 获取集合壁纸失败: ${e.message}")
                        }
                    }
                }

                run {
                    val successFeatured = featuredResult as? Success<List<Wallpaper>>
                    if (successFeatured != null && successFeatured.data.isNotEmpty()) {
                        val wallpaperId = successFeatured.data.first().id.split("_")[1]
                        val trackResult = unsplashApiAdapter.trackDownload(wallpaperId)
                        logApiResult("trackDownload", trackResult)
                    }
                }

                _resultMessage.value = context.getString(R.string.test_unsplash_api_complete)
            } catch (e: Exception) {
                Log.e(TAG, "测试过程中发生错误", e)
                _resultMessage.value = context.getString(R.string.test_failed, e.message)
            } finally {
                _isTestingUnsplash.value = false
            }
        }
    }

    /**
     * 模拟切换订阅状态
     */
    fun toggleMockPremium() {
        viewModelScope.launch {
            val current = isPremium.value
            val mockSub = UserSubscriptionEntity(
                id = 1,
                isPremium = !current,
                subscriptionType = if (!current) ProductType.VIP_GOLD else null,
                purchaseToken = if (!current) "mock_token_${System.currentTimeMillis()}" else null,
                expiryDate = if (!current) System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 else null,
                autoRenewing = !current,
                lastVerified = System.currentTimeMillis()
            )
            subscriptionDao.updateSubscription(mockSub)
            addTestResult(if (!current) "✅ 已开启模拟高级会员" else "ℹ️ 已关闭模拟高级会员")
        }
    }

    /**
     * 模拟增加金币
     */
    fun addMockCoins(amount: Int) {
        viewModelScope.launch {
            val current = userRepository.coinBalance.first()
            userRepository.updateCoinBalance(current + amount)
            addTestResult("💰 模拟充值: +$amount 金币 (当前: ${current + amount})")
        }
    }

    /**
     * 模拟消耗金币
     */
    fun consumeMockCoins(amount: Int) {
        viewModelScope.launch {
            val current = userRepository.coinBalance.first()
            if (current >= amount) {
                userRepository.updateCoinBalance(current - amount)
                addTestResult("🛍️ 模拟消费: -$amount 金币 (剩余: ${current - amount})")
            } else {
                addTestResult("❌ 模拟消费失败: 金币不足")
            }
        }
    }

    /**
     * 真实发起支付测试
     */
    fun testRealPurchase(activity: android.app.Activity, product: ProductDetails) {
        viewModelScope.launch {
            addTestResult("🚀 发起真实支付测试: ${product.name}")
            billingRepository.purchaseProduct(activity, product)
        }
    }

    /**
     * 切换 openThird 开关
     */
    fun toggleOpenThird() {
        viewModelScope.launch {
            val current = openThird.value
            userRepository.saveOpenThird(!current)
            addTestResult(if (!current) "🌐 已开启 openThird (三方支付)" else "🔒 已关闭 openThird (仅 Google Play)")
        }
    }

    /**
     * 模拟完整支付流 (下单 + 支付)
     */
    fun testFullPurchaseFlow(activity: android.app.Activity, isCoin: Boolean) {
        viewModelScope.launch {
            addTestResult("🧪 开始测试${if (isCoin) "金币" else "订阅"}完整支付流...")
            
            // 1. 获取后端产品
            val productsResult = userRepository.getCoinProducts(if (isCoin) 1 else 2)
            Log.d(TAG, "测试后端产品: $productsResult")
            if (productsResult is ApiResult.Success) {
                Log.d(TAG, "测试后端产品: ${productsResult.data}")
                val product = productsResult.data.firstOrNull()
                if (product == null) {
                    addTestResult("❌ 错误: 后端未配置产品")
                    return@launch
                }
                
                addTestResult("📦 选中后端产品: ${product.name} (ID: ${product.id}, SKU: ${product.sku})")

                Log.d(TAG, "测试后端产品: $product")

                // 2. 根据 openThird 逻辑模拟
                if (openThird.value && !product.channels.isNullOrEmpty()) {
                    addTestResult("📝 检测到 openThird=true，列出渠道: ${product.channels.joinToString { it.name ?: it.channel ?: "" }}")
                    // 模拟选择第一个渠道
                    val channel = product.channels.first()
                    addTestResult("🔗 模拟选择渠道: ${channel.name ?: channel.channel}")
                    
                    // 下单
                    val orderResult = userRepository.createOrder(
                        productId = product.id ?: 0,
                        payType = channel.payType ?: 0,
                        channel = channel.channel ?: ""
                    )
                    if (orderResult is ApiResult.Success) {
                        val order = orderResult.data
                        addTestResult("✅ 下单成功: OrderID=${order.id}")
                        if (order.apiIsGooglePay()) {
                             addTestResult("🛒 唤起 Google Play 支付 (SKU: ${product.sku})")
                        } else {
                             addTestResult("🌐 唤起浏览器支付 (URL: ${order.payUrl?.take(20)}...)")
                        }
                    } else {
                        addTestResult("❌ 下单失败: ${(orderResult as? ApiResult.Error)?.message}")
                    }
                } else {
                    addTestResult("📝 检测到 openThird=false，走 Google Play 备退方案")
                    val orderResult = userRepository.createOrder(
                        productId = product.id ?: 0,
                        payType = 1,
                        channel = "1"
                    )
                    if (orderResult is Success) {
                        addTestResult("✅ GooglePlay 下单成功 (ID: 1)")
                        addTestResult("🛒 唤起 Google Play 支付 (SKU: ${product.sku})")
                    } else {
                        addTestResult("❌ GooglePlay 下单失败: ${(orderResult as? ApiResult.Error)?.message}")
                    }
                }
            } else {
                addTestResult("❌ 获取后端产品失败")
            }
        }
    }

    private fun <T> logApiResult(methodName: String, result: ApiResult<T>) {
        when (result) {
            is Success -> {
                val message = "✅ $methodName 成功"
                Log.d(TAG, message)
                when (val data = result.data) {
                    is List<*> -> {
                        val dataMessage = "   返回 ${data.size} 条数据"
                        Log.d(TAG, dataMessage)
                        addTestResult("$message\n$dataMessage")
                    }
                    else -> {
                        val dataMessage = "   返回数据: $data"
                        Log.d(TAG, dataMessage)
                        addTestResult("$message\n$dataMessage")
                    }
                }
            }
            is ApiResult.Error -> {
                val message = "❌ $methodName 失败: ${result.message}"
                Log.e(TAG, message)
                addTestResult(message)
            }
            is ApiResult.Loading -> {
                val message = "⏳ $methodName 加载中"
                Log.d(TAG, message)
                addTestResult(message)
            }
        }
    }

    private fun addTestResult(result: String) {
        val currentResults = _testResults.value.toMutableList()
        currentResults.add(result)
        _testResults.value = currentResults
    }

    fun clearTestResults() {
        _testResults.value = emptyList()
    }

    fun clearResultMessage() {
        _resultMessage.value = null
    }
}
