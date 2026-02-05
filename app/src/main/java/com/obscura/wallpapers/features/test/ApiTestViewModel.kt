package com.obscura.wallpapers.features.test

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.R
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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiTestViewModel @Inject constructor(
    private val pexelsApiService: PexelsApiService,
    private val pexelsMapper: PexelsMapper,
    private val unsplashApiService: UnsplashApiService,
    private val unsplashMapper: UnsplashMapper,
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

                _resultMessage.value = context.getString(R.string.pexels_api_test_complete)
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

                _resultMessage.value = context.getString(R.string.unsplash_api_test_complete)
            } catch (e: Exception) {
                Log.e(TAG, "测试过程中发生错误", e)
                _resultMessage.value = context.getString(R.string.test_failed, e.message)
            } finally {
                _isTestingUnsplash.value = false
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
