package com.obscura.wallpapers.features.preview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.billing.BillingManager
import com.obscura.wallpapers.core.common.ImageProcessor
import com.obscura.wallpapers.core.common.NotificationUtil
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.model.WallpaperTarget
import com.obscura.wallpapers.core.data.repository.DiamondRepository
import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.core.data.repository.WallpaperRepository
import com.obscura.wallpapers.cache.EditedImageCache
import com.obscura.wallpapers.features.diamond.DiamondPurchaseResult
import com.obscura.wallpapers.manager.AppWallpaperManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WallpaperPreviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wallpaperRepository: WallpaperRepository,
    private val userPrefsRepository: UserPrefsRepository,
    private val userRepository: UserRepository,
    private val diamondRepository: DiamondRepository,
    private val billingManager: BillingManager,
    private val wallpaperManager: AppWallpaperManager,
    private val notificationUtil: NotificationUtil,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "WallpaperPreviewViewModel"
    }

    private val wallpaperId: String = checkNotNull(savedStateHandle["wallpaperId"])

    private val _wallpaperState = MutableStateFlow<UiState<Wallpaper>>(UiState.Loading)
    val wallpaperState: StateFlow<UiState<Wallpaper>> = _wallpaperState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _needLoginAction = MutableStateFlow<LoginAction?>(null)
    val needLoginAction: StateFlow<LoginAction?> = _needLoginAction.asStateFlow()

    private val _editedBitmap = mutableStateOf<Bitmap?>(null)
    val editedBitmap: State<Bitmap?> = _editedBitmap

    private val _blurredBackgroundBitmap = mutableStateOf<Bitmap?>(null)
    val blurredBackgroundBitmap: State<Bitmap?> = _blurredBackgroundBitmap

    private var currentWallpaperId: String = ""

    private val _isFavorite = mutableStateOf(false)
    val isFavorite: State<Boolean> = _isFavorite

    private val _isPremiumUser = mutableStateOf(false)
    val isPremiumUser: State<Boolean> = _isPremiumUser

    private val _diamondBalance = MutableStateFlow(0)
    val diamondBalance: StateFlow<Int> = _diamondBalance.asStateFlow()

    private val _showDiamondPurchaseDialog = mutableStateOf(false)
    val showDiamondPurchaseDialog: State<Boolean> = _showDiamondPurchaseDialog

    private val _diamondPurchaseResult = MutableStateFlow<DiamondPurchaseResult?>(null)
    val diamondPurchaseResult: StateFlow<DiamondPurchaseResult?> =
        _diamondPurchaseResult.asStateFlow()

    private val _needStoragePermission = mutableStateOf(false)
    val needStoragePermission: State<Boolean> = _needStoragePermission

    private val _isDownloading = mutableStateOf(false)
    val isDownloading: State<Boolean> = _isDownloading

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _isInfoExpanded = mutableStateOf(false)
    val isInfoExpanded: State<Boolean> = _isInfoExpanded

    private val _showSetWallpaperOptions = mutableStateOf(false)
    val showSetWallpaperOptions: State<Boolean> = _showSetWallpaperOptions

    private val _navigateToUpgrade = mutableStateOf(false)
    val navigateToUpgrade: State<Boolean> = _navigateToUpgrade

    private val _isProcessingWallpaper = mutableStateOf(false)
    val isProcessingWallpaper: State<Boolean> = _isProcessingWallpaper

    private val _wallpaperSetSuccess = MutableStateFlow<String?>(null)
    val wallpaperSetSuccess: StateFlow<String?> = _wallpaperSetSuccess.asStateFlow()

    sealed class UpgradeResult {
        data class Success(val message: String) : UpgradeResult()
        data class Error(val message: String) : UpgradeResult()
    }

    private val _upgradeResult = MutableStateFlow<UpgradeResult?>(null)
    val upgradeResult: StateFlow<UpgradeResult?> = _upgradeResult.asStateFlow()

    enum class LoginAction {
        FAVORITE, DOWNLOAD, SET_WALLPAPER, EDIT
    }

    private fun loadWallpaper() {
        viewModelScope.launch {
            try {
                _wallpaperState.value = UiState.Loading
                val wallpaper = wallpaperRepository.getWallpaperById(wallpaperId)
                if (wallpaper != null) {
                    currentWallpaperId = wallpaper.id
                    _isFavorite.value = wallpaperRepository.isWallpaperFavorited(wallpaper.id)
                    _isPremiumUser.value = userRepository.checkPremiumStatus()
                    _wallpaperState.value = UiState.Success(wallpaper)
                } else {
                    _wallpaperState.value = UiState.Error("加载壁纸失败")
                }
            } catch (e: Exception) {
                _wallpaperState.value = UiState.Error(e.message ?: "加载壁纸失败")
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val s = _wallpaperState.value
            if (s is UiState.Success) {
                val w = s.data
                if (!_isLoggedIn.value) {
                    _needLoginAction.value = LoginAction.FAVORITE
                    return@launch
                }
                val ok = if (_isFavorite.value) {
                    wallpaperRepository.unfavoriteWallpaper(w.id)
                } else {
                    wallpaperRepository.favoriteWallpaper(w)
                }
                if (ok) _isFavorite.value = !_isFavorite.value
            }
        }
    }

    fun toggleInfoExpanded() {
        _isInfoExpanded.value = !_isInfoExpanded.value
    }

    fun showSetWallpaperOptions(activity: android.app.Activity?) {
        _showSetWallpaperOptions.value = true
    }

    fun hideSetWallpaperOptions() {
        _showSetWallpaperOptions.value = false
    }

    fun setWallpaper(activity: android.app.Activity?, target: WallpaperTarget) {
        viewModelScope.launch {
            val s = _wallpaperState.value
            if (s is UiState.Success) {
                val w = s.data
                _isProcessingWallpaper.value = true
                wallpaperManager.setWallpaper(w, target, _editedBitmap.value) { success ->
                    _isProcessingWallpaper.value = false
                    _showSetWallpaperOptions.value = false
                    if (success) {
                        _wallpaperSetSuccess.value = when (target) {
                            WallpaperTarget.HOME -> "已设置主屏幕壁纸"
                            WallpaperTarget.LOCK -> "已设置锁屏壁纸"
                            WallpaperTarget.BOTH -> "已同时设置主屏与锁屏"
                        }
                    } else {
                        _wallpaperSetSuccess.value = null
                    }
                }
            }
        }
    }

    fun previewWallpaper(activity: android.app.Activity?) {
        viewModelScope.launch {
            val s = _wallpaperState.value
            if (s is UiState.Success && activity != null) {
                wallpaperManager.previewWallpaper(activity, s.data, _editedBitmap.value)
            }
        }
    }

    fun shareWallpaper() {
        viewModelScope.launch {
            val s = _wallpaperState.value
            if (s is UiState.Success) {
                try {
                    wallpaperRepository.trackWallpaperDownload(s.data.id)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun downloadWallpaper() {
        viewModelScope.launch {
            val s = _wallpaperState.value
            if (s is UiState.Success) {
                if (!_isLoggedIn.value) {
                    _needLoginAction.value = LoginAction.DOWNLOAD
                    return@launch
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    val granted = ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        _needStoragePermission.value = true
                        return@launch
                    }
                }
                _isDownloading.value = true
                _downloadProgress.value = 0.1f
                wallpaperRepository.trackWallpaperDownload(s.data.id)
                _downloadProgress.value = 1.0f
                _isDownloading.value = false
            }
        }
    }

    fun continueDownloadAfterPermissionGranted() {
        _needStoragePermission.value = false
        downloadWallpaper()
    }

    fun resetPermissionRequest() {
        _needStoragePermission.value = false
    }

    fun refreshEditedImage() {
        _editedBitmap.value = EditedImageCache.getEditedImage(currentWallpaperId)
    }

    fun loadBlurredBackground() {
        viewModelScope.launch {
            val s = _wallpaperState.value
            if (s is UiState.Success && !s.data.isLive) {
                try {
                    val file = wallpaperRepository.getLocalFile(s.data.id)
                    if (file != null && file.exists()) {
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        _blurredBackgroundBitmap.value = ImageProcessor.blurGaussian(context, bmp)
                    } else {
                        _blurredBackgroundBitmap.value = null
                    }
                } catch (_: Exception) {
                    _blurredBackgroundBitmap.value = null
                }
            }
        }
    }

    fun showPremiumPrompt() {
        _navigateToUpgrade.value = true
        _upgradeResult.value = UpgradeResult.Error("需要升级为高级用户")
    }

    fun resetNavigateToUpgrade() {
        _navigateToUpgrade.value = false
    }

    fun clearDiamondPurchaseResult() {
        _diamondPurchaseResult.value = null
    }

    fun clearUpgradeResult() {
        _upgradeResult.value = null
    }

    fun purchaseWithDiamonds() {
        viewModelScope.launch {
            val s = _wallpaperState.value
            if (s is UiState.Success) {
                val marked = wallpaperRepository.markWallpaperAsPurchased(s.data.id)
                if (marked) {
                    _diamondPurchaseResult.value = DiamondPurchaseResult.Success("已购买解锁")
                    _showDiamondPurchaseDialog.value = false
                } else {
                    _diamondPurchaseResult.value = DiamondPurchaseResult.Error("购买失败")
                }
            }
        }
    }

    fun hideDiamondPurchaseDialog() {
        _showDiamondPurchaseDialog.value = false
    }

    fun setNeedLoginAction(action: LoginAction) {
        _needLoginAction.value = action
    }

    fun clearNeedLoginAction() {
        _needLoginAction.value = null
    }

    fun clearWallpaperSetSuccess() {
        _wallpaperSetSuccess.value = null
    }

    init {
        viewModelScope.launch {
            try {
                _isLoggedIn.value = userRepository.checkUserLoggedIn()
            } catch (_: Exception) {
                _isLoggedIn.value = false
            }
        }
        loadWallpaper()
    }
}
