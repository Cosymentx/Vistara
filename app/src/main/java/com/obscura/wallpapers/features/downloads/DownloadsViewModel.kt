package com.obscura.wallpapers.features.downloads

import android.util.Log
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
class DownloadsViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DownloadsViewModel"
    }

    private val _downloadsState = MutableStateFlow<UiState<List<Wallpaper>>>(UiState.Loading)
    val downloadsState: StateFlow<UiState<List<Wallpaper>>> = _downloadsState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        checkLoginStatus()
    }

    fun refresh() {
        if (_isLoggedIn.value) {
            loadDownloads()
        } else {
            checkLoginStatus()
        }
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                _isLoggedIn.value = userRepository.checkUserLoggedIn()
                Log.d(TAG, "User login status: ${_isLoggedIn.value}")

                if (_isLoggedIn.value) {
                    loadDownloads()
                } else {
                    _downloadsState.value = UiState.Error("需要登录才能查看下载列表")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking login status", e)
                _isLoggedIn.value = false
                _downloadsState.value = UiState.Error("检查登录状态失败")
            }
        }
    }

    private fun loadDownloads() {
        viewModelScope.launch {
            _downloadsState.value = UiState.Loading

            wallpaperRepository.getDownloadedWallpapers()
                .catch { e ->
                    _downloadsState.value = UiState.Error(e.message ?: "加载下载壁纸失败")
                }
                .collectLatest { wallpapers ->
                    if (wallpapers.isEmpty()) {
                        _downloadsState.value = UiState.Success(emptyList())
                    } else {
                        _downloadsState.value = UiState.Success(wallpapers)
                    }
                }
        }
    }
}
