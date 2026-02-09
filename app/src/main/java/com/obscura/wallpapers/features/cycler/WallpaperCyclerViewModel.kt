package com.obscura.wallpapers.features.cycler

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.obscura.wallpapers.core.data.model.AutoChangeFrequency
import com.obscura.wallpapers.core.data.model.AutoChangeHistory
import com.obscura.wallpapers.core.data.model.AutoChangeSource
import com.obscura.wallpapers.core.data.model.Category
import com.obscura.wallpapers.core.data.model.WallpaperTarget
import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.core.data.repository.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WallpaperCyclerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefsRepository: UserPrefsRepository,
    private val userRepository: UserRepository,
    private val wallpaperRepository: WallpaperRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "WallpaperCyclerViewModel"
    }

    private val _autoChangeEnabled = MutableStateFlow(false)
    val autoChangeEnabled: StateFlow<Boolean> = _autoChangeEnabled.asStateFlow()

    private val _autoChangeFrequency = MutableStateFlow(AutoChangeFrequency.DAILY)
    val autoChangeFrequency: StateFlow<AutoChangeFrequency> = _autoChangeFrequency.asStateFlow()

    private val _autoChangeWifiOnly = MutableStateFlow(true)
    val autoChangeWifiOnly: StateFlow<Boolean> = _autoChangeWifiOnly.asStateFlow()

    private val _autoChangeSource = MutableStateFlow(AutoChangeSource.FAVORITES)
    val autoChangeSource: StateFlow<AutoChangeSource> = _autoChangeSource.asStateFlow()

    private val _autoChangeCategory = MutableStateFlow<String?>(null)
    val autoChangeCategory: StateFlow<String?> = _autoChangeCategory.asStateFlow()

    private val _autoChangeTarget = MutableStateFlow(WallpaperTarget.BOTH)
    val autoChangeTarget: StateFlow<WallpaperTarget> = _autoChangeTarget.asStateFlow()

    private val _availableCategories = MutableStateFlow<List<Category>>(emptyList())
    val availableCategories: StateFlow<List<Category>> = _availableCategories.asStateFlow()

    private val _autoChangeHistory = MutableStateFlow<List<AutoChangeHistory>>(emptyList())
    val autoChangeHistory: StateFlow<List<AutoChangeHistory>> = _autoChangeHistory.asStateFlow()

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _needLogin = MutableStateFlow(false)
    val needLogin: StateFlow<Boolean> = _needLogin.asStateFlow()

    private val _isChangingWallpaper = MutableStateFlow(false)
    val isChangingWallpaper: StateFlow<Boolean> = _isChangingWallpaper.asStateFlow()

    private val _settingsApplied = MutableStateFlow(false)
    val settingsApplied: StateFlow<Boolean> = _settingsApplied.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                _isLoggedIn.value = userRepository.checkUserLoggedIn()
                if (_isLoggedIn.value) {
                    loadSettings()
                    checkPremiumStatus()
                    loadCategories()
                    loadAutoChangeHistory()
                } else {
                    _needLogin.value = true
                }
            } catch (e: Exception) {
                _isLoggedIn.value = false
                _needLogin.value = true
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val userSettings = userPrefsRepository.getUserSettings()
                _autoChangeEnabled.value = userSettings.autoChangeEnabled
                _autoChangeFrequency.value = userSettings.autoChangeFrequency
                _autoChangeWifiOnly.value = userSettings.autoChangeWifiOnly
                _autoChangeSource.value = userSettings.autoChangeSource
                _autoChangeCategory.value = userSettings.autoChangeCategory
                _autoChangeTarget.value = userSettings.autoChangeTarget ?: WallpaperTarget.BOTH
            } catch (_: Exception) {
            }
        }
    }

    private fun checkPremiumStatus() {
        viewModelScope.launch {
            try {
                val isPremium = userRepository.isPremiumUser.first()
                _isPremiumUser.value = isPremium
                if (!isPremium) {
                    if (_autoChangeFrequency.value.isPremium) {
                        _autoChangeFrequency.value = AutoChangeFrequency.DAILY
                    }
                    if (_autoChangeSource.value.isPremium) {
                        _autoChangeSource.value = AutoChangeSource.FAVORITES
                    }
                }
            } catch (_: Exception) {
                _isPremiumUser.value = false
            }
        }
    }

    fun clearNeedLogin() {
        _needLogin.value = false
    }

    fun updateAutoChangeEnabled(enabled: Boolean) {
        if (!_isLoggedIn.value) {
            _needLogin.value = true
            return
        }
        _autoChangeEnabled.value = enabled
        saveSettings()
    }

    fun updateAutoChangeFrequency(frequency: AutoChangeFrequency) {
        if (!_isLoggedIn.value) {
            _needLogin.value = true
            return
        }
        _autoChangeFrequency.value = frequency
        saveSettings()
    }

    fun updateAutoChangeWifiOnly(wifiOnly: Boolean) {
        if (!_isLoggedIn.value) {
            _needLogin.value = true
            return
        }
        _autoChangeWifiOnly.value = wifiOnly
        saveSettings()
    }

    fun updateAutoChangeSource(source: AutoChangeSource) {
        if (!_isLoggedIn.value) {
            _needLogin.value = true
            return
        }
        _autoChangeSource.value = source
        saveSettings()
    }

    fun updateAutoChangeTarget(target: WallpaperTarget) {
        if (!_isLoggedIn.value) {
            _needLogin.value = true
            return
        }
        _autoChangeTarget.value = target
        saveSettings()
    }

    private fun saveSettings() {
        viewModelScope.launch {
            try {
                userPrefsRepository.updateAutoChangeSettings(
                    enabled = _autoChangeEnabled.value,
                    frequency = _autoChangeFrequency.value,
                    wifiOnly = _autoChangeWifiOnly.value,
                    source = _autoChangeSource.value,
                    categoryId = _autoChangeCategory.value
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = wallpaperRepository.getCategories()
                _availableCategories.value = categories
            } catch (_: Exception) {
                _availableCategories.value = emptyList()
            }
        }
    }

    private fun loadAutoChangeHistory() {
        viewModelScope.launch {
            try {
                wallpaperRepository.getAutoChangeHistory().collect { history ->
                    _autoChangeHistory.value = history
                }
            } catch (_: Exception) {
                _autoChangeHistory.value = emptyList()
            }
        }
    }

    fun applyAutoChangeSettings() {
        viewModelScope.launch {
            try {
                saveSettings()
                scheduleAutoChangeIfNeeded()
                _settingsApplied.value = true
            } catch (_: Exception) {
                _settingsApplied.value = false
            }
        }
    }

    private fun scheduleAutoChangeIfNeeded() {
        WorkManager.getInstance(context)
    }

    fun testUnlockBroadcast(context: Context) {
        _isChangingWallpaper.value = true
        val intent = Intent("android.intent.action.USER_PRESENT")
        context.sendBroadcast(intent)
        _isChangingWallpaper.value = false
    }
}
