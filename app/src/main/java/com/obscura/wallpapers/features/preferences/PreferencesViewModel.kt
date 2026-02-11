package com.obscura.wallpapers.features.preferences

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.AppLanguage
import com.obscura.wallpapers.core.data.repository.AuthRepository
import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.settings.LocaleManager
import com.obscura.wallpapers.core.common.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsToggleEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String?,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefsRepository: UserPrefsRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val localeManager: LocaleManager,
    private val stringProvider: StringProvider
) : ViewModel() {
    companion object { private const val TAG = "PreferencesViewModel" }

    enum class NotificationType { DOWNLOAD, WALLPAPER_CHANGE }
    enum class LoginAction { SIGN_OUT }

    private val _darkTheme = MutableStateFlow(false)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()
    private val _dynamicColors = MutableStateFlow(true)
    val dynamicColors: StateFlow<Boolean> = _dynamicColors.asStateFlow()
    private val _appLanguage = MutableStateFlow(AppLanguage.SYSTEM)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()
    private val _showDownloadNotification = MutableStateFlow(true)
    val showDownloadNotification: StateFlow<Boolean> = _showDownloadNotification.asStateFlow()
    private val _showWallpaperChangeNotification = MutableStateFlow(true)
    val showWallpaperChangeNotification: StateFlow<Boolean> = _showWallpaperChangeNotification.asStateFlow()
    private val _downloadOriginalQuality = MutableStateFlow(true)
    val downloadOriginalQuality: StateFlow<Boolean> = _downloadOriginalQuality.asStateFlow()
    private val _cacheSize = MutableStateFlow("0 MB")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()
    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache.asStateFlow()
    private val _appVersion = MutableStateFlow("1.0.0")
    val appVersion: StateFlow<String> = _appVersion.asStateFlow()
    private val _needNotificationPermission = MutableStateFlow(false)
    val needNotificationPermission: StateFlow<Boolean> = _needNotificationPermission.asStateFlow()
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()
    private val _needLoginAction = MutableStateFlow<LoginAction?>(null)
    val needLoginAction: StateFlow<LoginAction?> = _needLoginAction.asStateFlow()
    private val _operationResult = MutableStateFlow<String?>(null)
    val operationResult: StateFlow<String?> = _operationResult.asStateFlow()
    private var pendingEnableDownloadNotification: Boolean? = null
    private var pendingEnableWallpaperNotification: Boolean? = null

    init {
        loadUserSettings()
        calculateCacheSize()
        loadAppVersion()
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                _isLoggedIn.value = userRepository.checkUserLoggedIn()
                Log.d(TAG, "Login status: ${_isLoggedIn.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking login status: ${e.message}")
                _isLoggedIn.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                _isLoggingOut.value = true
                authRepository.signOut()
                _isLoggedIn.value = false
                _operationResult.value = context.getString(R.string.settings_sign_out_success)
            } catch (e: Exception) {
                Log.e(TAG, "Error signing out: ${e.message}")
                _operationResult.value = context.getString(R.string.settings_sign_out_failed)
            } finally {
                _isLoggingOut.value = false
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private suspend fun saveSettings(update: (com.obscura.wallpapers.core.data.model.UserSettings) -> com.obscura.wallpapers.core.data.model.UserSettings) {
        val current = userPrefsRepository.getUserSettings()
        userPrefsRepository.saveUserSettings(update(current))
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format("%.2f MB", mb)
    }
    private fun directorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isFile) file.length() else directorySize(file)
        }
        return size
    }

    private fun loadUserSettings() {
        viewModelScope.launch {
            try {
                val settings = userPrefsRepository.getUserSettings()
                _darkTheme.value = settings.darkTheme
                _dynamicColors.value = settings.dynamicColors
                _appLanguage.value = settings.appLanguage
                _showDownloadNotification.value = settings.showDownloadNotification
                _showWallpaperChangeNotification.value = settings.showWallpaperChangeNotification
                _downloadOriginalQuality.value = settings.downloadOriginalQuality
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user settings: ${e.message}")
            }
        }
    }
    fun updateDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _darkTheme.value = enabled
                saveSettings { it.copy(darkTheme = enabled) }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating dark theme: ${e.message}")
            }
        }
    }
    fun updateDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _dynamicColors.value = enabled
                saveSettings { it.copy(dynamicColors = enabled) }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating dynamic colors: ${e.message}")
            }
        }
    }
    fun updateAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            try {
                _appLanguage.value = language
                localeManager.updateAppLanguage(language)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating app language: ${e.message}")
            }
        }
    }
    fun updateShowDownloadNotification(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled && !hasNotificationPermission()) {
                    pendingEnableDownloadNotification = enabled
                    _needNotificationPermission.value = true
                    return@launch
                }
                _showDownloadNotification.value = enabled
                userPrefsRepository.updateNotificationSettings(showDownloadNotification = enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating download notification: ${e.message}")
            }
        }
    }
    fun updateShowWallpaperChangeNotification(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled && !hasNotificationPermission()) {
                    pendingEnableWallpaperNotification = enabled
                    _needNotificationPermission.value = true
                    return@launch
                }
                _showWallpaperChangeNotification.value = enabled
                userPrefsRepository.updateNotificationSettings(showWallpaperChangeNotification = enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating wallpaper change notification: ${e.message}")
            }
        }
    }
    fun onNotificationPermissionGranted(type: NotificationType) {
        viewModelScope.launch {
            _needNotificationPermission.value = false
            when (type) {
                NotificationType.DOWNLOAD -> {
                    pendingEnableDownloadNotification?.let { enabled ->
                        _showDownloadNotification.value = enabled
                        userPrefsRepository.updateNotificationSettings(showDownloadNotification = enabled)
                    }
                    pendingEnableDownloadNotification = null
                }
                NotificationType.WALLPAPER_CHANGE -> {
                    pendingEnableWallpaperNotification?.let { enabled ->
                        _showWallpaperChangeNotification.value = enabled
                        userPrefsRepository.updateNotificationSettings(showWallpaperChangeNotification = enabled)
                    }
                    pendingEnableWallpaperNotification = null
                }
            }
        }
    }
    fun onNotificationPermissionDenied() {
        _needNotificationPermission.value = false
        pendingEnableDownloadNotification = null
        pendingEnableWallpaperNotification = null
        _operationResult.value = "通知权限已被拒绝"
    }
    fun clearOperationResult() { _operationResult.value = null }
    fun updateDownloadOriginalQuality(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _downloadOriginalQuality.value = enabled
                saveSettings { it.copy(downloadOriginalQuality = enabled) }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating original quality: ${e.message}")
            }
        }
    }
    private fun calculateCacheSize() {
        viewModelScope.launch {
            try {
                val total = directorySize(context.cacheDir) + directorySize(context.externalCacheDir)
                _cacheSize.value = formatSize(total)
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating cache size: ${e.message}")
            }
        }
    }
    fun clearCache() {
        viewModelScope.launch {
            try {
                _isClearingCache.value = true
                deleteRecursively(context.cacheDir)
                deleteRecursively(context.externalCacheDir)
                calculateCacheSize()
                _operationResult.value = stringProvider.getString(R.string.settings_clear_cache)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache: ${e.message}")
                _operationResult.value = stringProvider.getString(R.string.settings_clear_cache)
            } finally {
                _isClearingCache.value = false
            }
        }
    }
    private fun deleteRecursively(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { f ->
            if (f.isDirectory) deleteRecursively(f)
            runCatching { f.delete() }
        }
    }
    private fun loadAppVersion() {
        viewModelScope.launch {
            try {
                val pm = context.packageManager
                val pInfo = pm.getPackageInfo(context.packageName, 0)
                _appVersion.value = pInfo.versionName ?: "1.0.0"
            } catch (e: Exception) {
                Log.e(TAG, "Error loading app version: ${e.message}")
            }
        }
    }
}
