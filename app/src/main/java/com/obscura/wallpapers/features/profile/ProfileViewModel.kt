package com.obscura.wallpapers.features.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.BuildConfig
import com.obscura.wallpapers.core.data.repository.AuthRepository
import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import com.obscura.wallpapers.core.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPrefsRepository: UserPrefsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _username = MutableStateFlow("Obscura User")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _userPhotoUrl = MutableStateFlow<String?>(null)
    val userPhotoUrl: StateFlow<String?> = _userPhotoUrl.asStateFlow()

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    val coinBalance: StateFlow<Int> = userRepository.coinBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isDebugMode = MutableStateFlow(false)
    val isDebugMode: StateFlow<Boolean> = _isDebugMode.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _needLoginAction = MutableStateFlow<LoginAction?>(null)
    val needLoginAction: StateFlow<LoginAction?> = _needLoginAction.asStateFlow()

    init {
        loadUserData()
        checkDebugMode()
    }

    fun refreshUserData() {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                _isLoggedIn.value = userRepository.checkUserLoggedIn()
                val isPremium = userRepository.isPremiumUser.first()
                _isPremiumUser.value = isPremium
                if (_isLoggedIn.value) {
                    val name = authRepository.userName.first()
                    if (!name.isNullOrEmpty()) _username.value = name
                    val photoUrl = authRepository.userPhotoUrl.first()
                    _userPhotoUrl.value = photoUrl

//                    try {
//                        userRepository.refreshUserProfile()
//                    } catch (e: Exception) {
//                        Log.e("ProfileViewModel", "刷新用户个人资料失败: ${e.message}", e)
//                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading user data: ${e.message}")
            }
        }
    }

    private fun checkDebugMode() {
        _isDebugMode.value = BuildConfig.DEBUG
    }

    enum class LoginAction { FAVORITES, DOWNLOADS, AUTO_WALLPAPER }

    fun clearNeedLoginAction() { _needLoginAction.value = null }
    fun setNeedLoginAction(action: LoginAction) { _needLoginAction.value = action }

    fun checkLoginAndExecute(action: LoginAction, onLoggedIn: () -> Unit): Boolean {
        return if (_isLoggedIn.value) { onLoggedIn(); true } else { _needLoginAction.value = action; false }
    }

    fun upgradeToPremium() {
        viewModelScope.launch {
            try {
                userRepository.updatePremiumStatus(true)
                _isPremiumUser.value = true
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error upgrading to premium: ${e.message}")
            }
        }
    }

    fun cancelPremium() {
        viewModelScope.launch {
            try {
                userRepository.updatePremiumStatus(false)
                _isPremiumUser.value = false
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error cancelling premium: ${e.message}")
            }
        }
    }

    fun toggleDebugMode() {
        _isDebugMode.value = !_isDebugMode.value
    }

    fun clearUserData() {
        viewModelScope.launch {
            try {
                userRepository.clearUserData()
                userPrefsRepository.clearUserSettings()
                _isPremiumUser.value = false
                loadUserData()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error clearing user data: ${e.message}")
            }
        }
    }
}
