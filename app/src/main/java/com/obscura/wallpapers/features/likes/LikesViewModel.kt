package com.obscura.wallpapers.features.likes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.core.data.repository.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LikesViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "LikesViewModel"
    }

    private val _favoritesState = MutableStateFlow<UiState<List<Wallpaper>>>(UiState.Loading)
    val favoritesState: StateFlow<UiState<List<Wallpaper>>> = _favoritesState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                _isLoggedIn.value = userRepository.checkUserLoggedIn()
                if (_isLoggedIn.value) {
                    loadFavorites()
                } else {
                    _favoritesState.value = UiState.Error("需要登录才能查看收藏列表")
                }
            } catch (_: Exception) {
                _isLoggedIn.value = false
                _favoritesState.value = UiState.Error("检查登录状态失败")
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _favoritesState.value = UiState.Loading
            wallpaperRepository.getFavoriteWallpapers()
                .catch { e ->
                    _favoritesState.value = UiState.Error(e.message ?: "加载收藏壁纸失败")
                }
                .collectLatest { wallpapers ->
                    if (wallpapers.isEmpty()) {
                        _favoritesState.value = UiState.Success(emptyList())
                    } else {
                        _favoritesState.value = UiState.Success(wallpapers)
                    }
                }
        }
    }

    fun unfavoriteWallpaper(wallpaperId: String) {
        viewModelScope.launch {
            val success = wallpaperRepository.unfavoriteWallpaper(wallpaperId)
            if (success) {
            }
        }
    }

    fun refresh() {
        checkLoginStatus()
    }
}
