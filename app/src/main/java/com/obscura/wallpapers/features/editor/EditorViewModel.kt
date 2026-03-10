package com.obscura.wallpapers.features.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obscura.wallpapers.R
import com.obscura.wallpapers.cache.EditedImageCache
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.repository.WallpaperRepository
import com.obscura.wallpapers.core.common.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class WallpaperEditState(
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val filter: ImageFilter = ImageFilter.NONE,
    val croppedBitmap: Bitmap? = null
)

@HiltViewModel
class WallpaperEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wallpaperRepository: WallpaperRepository,
    savedStateHandle: SavedStateHandle,
    val stringProvider: StringProvider
) : ViewModel() {

    private val wallpaperId: String = checkNotNull(savedStateHandle["wallpaperId"])

    private val _wallpaperState = MutableStateFlow<UiState<Wallpaper>>(UiState.Loading)
    val wallpaperState: StateFlow<UiState<Wallpaper>> = _wallpaperState.asStateFlow()

    private val _editState = MutableStateFlow(WallpaperEditState())
    val editState: StateFlow<WallpaperEditState> = _editState.asStateFlow()

    private var originalBitmap: Bitmap? = null

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadWallpaper()
    }

    private fun loadWallpaper() {
        viewModelScope.launch {
            _wallpaperState.value = UiState.Loading

            try {
                var wallpaper: Wallpaper? = wallpaperRepository.getWallpaperById(wallpaperId)

                if (wallpaper != null) {
                    _wallpaperState.value = UiState.Success(wallpaper)
                    loadOriginalBitmap(wallpaper.url)

                    try {
                        val updatedWallpaper = wallpaperRepository.getWallpaperById(wallpaperId)
                        if (updatedWallpaper != null && updatedWallpaper != wallpaper) {
                            _wallpaperState.value = UiState.Success(updatedWallpaper)
                            if (updatedWallpaper.url != wallpaper.url) {
                                loadOriginalBitmap(updatedWallpaper.url)
                            }
                        }
                    } catch (_: Exception) {
                    }
                    return@launch
                }

                var retryCount = 0
                val maxRetries = 3
                var isRateLimitError = false

                while (wallpaper == null && retryCount < maxRetries) {
                    try {
                        wallpaper = wallpaperRepository.getWallpaperById(wallpaperId)
                        if (wallpaper != null) {
                            break
                        }
                    } catch (e: Exception) {
                        if (e.message?.contains("Rate Limit") == true || e.message?.contains("403") == true) {
                            isRateLimitError = true
                            kotlinx.coroutines.delay(2000)
                        }
                    }
                    retryCount++
                    if (!isRateLimitError) {
                        kotlinx.coroutines.delay(500)
                    }
                }

                if (wallpaper != null) {
                    _wallpaperState.value = UiState.Success(wallpaper)
                    loadOriginalBitmap(wallpaper.url)
                } else {
                    if (isRateLimitError) {
                        _wallpaperState.value =
                            UiState.Error(stringProvider.getString(R.string.network_api_rate_limit_exceeded))
                    } else {
                        _wallpaperState.value =
                            UiState.Error(stringProvider.getString(R.string.wallpaper_load_failed))
                    }
                }
            } catch (e: Exception) {
                _wallpaperState.value = UiState.Error(
                    e.message ?: stringProvider.getString(R.string.wallpaper_load_failed)
                )
            }
        }
    }

    private fun loadOriginalBitmap(url: String?) {
        if (url == null) return

        val editedImage = EditedImageCache.getEditedImage(wallpaperId)
        if (editedImage != null) {
            originalBitmap = editedImage
            _editState.value = _editState.value.copy(croppedBitmap = editedImage)
            return
        }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    try {
                        val cacheDir = File(context.cacheDir, "wallpapers")
                        if (!cacheDir.exists()) {
                            cacheDir.mkdirs()
                        }

                        val cacheFile = File(cacheDir, "original_${wallpaperId}.jpg")

                        if (cacheFile.exists() && cacheFile.length() > 0) {
                            originalBitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                        } else {
                            val imageUrl = URL(url)
                            val connection = imageUrl.openConnection()
                            connection.connectTimeout = 5000
                            connection.readTimeout = 5000

                            val inputStream = connection.getInputStream()
                            originalBitmap = BitmapFactory.decodeStream(inputStream)

                            if (originalBitmap != null) {
                                val outputStream = FileOutputStream(cacheFile)
                                originalBitmap?.compress(
                                    Bitmap.CompressFormat.JPEG, 100, outputStream
                                )
                                outputStream.close()
                            }

                            inputStream.close()
                        }
                    } catch (e: Exception) {
                        val imageUrl = URL(url)
                        originalBitmap = BitmapFactory.decodeStream(imageUrl.openStream())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "${stringProvider.getString(R.string.wallpaper_load_failed)}: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun updateBrightness(value: Float) {
        _editState.value = _editState.value.copy(brightness = value)
    }

    fun updateContrast(value: Float) {
        _editState.value = _editState.value.copy(contrast = value)
    }

    fun updateSaturation(value: Float) {
        _editState.value = _editState.value.copy(saturation = value)
    }

    fun applyFilter(filter: ImageFilter) {
        _editState.value = _editState.value.copy(filter = filter)
    }

    fun resetEdits() {
        _editState.value = WallpaperEditState()
    }

    fun updateCroppedBitmap(bitmap: Bitmap?) {
        _editState.value = _editState.value.copy(croppedBitmap = bitmap)
    }

    fun applyEdits() {
    }

    fun saveEditedWallpaper(onComplete: () -> Unit) {
        val sourceBitmap = _editState.value.croppedBitmap ?: originalBitmap ?: return

        viewModelScope.launch {
            _isSaving.value = true

            try {
                val finalBitmap = withContext(Dispatchers.Default) {
                    applyEffectsToBitmap(
                        sourceBitmap,
                        _editState.value.brightness,
                        _editState.value.contrast,
                        _editState.value.saturation,
                        _editState.value.filter
                    )
                }

                saveToGallery(finalBitmap)

                EditedImageCache.saveEditedImage(wallpaperId, finalBitmap)

                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (_: Exception) {
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun saveToGallery(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Obscura_Edited_$timestamp.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
                )

                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    }
                }
            } else {
                val imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imagesDir, fileName)
                FileOutputStream(image).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
            }
        }
    }

    private fun applyEffectsToBitmap(
        bitmap: Bitmap, brightness: Float, contrast: Float, saturation: Float, filter: ImageFilter
    ): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()

        val brightnessMatrix = ColorMatrix().apply {
            val scale = brightness
            val matrix = floatArrayOf(
                scale, 0f, 0f, 0f, 0f,
                0f, scale, 0f, 0f, 0f,
                0f, 0f, scale, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            set(matrix)
        }
        colorMatrix.postConcat(brightnessMatrix)

        val contrastMatrix = ColorMatrix().apply {
            val scale = contrast
            val translate = (1.0f - scale) * 128f
            val matrix = floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
            set(matrix)
        }
        colorMatrix.postConcat(contrastMatrix)

        val saturationMatrix = ColorMatrix().apply {
            setSaturation(saturation)
        }
        colorMatrix.postConcat(saturationMatrix)

        when (filter) {
            ImageFilter.GRAYSCALE -> {
                val grayscaleMatrix = ColorMatrix().apply {
                    setSaturation(0f)
                }
                colorMatrix.postConcat(grayscaleMatrix)
            }

            ImageFilter.SEPIA -> {
                val sepiaMatrix = ColorMatrix().apply {
                    val matrix = floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    set(matrix)
                }
                colorMatrix.postConcat(sepiaMatrix)
            }

            ImageFilter.VINTAGE -> {
                val vintageMatrix = ColorMatrix().apply {
                    val matrix = floatArrayOf(
                        0.9f, 0.5f, 0.1f, 0f, 0f,
                        0.3f, 0.8f, 0.1f, 0f, 0f,
                        0.2f, 0.3f, 0.5f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    set(matrix)
                }
                colorMatrix.postConcat(vintageMatrix)
            }

            ImageFilter.COLD -> {
                val coldMatrix = ColorMatrix().apply {
                    val matrix = floatArrayOf(
                        0.8f, 0f, 0f, 0f, 0f,
                        0f, 0.9f, 0.1f, 0f, 0f,
                        0f, 0.1f, 1.1f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    set(matrix)
                }
                colorMatrix.postConcat(coldMatrix)
            }

            ImageFilter.WARM -> {
                val warmMatrix = ColorMatrix().apply {
                    val matrix = floatArrayOf(
                        1.1f, 0f, 0f, 0f, 10f,
                        0f, 1.0f, 0f, 0f, 10f,
                        0f, 0f, 0.8f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    set(matrix)
                }
                colorMatrix.postConcat(warmMatrix)
            }

            ImageFilter.PURPLE -> {
                val purpleMatrix = ColorMatrix().apply {
                    val matrix = floatArrayOf(
                        0.9f, 0.1f, 0.2f, 0f, 0f,
                        0.1f, 0.8f, 0.2f, 0f, 0f,
                        0.2f, 0.2f, 1.0f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    set(matrix)
                }
                colorMatrix.postConcat(purpleMatrix)
            }

            ImageFilter.BLUE -> {
                val blueMatrix = ColorMatrix().apply {
                    val matrix = floatArrayOf(
                        0.8f, 0f, 0f, 0f, 0f,
                        0f, 0.9f, 0f, 0f, 0f,
                        0.2f, 0.2f, 1.2f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    set(matrix)
                }
                colorMatrix.postConcat(blueMatrix)
            }

            ImageFilter.GREEN -> {
                val greenMatrix = ColorMatrix().apply {
                    val matrix = floatArrayOf(
                        0.8f, 0.1f, 0f, 0f, 0f,
                        0.1f, 1.1f, 0.1f, 0f, 10f,
                        0f, 0.1f, 0.8f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    set(matrix)
                }
                colorMatrix.postConcat(greenMatrix)
            }

            ImageFilter.PINK -> {
                val pinkMatrix = ColorMatrix().apply {
                    val matrix = floatArrayOf(
                        1.2f, 0.1f, 0.1f, 0f, 10f,
                        0.1f, 0.9f, 0.1f, 0f, 0f,
                        0.1f, 0.1f, 0.8f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    set(matrix)
                }
                colorMatrix.postConcat(pinkMatrix)
            }

            ImageFilter.ORANGE -> {
                val orangeMatrix = ColorMatrix().apply {
                    val matrix = floatArrayOf(
                        1.3f, 0.1f, 0f, 0f, 10f,
                        0.1f, 0.9f, 0f, 0f, 5f,
                        0f, 0f, 0.7f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    set(matrix)
                }
                colorMatrix.postConcat(orangeMatrix)
            }

            ImageFilter.NONE -> {
            }
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
}
