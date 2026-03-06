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
                subscriptionType = if (!current) ProductType.PREMIUM_MONTHLY else null,
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
