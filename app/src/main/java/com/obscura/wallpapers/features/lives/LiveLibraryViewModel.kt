package com.obscura.wallpapers.features.lives

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.model.WallpaperCategory
import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.repository.WallpaperRepository
import com.obscura.wallpapers.core.common.RefreshUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveLibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wallpaperRepository: WallpaperRepository,
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
        private const val TAG = "LiveLibraryViewModel"
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
        viewModelScope.launch {
            if (isRefresh) {
                Log.d(TAG, "Refreshing, resetting page to 1")
                _isRefreshing.value = true
                _currentPage.value = 1
                _wallpapers.value = emptyList()
            } else if (_currentPage.value == 1) {
                _wallpapersState.value = UiState.Loading
            } else {
                Log.d(TAG, "Loading more, page: ${_currentPage.value}")
                _isLoadingMore.value = true
            }

            try {
                val result = if (categoryFilter != null) {
                    Log.d(TAG, "Searching videos with category filter: $categoryFilter")

                    val liveType = if (categoryFilter != null) {
                        "live:$categoryFilter"
                    } else {
                        "live"
                    }

                    Log.d(TAG, "Using live type with category: $liveType")
                    wallpaperRepository.getWallpapers(
                        liveType, _currentPage.value, PAGE_SIZE
                    )
                } else {
                    Log.d(TAG, "No category filter, using live API")
                    wallpaperRepository.getWallpapers(
                        "live", _currentPage.value, PAGE_SIZE
                    )
                }

                when (result) {
                    is ApiResult.Success -> {
                        _canLoadMore.value = result.data.size >= PAGE_SIZE

                        val newWallpapers = if (isRefresh || _currentPage.value == 1) {
                            Log.d(TAG, "刷新或首次加载，设置 ${result.data.size} 个壁纸")
                            _wallpapers.value = emptyList()
                            result.data
                        } else {
                            Log.d(TAG, "加载更多，添加 ${result.data.size} 个壁纸，总计 ${_wallpapers.value.size + result.data.size} 个")
                            _wallpapers.value + result.data
                        }

                        _wallpapers.value = newWallpapers
                        _wallpapersState.value = UiState.Success(newWallpapers)

                        Log.d(TAG, "更新壁纸列表和状态，当前页码: ${_currentPage.value}, 壁纸数量: ${newWallpapers.size}")

                        if (!isRefresh && result.data.isNotEmpty()) {
                            _currentPage.value = _currentPage.value + 1
                        }
                    }

                    is ApiResult.Error -> {
                        _wallpapersState.value = UiState.Error(result.message)
                    }

                    is ApiResult.Loading -> {
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during loading: ${e.message}", e)
                _wallpapersState.value =
                    UiState.Error(e.message ?: context.getString(R.string.error_loading_wallpapers))
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

    fun filterByCategory(category: WallpaperCategory) {
        if (_selectedCategory.value == category) return

        _selectedCategory.value = category

        _currentPage.value = 1
        _canLoadMore.value = true
        _isLoadingMore.value = false
        _isRefreshing.value = false

        _wallpapersState.value = UiState.Loading

        Log.d(TAG, "切换到分类: ${category.name}, 重置分页参数")

        val categoryFilter = if (category != WallpaperCategory.ALL) {
            category.apiValue
        } else null
        loadWallpapers(true, categoryFilter)
    }

    fun refresh() {
        Log.d(TAG, "refresh called")
        val currentCategory = _selectedCategory.value
        val categoryFilter = if (currentCategory != WallpaperCategory.ALL) {
            currentCategory.apiValue
        } else null
        loadWallpapers(true, categoryFilter)
    }

    fun loadMore() {
        Log.d(
            TAG,
            "loadMore called, isLoadingMore: ${_isLoadingMore.value}, canLoadMore: ${_canLoadMore.value}"
        )
        if (_isLoadingMore.value || !_canLoadMore.value) {
            Log.d(
                TAG,
                "loadMore aborted due to isLoadingMore: ${_isLoadingMore.value} or !canLoadMore: ${!_canLoadMore.value}"
            )
            return
        }
        Log.d(TAG, "loadMore executing loadWallpapers(false)")
        val currentCategory = _selectedCategory.value
        val categoryFilter = if (currentCategory != WallpaperCategory.ALL) {
            currentCategory.apiValue
        } else null
        loadWallpapers(false, categoryFilter)
    }
}
