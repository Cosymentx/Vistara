package com.obscura.wallpapers.features.discover

/* Moved from features/home to features/discover to align folder with package */

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.common.StringProvider
import com.obscura.wallpapers.core.data.model.Banner
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.model.WallpaperCategory
import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.repository.BannerRepository
import com.obscura.wallpapers.core.data.repository.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val bannerRepository: BannerRepository,
    private val stringProvider: StringProvider
) : ViewModel() {

    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners.asStateFlow()

    private val _featuredWallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val featuredWallpapers: StateFlow<List<Wallpaper>> = _featuredWallpapers.asStateFlow()

    private val _staticWallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val staticWallpapers: StateFlow<List<Wallpaper>> = _staticWallpapers.asStateFlow()

    private val _liveWallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val liveWallpapers: StateFlow<List<Wallpaper>> = _liveWallpapers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            Log.d(TAG, "开始加载初始数据")
            _isLoading.value = true
            _error.value = null

            try {
                try {
                    when (val result = bannerRepository.getHomeBanners()) {
                        is ApiResult.Success -> {
                            _banners.value = result.data
                        }

                        is ApiResult.Error -> {
                            Log.e(TAG, "轮播图数据加载失败: ${result.message}")
                        }

                        is ApiResult.Loading -> {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "轮播图数据加载异常", e)
                }

                try {
                    when (val result = wallpaperRepository.getFeaturedWallpapers(1, 10)) {
                        is ApiResult.Success -> {
                            _featuredWallpapers.value = result.data
                        }

                        is ApiResult.Error -> {
                            Log.e(TAG, "精选壁纸加载失败: ${result.message}")
                        }

                        is ApiResult.Loading -> {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "精选壁纸加载异常", e)
                }

                try {
                    when (val result = wallpaperRepository.getWallpapers("photo", 1, 10)) {
                        is ApiResult.Success -> {
                            _staticWallpapers.value = result.data
                        }

                        is ApiResult.Error -> {
                            Log.e(TAG, "图片壁纸加载失败: ${result.message}")
                        }

                        is ApiResult.Loading -> {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "图片壁纸加载异常", e)
                }

                try {
                    when (val result = wallpaperRepository.getWallpapers("video", 1, 10)) {
                        is ApiResult.Success -> {
                            _liveWallpapers.value = result.data
                        }

                        is ApiResult.Error -> {
                            Log.e(TAG, "视频壁纸加载失败: ${result.message}")
                        }

                        is ApiResult.Loading -> {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "视频壁纸加载异常", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载数据时发生异常", e)
                _error.value = e.message ?: stringProvider.getString(R.string.errors_loading_data)
            } finally {
                _isLoading.value = false
                Log.d(TAG, "数据加载完成")
            }
        }
    }

    companion object {
        private const val TAG = "DiscoverViewModel"
    }

    fun refresh() {
        loadInitialData()
    }

    fun loadMoreWallpapers() {}

    private val _selectedCategory = MutableStateFlow<WallpaperCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _categoryWallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val categoryWallpapers = _categoryWallpapers.asStateFlow()

    private val _isCategoryLoading = MutableStateFlow(false)
    val isCategoryLoading = _isCategoryLoading.asStateFlow()

    fun loadWallpapersByCategory(category: WallpaperCategory) {
        _selectedCategory.value = category
        viewModelScope.launch {
            _isCategoryLoading.value = true
            try {
                val categoryId =
                    if (category == WallpaperCategory.ALL) "all" else "unsplash_${category.apiValue}"
                when (val result = wallpaperRepository.getWallpapersByCategory(categoryId, 1, 10)) {
                    is ApiResult.Success -> {
                        _categoryWallpapers.value = result.data
                    }

                    is ApiResult.Error -> {
                        _error.value = result.message
                    }

                    is ApiResult.Loading -> {}
                }
            } catch (e: Exception) {
                _error.value = e.message
                    ?: stringProvider.getString(R.string.errors_loading_category_wallpapers)
            } finally {
                _isCategoryLoading.value = false
            }
        }
    }

    fun loadWallpapersByCategory(category: String) {
        val categoryEnum =
            WallpaperCategory.values().find { it.apiValue.equals(category, ignoreCase = true) }
                ?: WallpaperCategory.ALL
        loadWallpapersByCategory(categoryEnum)
    }
}
