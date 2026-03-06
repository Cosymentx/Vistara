package com.obscura.wallpapers.features.photo

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoLibraryViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val apiUsageTracker: ApiUsageTracker,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    private var loadJob: Job? = null

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
    val wallpapers: StateFlow<List<Wallpaper>> = _wallpapers.asStateFlow()

    // 缓存各分类的状态
    private val categoryCache = mutableMapOf<WallpaperCategory, CategoryState>()

    private data class CategoryState(
        val wallpapers: List<Wallpaper>,
        val currentPage: Int,
        val canLoadMore: Boolean
    )

    init {
        loadWallpapers()
    }

    fun loadWallpapers(isRefresh: Boolean = false, categoryFilter: String? = null) {
        if (!isRefresh && (_isLoadingMore.value || _isRefreshing.value)) {
            Log.d("PhotoLibraryViewModel", "正在加载中，跳过请求")
            return
        }

        if (isRefresh || _currentPage.value == 1) {
            loadJob?.cancel()
        }

        loadJob = viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
                _currentPage.value = 1
                _wallpapers.value = emptyList()
                Log.d("PhotoLibraryViewModel", "刷新壁纸数据，重置到第1页")
            } else if (_currentPage.value == 1) {
                _wallpapersState.value = UiState.Loading
                Log.d("PhotoLibraryViewModel", "首次加载壁纸数据，第1页")
            } else {
                _isLoadingMore.value = true
                Log.d("PhotoLibraryViewModel", "加载更多壁纸，当前页码: ${_currentPage.value}")
            }

            try {
                val result = if (categoryFilter != null && categoryFilter != "全部") {
                    val apiSources = listOf("unsplash", "pexels")
                    val apiSource = apiSources[categoryFilter.length % apiSources.size]
                    val categoryId = "${apiSource}_${categoryFilter.lowercase()}"

                    Log.d("PhotoLibraryViewModel", "使用分类ID: $categoryId 用于筛选: $categoryFilter")

                    if (isRefresh || _currentPage.value == 1) {
                        try {
                            val categoryResult = wallpaperRepository.getWallpapersByCategory(
                                categoryId, _currentPage.value, PAGE_SIZE
                            )

                            if (categoryResult is Success && categoryResult.data.isNotEmpty()) {
                                Log.d("PhotoLibraryViewModel", "使用 $apiSource 获取分类成功，返回 ${categoryResult.data.size} 个壁纸")
                                categoryResult
                            } else {
                                Log.w("PhotoLibraryViewModel", "使用 $apiSource 获取分类失败或没有数据，尝试使用默认API")
                                wallpaperRepository.getWallpapers(
                                    "photo", _currentPage.value, PAGE_SIZE
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("PhotoLibraryViewModel", "获取分类壁纸异常: ${e.message}")
                            wallpaperRepository.getWallpapers(
                                "photo", _currentPage.value, PAGE_SIZE
                            )
                        }
                    } else {
                        try {
                            wallpaperRepository.getWallpapersByCategory(
                                categoryId, _currentPage.value, PAGE_SIZE
                            )
                        } catch (e: Exception) {
                            Log.e("PhotoLibraryViewModel", "加载更多分类壁纸异常: ${e.message}")
                            Success(emptyList())
                        }
                    }
                } else {
                    Log.d("PhotoLibraryViewModel", "使用默认壁纸获取方法")
                    wallpaperRepository.getWallpapers(
                        "photo", _currentPage.value, PAGE_SIZE
                    )
                }

                when (result) {
                    is Success -> {
                        // 允许加载更多，只要本次返回了数据（考虑到组合数据源可能由于单方受限导致总量不足PAGE_SIZE）
                        _canLoadMore.value = result.data.isNotEmpty()

                        val newWallpapers = if (isRefresh || _currentPage.value == 1) {
                            Log.d("PhotoLibraryViewModel", "刷新或首次加载，设置 ${result.data.size} 个壁纸")
                            result.data.distinctBy { it.id }
                        } else {
                            Log.d("PhotoLibraryViewModel", "加载更多，添加 ${result.data.size} 个壁纸，总计 ${_wallpapers.value.size + result.data.size} 个")
                            (_wallpapers.value + result.data).distinctBy { it.id }
                        }

                        _wallpapers.value = newWallpapers
                        _wallpapersState.value = UiState.Success(newWallpapers)

                        Log.d("PhotoLibraryViewModel", "更新壁纸列表和状态，当前页码: ${_currentPage.value}, 壁纸数量: ${newWallpapers.size}")

                        if (result.data.isNotEmpty()) {
                            val nextPage = _currentPage.value + 1
                            Log.d("PhotoLibraryViewModel", "增加页码从 ${_currentPage.value} 到 $nextPage")
                            _currentPage.value = nextPage
                        }
                    }

                    is ApiResult.Error -> {
                        if (_wallpapers.value.isEmpty()) {
                            _wallpapersState.value = UiState.Error(result.message)
                        } else {
                            // 如果已经有数据，不进入全局错误状态，仅在日志记录
                            Log.e("PhotoLibraryViewModel", "加载更多失败: ${result.message}")
                            _wallpapersState.value = UiState.Success(_wallpapers.value)
                        }
                    }

                    is ApiResult.Loading -> {
                    }
                }
            } catch (e: Exception) {
                if (_wallpapers.value.isEmpty()) {
                    _wallpapersState.value = UiState.Error(e.message ?: context.getString(R.string.errors_loading_wallpapers))
                } else {
                    Log.e("PhotoLibraryViewModel", "加载更多异常: ${e.message}")
                    _wallpapersState.value = UiState.Success(_wallpapers.value)
                }
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
                "PhotoLibraryViewModel",
                "loadMore aborted due to isLoadingMore: ${_isLoadingMore.value} or !canLoadMore: ${!_canLoadMore.value}"
            )
            return
        }

        Log.d("PhotoLibraryViewModel", "加载更多壁纸，当前页码: ${_currentPage.value}")

        val currentCategory = _selectedCategory.value
        val categoryFilter = if (currentCategory != WallpaperCategory.ALL) {
            currentCategory.apiValue
        } else null
        loadWallpapers(false, categoryFilter)
    }

    fun filterByCategory(category: WallpaperCategory) {
        if (_selectedCategory.value == category) return

        // 切换前保存当前分类的状态
        val oldCategory = _selectedCategory.value
        categoryCache[oldCategory] = CategoryState(
            wallpapers = _wallpapers.value,
            currentPage = _currentPage.value,
            canLoadMore = _canLoadMore.value
        )

        _selectedCategory.value = category

        // 检查是否有缓存
        val cached = categoryCache[category]
        if (cached != null && cached.wallpapers.isNotEmpty()) {
            Log.d("PhotoLibraryViewModel", "从缓存恢复分类: ${category.name}, 壁纸数量: ${cached.wallpapers.size}")
            _wallpapers.value = cached.wallpapers
            _currentPage.value = cached.currentPage
            _canLoadMore.value = cached.canLoadMore
            _wallpapersState.value = UiState.Success(cached.wallpapers)
            return
        }

        _currentPage.value = 1
        _canLoadMore.value = true
        _isLoadingMore.value = false
        _isRefreshing.value = false

        _wallpapersState.value = UiState.Loading

        Log.d("PhotoLibraryViewModel", "切换到分类: ${category.name}, 重置分页参数")

        if (category == WallpaperCategory.ALL) {
            Log.d("PhotoLibraryViewModel", "切换到全部分类，使用默认数据源")
            loadWallpapers(true, null)
            return
        }

        val categoryFilter = category.apiValue
        Log.d("PhotoLibraryViewModel", "切换到分类: $categoryFilter")
        loadWallpapers(true, categoryFilter)
    }

    fun filterByCategory(categoryResId: Int) {
        val category = when (categoryResId) {
            R.string.categories_all -> WallpaperCategory.ALL
            R.string.categories_nature -> WallpaperCategory.NATURE
            R.string.categories_city -> WallpaperCategory.CITY
            R.string.categories_abstract -> WallpaperCategory.ABSTRACT
            R.string.categories_minimal -> WallpaperCategory.MINIMAL
            R.string.categories_animals -> WallpaperCategory.ANIMALS
            R.string.categories_food -> WallpaperCategory.FOOD
            R.string.categories_architecture -> WallpaperCategory.ARCHITECTURE
            R.string.categories_art -> WallpaperCategory.ART
            R.string.categories_space -> WallpaperCategory.SPACE
            R.string.categories_illustration -> WallpaperCategory.ILLUSTRATION
            R.string.categories_technology -> WallpaperCategory.TECHNOLOGY
            else -> WallpaperCategory.ALL
        }

        filterByCategory(category)
    }

    fun refresh() {
        apiUsageTracker.resetAllRateLimits()
        Log.d("PhotoLibraryViewModel", "已重置所有API速率限制")

        apiUsageTracker.resetAllStats()
        Log.d("PhotoLibraryViewModel", "已重置所有API统计数据")

        // 刷新时清空所有缓存，强制重新加载
        categoryCache.clear()

        _currentPage.value = 1
        _canLoadMore.value = true
        _isLoadingMore.value = false
        _isRefreshing.value = true
        _wallpapers.value = emptyList()
        _wallpapersState.value = UiState.Loading

        Log.d("PhotoLibraryViewModel", "刷新壁纸数据，重置分页参数")

        val currentCategory = _selectedCategory.value
        val categoryFilter = if (currentCategory != WallpaperCategory.ALL) {
            currentCategory.apiValue
        } else null

        loadWallpapers(true, categoryFilter)
    }
}
