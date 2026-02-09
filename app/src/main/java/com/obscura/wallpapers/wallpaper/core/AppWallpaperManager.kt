package com.obscura.wallpapers.wallpaper.core

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.model.WallpaperTarget
import com.obscura.wallpapers.wallpaper.live.LiveWallpaperService
import com.obscura.wallpapers.di.ActivityScopeHolder
import com.obscura.wallpapers.core.common.NotificationUtil
import com.obscura.wallpapers.core.common.StringProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppWallpaperManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityScopeHolder: ActivityScopeHolder,
) {
    companion object {
        private const val TAG = "AppWallpaperManager"
        private const val WALLPAPERS_DIR = "wallpapers"
        private const val DOWNLOAD_BUFFER_SIZE = 8192
    }
    interface DownloadProgressCallback { fun onProgressUpdate(progress: Float) }
    @Inject lateinit var stringProvider: StringProvider
    @Inject lateinit var notificationUtil: NotificationUtil
    suspend fun setWallpaper(
        wallpaper: Wallpaper, target: WallpaperTarget, editedBitmap: Bitmap? = null, onComplete: (Boolean) -> Unit
    ) {
        try {
            val activity = activityScopeHolder.current()
            Log.d(TAG, "Setting wallpaper: ${wallpaper.title}, target: ${wallpaper.isLive}")
            activity?.let {
                if (wallpaper.isLive) {
                    setLiveWallpaper(activity, wallpaper, target, onComplete)
                } else {
                    setStaticWallpaper(activity, wallpaper, target, editedBitmap, onComplete)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting wallpaper", e)
            withContext(Dispatchers.Main) { onComplete(false) }
        }
    }
    private suspend fun setStaticWallpaper(
        activity: Activity, wallpaper: Wallpaper, target: WallpaperTarget, editedBitmap: Bitmap? = null, onComplete: (Boolean) -> Unit,
    ) {
        try {
            val bitmap = editedBitmap ?: loadBitmapFromFile(wallpaper.id) ?: withContext(Dispatchers.IO) {
                val url = URL(wallpaper.url); BitmapFactory.decodeStream(url.openStream())
            }
            saveBitmapToFile(bitmap, wallpaper.id)
            withContext(Dispatchers.IO) {
                val wallpaperManager = WallpaperManager.getInstance(activity)
                when (target) {
                    WallpaperTarget.HOME -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        } else wallpaperManager.setBitmap(bitmap)
                    }
                    WallpaperTarget.LOCK -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                        } else Log.w(TAG, "Setting lock screen wallpaper not supported on this Android version")
                    }
                    WallpaperTarget.BOTH -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                        } else wallpaperManager.setBitmap(bitmap)
                    }
                }
                sendWallpaperChangedNotification(wallpaper)
            }
            withContext(Dispatchers.Main) {
                val message = getSuccessMessage(target)
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                onComplete(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting static wallpaper", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, stringProvider.getString(R.string.set_wallpaper_failed), Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
        }
    }
    suspend fun previewWallpaper(context: Activity, wallpaper: Wallpaper, editedBitmap: Bitmap? = null) {
        try {
            val bitmap = editedBitmap ?: loadBitmapFromFile(wallpaper.id) ?: withContext(Dispatchers.IO) {
                val url = URL(wallpaper.url); BitmapFactory.decodeStream(url.openStream())
            }
            saveBitmapToFile(bitmap, wallpaper.id)
            val success = com.obscura.wallpapers.core.common.WallpaperPreviewUtil.previewWallpaper(context, bitmap)
            if (!success) { }
        } catch (e: Exception) { Log.e(TAG, "Error previewing wallpaper", e) }
    }
    private suspend fun setLiveWallpaper(
        activity: Activity, wallpaper: Wallpaper, target: WallpaperTarget, onComplete: (Boolean) -> Unit
    ) {
        try {
            val videoUrl = wallpaper.url
            if (videoUrl.isNullOrEmpty()) {
                Log.e(TAG, "Video URL is null or empty")
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, stringProvider.getString(R.string.invalid_video_url), Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
                return
            }
            val cacheDir = File(activity.cacheDir, "videos")
            val videoFileName = "video_${System.currentTimeMillis()}.mp4"
            val videoFile = File(cacheDir, videoFileName)
            val cachedFile = findCachedVideoByUrl(activity, videoUrl)
            if (cachedFile == null) {
                Log.d(TAG, "Starting to download video from URL: $videoUrl")
                val cacheDir2 = File(context.cacheDir, "videos"); if (!cacheDir2.exists()) { cacheDir2.mkdirs() }
                val emptyProgressCallback = object : DownloadProgressCallback { override fun onProgressUpdate(progress: Float) {} }
                downloadFile(videoUrl, videoFile, emptyProgressCallback)
            } else { Log.d(TAG, "Using cached video file: ${cachedFile.absolutePath}") }
            val wallpaperManager = WallpaperManager.getInstance(activity)
            withContext(Dispatchers.IO) {
                try {
                    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.TRANSPARENT)
                    when (target) {
                        WallpaperTarget.HOME -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        WallpaperTarget.LOCK -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                        WallpaperTarget.BOTH -> {
                            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                        }
                    }
                    bitmap.recycle(); Log.d(TAG, "Successfully reset wallpaper state"); delay(300)
                } catch (e: Exception) { Log.e(TAG, "Failed to reset wallpaper: ${e.message}") }
            }
            LiveWallpaperService.setVideoUri(activity, Uri.parse(videoUrl))
            Log.d(TAG, "Set video URI directly from URL: $videoUrl")
            val result = setVideoWallpaper(activity)
            withContext(Dispatchers.Main) {
                if (result.directSuccess) {
                    val message = getSuccessMessage(target); Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            notificationUtil.showWallpaperChangedNotification(wallpaper)
                        }
                    } else { notificationUtil.showWallpaperChangedNotification(wallpaper) }
                } else if (!result.anyMethodSucceeded) {
                    Toast.makeText(activity, stringProvider.getString(R.string.set_wallpaper_failed), Toast.LENGTH_SHORT).show()
                }
                onComplete(result.anyMethodSucceeded)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting live wallpaper", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, stringProvider.getString(R.string.set_wallpaper_failed), Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
        }
    }
    private fun findCachedVideoByUrl(context: Context, videoUrl: String): File? {
        return try {
            val cacheDir = File(context.cacheDir, "videos")
            if (!cacheDir.exists() || !cacheDir.isDirectory) return null
            val videoFiles = cacheDir.listFiles { file -> file.name.endsWith(".mp4") } ?: return null
            videoFiles.maxByOrNull { it.lastModified() }
        } catch (e: Exception) { Log.e(TAG, "Error finding cached video: ${e.message}"); null }
    }
    data class WallpaperSetResult(val directSuccess: Boolean, val anyMethodSucceeded: Boolean)
    private suspend fun setVideoWallpaper(activity: Activity): WallpaperSetResult = withContext(Dispatchers.IO) {
        try {
            val videoUri = LiveWallpaperService.getCurrentVideoUri(activity) ?: return@withContext WallpaperSetResult(false, false)
            Log.d(TAG, "Setting video wallpaper with URI: $videoUri")
            try {
                val componentName = ComponentName(activity.packageName, LiveWallpaperService::class.java.name)
                withContext(Dispatchers.Main) {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, componentName)
                    activity.startActivity(intent)
                }
                Log.d(TAG, "Started ACTION_CHANGE_LIVE_WALLPAPER intent as fallback")
                return@withContext WallpaperSetResult(false, true)
            } catch (e: Exception) { Log.e(TAG, "Failed to start ACTION_CHANGE_LIVE_WALLPAPER intent: ${e.message}") }
            return@withContext WallpaperSetResult(false, false)
        } catch (e: Exception) { Log.e(TAG, "Error in setVideoWallpaper: ${e.message}"); WallpaperSetResult(false, false) }
    }
    private fun getSuccessMessage(target: WallpaperTarget): String {
        return when (target) {
            WallpaperTarget.HOME -> stringProvider.getString(R.string.home_screen_wallpaper_set)
            WallpaperTarget.LOCK -> stringProvider.getString(R.string.lock_screen_wallpaper_set)
            WallpaperTarget.BOTH -> stringProvider.getString(R.string.both_screens_wallpaper_set)
        }
    }
    fun getWallpapersDir(): File {
        val dir = File(context.filesDir, WALLPAPERS_DIR); if (!dir.exists()) { dir.mkdirs() }; return dir
    }
    fun saveBitmapToFile(bitmap: Bitmap, fileName: String): File? {
        return try {
            val wallpapersDir = getWallpapersDir(); val file = File(wallpapersDir, "$fileName.jpg")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out); out.flush() }
            file
        } catch (e: Exception) { Log.e(TAG, "保存壁纸到文件失败", e); null }
    }
    fun loadBitmapFromFile(fileName: String): Bitmap? {
        return try {
            val wallpapersDir = getWallpapersDir(); val file = File(wallpapersDir, "$fileName.jpg")
            if (!file.exists()) return null
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) { Log.e(TAG, "从文件加载壁纸失败", e); null }
    }
    fun downloadWallpaper(wallpaper: Wallpaper, downloadOriginalQuality: Boolean): Flow<Pair<Float, String?>> = channelFlow {
        try {
            send(Pair(0f, null))
            val progressCallback = object : DownloadProgressCallback { override fun onProgressUpdate(progress: Float) { trySend(Pair(progress, null)) } }
            val filePath = if (wallpaper.isLive) { downloadVideoFile(wallpaper, downloadOriginalQuality, progressCallback) }
            else { downloadImageFile(wallpaper, downloadOriginalQuality, progressCallback) }
            send(Pair(1f, filePath))
        } catch (e: Exception) { Log.e(TAG, "Download failed: ${e.message}"); e.printStackTrace(); send(Pair(-1f, null)); throw e }
    }
    private suspend fun downloadImageFile(
        wallpaper: Wallpaper, downloadOriginalQuality: Boolean, progressCallback: DownloadProgressCallback
    ): String {
        val directory = createDownloadDirectory("images")
        val fileName = "${sanitizeFileName(wallpaper.title)}_${System.currentTimeMillis()}.jpg"
        val targetFile = File(directory, fileName)
        val chosen = if (downloadOriginalQuality) {
            // 优先使用高清或原始链接，否则回退到主链接
            wallpaper.url ?: wallpaper.previewUrl ?: ""
        } else {
            // 优先使用预览/标清链接，否则回退到主链接
            wallpaper.previewUrl ?: wallpaper.url ?: ""
        }
        val url = URL(chosen)
        downloadFileWithProgress(url.toString(), targetFile, progressCallback)
        addImageToGallery(targetFile)
        return targetFile.absolutePath
    }
    private suspend fun downloadVideoFile(
        wallpaper: Wallpaper, downloadOriginalQuality: Boolean, progressCallback: DownloadProgressCallback
    ): String {
        val directory = createDownloadDirectory("videos")
        val fileName = "${sanitizeFileName(wallpaper.title)}_${System.currentTimeMillis()}.mp4"
        val targetFile = File(directory, fileName)
        val chosen = if (downloadOriginalQuality) {
            wallpaper.downloadHdUrl ?: wallpaper.downloadUrl ?: wallpaper.url ?: ""
        } else {
            wallpaper.downloadSdUrl ?: wallpaper.downloadUrl ?: wallpaper.url ?: ""
        }
        val url = URL(chosen)
        downloadFileWithProgress(url.toString(), targetFile, progressCallback)
        addVideoToGallery(targetFile)
        return targetFile.absolutePath
    }
    private fun createDownloadDirectory(type: String): File {
        val baseDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(baseDir, "Vistara/$type"); if (!dir.exists()) dir.mkdirs(); return dir
    }
    private fun sanitizeFileName(name: String?): String {
        return name?.replace(Regex("[^a-zA-Z0-9_\\-]"), "_") ?: "wallpaper"
    }
    private suspend fun downloadFileWithProgress(url: String, targetFile: File, progressCallback: DownloadProgressCallback) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Failed to download file: ${response.code}")
            val body = response.body ?: throw IOException("Empty response body")
            val contentLength = body.contentLength()
            var bytesReadTotal = 0L
            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        output.write(buffer, 0, bytesRead)
                        bytesReadTotal += bytesRead
                        if (contentLength > 0) {
                            val progress = bytesReadTotal.toFloat() / contentLength
                            progressCallback.onProgressUpdate(progress)
                        }
                    }
                    output.flush()
                }
            }
        }
    }
    private fun addImageToGallery(file: File) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.RELATIVE_PATH, "Download/Vistara/images")
                else put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.insert(contentUri, values)?.let { uri ->
                FileInputStream(file).use { input ->
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE); var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) { output.write(buffer, 0, bytesRead) }
                    }
                }
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to add image to gallery", e) }
    }
    private fun addVideoToGallery(file: File) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Video.Media.RELATIVE_PATH, "Download/Vistara/videos")
                else put(MediaStore.Video.Media.DATA, file.absolutePath)
            }
            val contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.insert(contentUri, values)?.let { uri ->
                FileInputStream(file).use { input ->
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE); var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) { output.write(buffer, 0, bytesRead) }
                    }
                }
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to add video to gallery", e) }
    }
    private fun sendWallpaperChangedNotification(wallpaper: Wallpaper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationUtil.showWallpaperChangedNotification(wallpaper)
            }
        } else { notificationUtil.showWallpaperChangedNotification(wallpaper) }
    }
    private suspend fun downloadFile(url: String, targetFile: File, progressCallback: DownloadProgressCallback) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Failed to download file: ${response.code}")
            val body = response.body ?: throw IOException("Empty response body")
            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    while (true) {
                        val bytesRead = input.read(buffer); if (bytesRead == -1) break
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }
        }
    }
}
