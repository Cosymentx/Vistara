package com.obscura.wallpapers.features.statics

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.model.WallpaperCategory
import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.remote.ApiUsageTracker
import com.obscura.wallpapers.core.data.repository.WallpaperRepository
import com.obscura.wallpapers.core.common.RefreshUtil
import com.obscura.wallpapers.core.data.remote.ApiResult.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaticLibraryViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val apiUsageTracker: ApiUsageTracker,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    private val _wallpapersState = MutableStateFlow<UiState<List<Wallpaper>>>(UiState.Loading)
    val wallpapersState: StateFlow<UiState<List<Wallpaper>>> = _wallpapersState.asStateFlow()

    val categories = WallpaperCategory.getAllCategories()

    private val _selectedCategory = MutableStateFlow(WallpaperCategory.ALL)
    val selectedCategory: StateFlow<WallpaperCategory> = _selectedCategory.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _wallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())

    init {
        loadWallpapers()
    }

    fun loadWallpapers(isRefresh: Boolean = false, categoryFilter: String? = null) {
        if (_isLoadingMore.value || _isRefreshing.value) {
            Log.d("StaticLibraryViewModel", "正在加载中，跳过请求")
            return
        }

        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
                _currentPage.value = 1
                _wallpapers.value = emptyList()
                Log.d("StaticLibraryViewModel", "刷新壁纸数据，重置到第1页")
            } else if (_currentPage.value == 1) {
                _wallpapersState.value = UiState.Loading
                Log.d("StaticLibraryViewModel", "首次加载壁纸数据，第1页")
            } else {
                _isLoadingMore.value = true
                Log.d("StaticLibraryViewModel", "加载更多壁纸，当前页码: ${_currentPage.value}")
            }

            try {
                val result = if (categoryFilter != null && categoryFilter != "全部") {
                    val apiSources = listOf("unsplash", "pexels")
                    val apiSource = apiSources[categoryFilter.length % apiSources.size]
                    val categoryId = "${apiSource}_${categoryFilter.lowercase()}"

                    Log.d("StaticLibraryViewModel", "使用分类ID: $categoryId 用于筛选: $categoryFilter")

                    if (isRefresh || _currentPage.value == 1) {
                        try {
                            val categoryResult = wallpaperRepository.getWallpapersByCategory(
                                categoryId, _currentPage.value, PAGE_SIZE
                            )

                            if (categoryResult is Success && categoryResult.data.isNotEmpty()) {
                                Log.d("StaticLibraryViewModel", "使用 $apiSource 获取分类成功，返回 ${categoryResult.data.size} 个壁纸")
                                categoryResult
                            } else {
                                Log.w("StaticLibraryViewModel", "使用 $apiSource 获取分类失败或没有数据，尝试使用默认API")
                                wallpaperRepository.getWallpapers(
                                    "static", _currentPage.value, PAGE_SIZE
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("StaticLibraryViewModel", "获取分类壁纸异常: ${e.message}")
                            wallpaperRepository.getWallpapers(
                                "static", _currentPage.value, PAGE_SIZE
                            )
                        }
                    } else {
                        try {
                            wallpaperRepository.getWallpapersByCategory(
                                categoryId, _currentPage.value, PAGE_SIZE
                            )
                        } catch (e: Exception) {
                            Log.e("StaticLibraryViewModel", "加载更多分类壁纸异常: ${e.message}")
                            Success(emptyList())
                        }
                    }
                } else {
                    Log.d("StaticLibraryViewModel", "使用默认壁纸获取方法")
                    wallpaperRepository.getWallpapers(
                        "static", _currentPage.value, PAGE_SIZE
                    )
                }

                when (result) {
                    is Success -> {
                        _canLoadMore.value = result.data.size >= PAGE_SIZE

                        val newWallpapers = if (isRefresh || _currentPage.value == 1) {
                            Log.d("StaticLibraryViewModel", "刷新或首次加载，设置 ${result.data.size} 个壁纸")
                            _wallpapers.value = emptyList()
                            result.data
                        } else {
                            Log.d("StaticLibraryViewModel", "加载更多，添加 ${result.data.size} 个壁纸，总计 ${_wallpapers.value.size + result.data.size} 个")
                            _wallpapers.value + result.data
                        }

                        _wallpapers.value = newWallpapers
                        _wallpapersState.value = UiState.Success(newWallpapers)

                        Log.d("StaticLibraryViewModel", "更新壁纸列表和状态，当前页码: ${_currentPage.value}, 壁纸数量: ${newWallpapers.size}")

                        if (!isRefresh && result.data.isNotEmpty()) {
                            val nextPage = _currentPage.value + 1
                            Log.d("StaticLibraryViewModel", "增加页码从 ${_currentPage.value} 到 $nextPage")
                            _currentPage.value = nextPage
                        }
                    }

                    is ApiResult.Error -> {
                        _wallpapersState.value = UiState.Error(result.message)
                    }

                    is ApiResult.Loading -> {
                    }
                }
            } catch (e: Exception) {
                _wallpapersState.value = UiState.Error(e.message ?: "加载壁纸失败")
            } finally {
                if (isRefresh) {
                    RefreshUtil.delayedEndRefreshing(_isRefreshing, viewModelScope)
                } else {
                    _isRefreshing.value = false
                }
                _isLoadingMore.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !_canLoadMore.value) {
            Log.d(
                "StaticLibraryViewModel",
                "loadMore aborted due to isLoadingMore: ${_isLoadingMore.value} or !canLoadMore: ${!_canLoadMore.value}"
            )
            return
        }

        Log.d("StaticLibraryViewModel", "加载更多壁纸，当前页码: ${_currentPage.value}")

        val currentCategory = _selectedCategory.value
        val categoryFilter = if (currentCategory != WallpaperCategory.ALL) {
            currentCategory.apiValue
        } else null
        loadWallpapers(false, categoryFilter)
    }

    fun filterByCategory(category: WallpaperCategory) {
        if (_selectedCategory.value == category) return

        _selectedCategory.value = category

        _currentPage.value = 1
        _canLoadMore.value = true
        _isLoadingMore.value = false
        _isRefreshing.value = false

        _wallpapersState.value = UiState.Loading

        Log.d("StaticLibraryViewModel", "切换到分类: ${category.name}, 重置分页参数")

        if (category == WallpaperCategory.ALL) {
            Log.d("StaticLibraryViewModel", "切换到全部分类，使用默认数据源")
            loadWallpapers(true, null)
            return
        }

        val categoryFilter = category.apiValue
        Log.d("StaticLibraryViewModel", "切换到分类: $categoryFilter")
        loadWallpapers(true, categoryFilter)
    }

    fun filterByCategory(categoryResId: Int) {
        val category = when (categoryResId) {
            R.string.category_all -> WallpaperCategory.ALL
            R.string.category_nature -> WallpaperCategory.NATURE
            R.string.category_city -> WallpaperCategory.CITY
            R.string.category_abstract -> WallpaperCategory.ABSTRACT
            R.string.category_minimal -> WallpaperCategory.MINIMAL
            R.string.category_animals -> WallpaperCategory.ANIMALS
            R.string.category_food -> WallpaperCategory.FOOD
            R.string.category_architecture -> WallpaperCategory.ARCHITECTURE
            R.string.category_art -> WallpaperCategory.ART
            R.string.category_space -> WallpaperCategory.SPACE
            R.string.category_illustration -> WallpaperCategory.ILLUSTRATION
            R.string.category_technology -> WallpaperCategory.TECHNOLOGY
            else -> WallpaperCategory.ALL
        }

        filterByCategory(category)
    }

    fun refresh() {
        apiUsageTracker.resetAllRateLimits()
        Log.d("StaticLibraryViewModel", "已重置所有API速率限制")

        apiUsageTracker.resetAllStats()
        Log.d("StaticLibraryViewModel", "已重置所有API统计数据")

        _currentPage.value = 1
        _canLoadMore.value = true
        _isLoadingMore.value = false
        _isRefreshing.value = true
        _wallpapers.value = emptyList()
        _wallpapersState.value = UiState.Loading

        Log.d("StaticLibraryViewModel", "刷新壁纸数据，重置分页参数")

        val currentCategory = _selectedCategory.value
        val categoryFilter = if (currentCategory != WallpaperCategory.ALL) {
            currentCategory.apiValue
        } else null

        loadWallpapers(true, categoryFilter)
    }
}
