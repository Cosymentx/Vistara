package com.obscura.wallpapers.features.preview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.R
import com.obscura.wallpapers.cache.EditedImageCache
import com.obscura.wallpapers.core.common.ImageProcessor
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.model.WallpaperTarget
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.core.data.repository.WallpaperRepository
import com.obscura.wallpapers.wallpaper.core.AppWallpaperManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class WallpaperPreviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wallpaperRepository: WallpaperRepository,
    private val userRepository: UserRepository,
    private val wallpaperManager: AppWallpaperManager,
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

    private val _coinBalance = MutableStateFlow(0)
    val coinBalance: StateFlow<Int> = _coinBalance.asStateFlow()

    private val _showCoinPurchaseDialog = mutableStateOf(false)
    val showCoinPurchaseDialog: State<Boolean> = _showCoinPurchaseDialog

    private val _isWallpaperPurchased = mutableStateOf(false)
    val isWallpaperPurchased: State<Boolean> = _isWallpaperPurchased

    private val _showPurchasePrompt = mutableStateOf(false)
    val showPurchasePrompt: State<Boolean> = _showPurchasePrompt

    private val _purchasePromptMessage = mutableStateOf("")
    val purchasePromptMessage: State<String> = _purchasePromptMessage

    private val _purchaseType = mutableStateOf(PurchaseType.NONE)
    val purchaseType: State<PurchaseType> = _purchaseType

    enum class PurchaseType {
        NONE, SUBSCRIPTION, COIN
    }


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
                    _isWallpaperPurchased.value = wallpaperRepository.isWallpaperPurchased(wallpaper.id)
                    _wallpaperState.value = UiState.Success(wallpaper)
                } else {
                    _wallpaperState.value =
                        UiState.Error(context.getString(R.string.errors_loading_wallpapers))
                }
            } catch (e: Exception) {
                _wallpaperState.value = UiState.Error(
                    e.message ?: context.getString(R.string.errors_loading_wallpapers)
                )
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

                // Check if premium and not a premium user
                if (w.isPremium && !_isPremiumUser.value) {
                    _purchasePromptMessage.value = context.getString(R.string.feature_requires_subscription)
                    _purchaseType.value = PurchaseType.SUBSCRIPTION
                    _showPurchasePrompt.value = true
                    return@launch
                }
                
                // Check if wallpaper requires purchase
                if (w.requiresPurchase && !_isWallpaperPurchased.value) {
                    _purchasePromptMessage.value = context.getString(
                        R.string.purchase_required_for_action,
                        w.purchasePrice.toString()
                    )
                    _purchaseType.value = PurchaseType.COIN
                    _showPurchasePrompt.value = true
                    return@launch
                }
                
                _isProcessingWallpaper.value = true
                try {
                    wallpaperManager.setWallpaper(w, target, _editedBitmap.value) { success ->
                        _isProcessingWallpaper.value = false
                        if (success) {
                            _wallpaperSetSuccess.value = when (target) {
                                WallpaperTarget.HOME -> context.getString(com.obscura.wallpapers.R.string.wallpaper_set_home_success)
                                WallpaperTarget.LOCK -> context.getString(com.obscura.wallpapers.R.string.wallpaper_set_lock_success)
                                WallpaperTarget.BOTH -> context.getString(com.obscura.wallpapers.R.string.wallpaper_set_both_success)
                            }
                            // 延迟关闭对话框，让用户看到成功提示
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(1500)
                                _showSetWallpaperOptions.value = false
                            }
                        } else {
                            _wallpaperSetSuccess.value = context.getString(com.obscura.wallpapers.R.string.wallpaper_set_failed)
                        }
                    }
                } catch (e: Exception) {
                    _isProcessingWallpaper.value = false
                    _wallpaperSetSuccess.value = context.getString(com.obscura.wallpapers.R.string.wallpaper_set_failed)
                }
            }
        }
    }

    fun preview(activity: android.app.Activity?) {
        viewModelScope.launch {
            val s = _wallpaperState.value
            if (s is UiState.Success && activity != null) {
                wallpaperManager.previewWallpaper(activity, s.data, _editedBitmap.value)
            }
        }
    }

    fun share() {
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

    fun download() {
        viewModelScope.launch {
            val s = _wallpaperState.value
            if (s is UiState.Success) {
                val w = s.data

                // Check if premium and not a premium user
                if (w.isPremium && !_isPremiumUser.value) {
                    _purchasePromptMessage.value = context.getString(R.string.feature_requires_subscription)
                    _purchaseType.value = PurchaseType.SUBSCRIPTION
                    _showPurchasePrompt.value = true
                    return@launch
                }
                
                // Check if wallpaper requires purchase
                if (w.requiresPurchase && !_isWallpaperPurchased.value) {
                    _purchasePromptMessage.value = context.getString(
                        R.string.purchase_required_for_action,
                        w.purchasePrice.toString()
                    )
                    _purchaseType.value = PurchaseType.COIN
                    _showPurchasePrompt.value = true
                    return@launch
                }
                
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
                _downloadProgress.value = 0f
                try {
                    wallpaperManager
                        .downloadWallpaper(s.data, downloadOriginalQuality = true)
                        .collect { (progress, filePath) ->
                            when {
                                progress < 0f -> {
                                    _downloadProgress.value = 0f
                                    _isDownloading.value = false
                                }

                                progress >= 1f -> {
                                    _downloadProgress.value = 1f
                                    _isDownloading.value = false
                                    wallpaperRepository.trackWallpaperDownload(s.data.id)
                                    try {
                                        Toast.makeText(
                                            context,
                                            context.getString(com.obscura.wallpapers.R.string.preview_download_success),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (_: Exception) {
                                    }
                                }

                                else -> {
                                    _downloadProgress.value = progress
                                }
                            }
                        }
                } catch (_: Exception) {
                    _downloadProgress.value = 0f
                    _isDownloading.value = false
                }
            }
        }
    }

    fun continueDownloadAfterPermissionGranted() {
        _needStoragePermission.value = false
        download()
    }

    fun resetPermissionRequest() {
        _needStoragePermission.value = false
    }

    fun refreshEditedImage() {
        _editedBitmap.value = EditedImageCache.getEditedImage(currentWallpaperId)
    }

    fun refreshPurchaseState() {
        viewModelScope.launch {
            try {
                _isWallpaperPurchased.value = wallpaperRepository.isWallpaperPurchased(currentWallpaperId)
            } catch (_: Exception) {
                _isWallpaperPurchased.value = false
            }
        }
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


    fun resetNavigateToUpgrade() {
        _navigateToUpgrade.value = false
    }

    fun clearUpgradeResult() {
        _upgradeResult.value = null
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

    fun closePurchasePrompt() {
        _showPurchasePrompt.value = false
    }

    fun onPurchaseConfirmed() {
        _showPurchasePrompt.value = false
        // The navigation is handled in the UI (WallpaperPreviewScreen)
    }

    fun onCoinPurchaseClick() {
        _showPurchasePrompt.value = false
        // The navigation is handled in the UI (WallpaperPreviewScreen)
    }

    fun refreshPurchaseStatus() {
        viewModelScope.launch {
            val s = _wallpaperState.value
            if (s is UiState.Success) {
                _isWallpaperPurchased.value = wallpaperRepository.isWallpaperPurchased(s.data.id)
            }
        }
    }

    fun onPurchaseSuccess(wallpaperId: String) {
        viewModelScope.launch {
            try {
                wallpaperRepository.markWallpaperAsPurchased(wallpaperId)
                _isWallpaperPurchased.value = true
                _showPurchasePrompt.value = false
                Toast.makeText(
                    context,
                    context.getString(R.string.purchase_success),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.purchase_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun showPurchasePromptForEdit(wallpaper: Wallpaper) {
        if (wallpaper.isPremium && !_isPremiumUser.value) {
            _purchasePromptMessage.value = context.getString(R.string.feature_requires_subscription)
            _purchaseType.value = PurchaseType.SUBSCRIPTION
        } else {
            _purchasePromptMessage.value = context.getString(
                R.string.purchase_required_for_action,
                wallpaper.purchasePrice.toString()
            )
            _purchaseType.value = PurchaseType.COIN
        }
        _showPurchasePrompt.value = true
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
