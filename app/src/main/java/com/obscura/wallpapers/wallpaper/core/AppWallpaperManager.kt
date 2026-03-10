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
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri

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
                Toast.makeText(activity, stringProvider.getString(R.string.wallpaper_set_failed), Toast.LENGTH_SHORT).show()
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

    suspend fun shareWallpaper(activity: Activity, wallpaper: Wallpaper, editedBitmap: Bitmap? = null) {
        try {
            val file = if (wallpaper.isLive) {
                // 对于动态壁纸，分享下载链接或本地视频文件
                findCachedVideoByUrl(activity, wallpaper.url ?: "")
            } else {
                // 对于静态壁纸，保存并获取文件对象
                val bitmap = editedBitmap ?: loadBitmapFromFile(wallpaper.id) ?: withContext(Dispatchers.IO) {
                    val url = URL(wallpaper.url); BitmapFactory.decodeStream(url.openStream())
                }
                saveBitmapToFile(bitmap, wallpaper.id)
            }

            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    if (file != null && file.exists()) {
                        val uri = FileProvider.getUriForFile(
                            activity,
                            "${activity.packageName}.fileprovider",
                            file
                        )
                        // 设置 ClipData 是为了在 Android 10+ 更好地传递 URI 权限
                        clipData = android.content.ClipData.newRawUri("", uri)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = if (wallpaper.isLive) "video/*" else "image/*"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        // 如果没有文件，分享文本链接
                        type = "text/plain"
                        val shareText = "${wallpaper.title ?: "Wallpaper"}\n${wallpaper.url ?: ""}"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    putExtra(Intent.EXTRA_SUBJECT, wallpaper.title ?: "Wallpaper")
                }
                val chooser = Intent.createChooser(intent, "分享壁纸")
                // 确保 Chooser 也能获取权限
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                activity.startActivity(chooser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing wallpaper", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, "分享失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private suspend fun setLiveWallpaper(
        activity: Activity, wallpaper: Wallpaper, target: WallpaperTarget, onComplete: (Boolean) -> Unit
    ) {
        try {
            val videoUrl = wallpaper.url
            if (videoUrl.isNullOrEmpty()) {
                Log.e(TAG, "Video URL is null or empty")
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, stringProvider.getString(R.string.wallpaper_invalid_video_url), Toast.LENGTH_SHORT).show()
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
                    val bitmap = createBitmap(1, 1)
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
            LiveWallpaperService.setVideoUri(activity, videoUrl.toUri())
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
                    Toast.makeText(activity, stringProvider.getString(R.string.wallpaper_set_failed), Toast.LENGTH_SHORT).show()
                }
                onComplete(result.anyMethodSucceeded)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting live wallpaper", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, stringProvider.getString(R.string.wallpaper_set_failed), Toast.LENGTH_SHORT).show()
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
            WallpaperTarget.HOME -> stringProvider.getString(R.string.wallpaper_set_home_success)
            WallpaperTarget.LOCK -> stringProvider.getString(R.string.wallpaper_set_lock_success)
            WallpaperTarget.BOTH -> stringProvider.getString(R.string.wallpaper_set_both_success)
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
            
            // 增加下载完成通知
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationUtil.showDownloadCompleteNotification(wallpaper, filePath)
                }
            } else {
                notificationUtil.showDownloadCompleteNotification(wallpaper, filePath)
            }

            send(Pair(1f, filePath))
        } catch (e: Exception) { Log.e(TAG, "Download failed: ${e.message}"); e.printStackTrace(); send(Pair(-1f, null)); throw e }
    }
    private suspend fun downloadImageFile(
        wallpaper: Wallpaper, downloadOriginalQuality: Boolean, progressCallback: DownloadProgressCallback
    ): String {
        val chosen = if (downloadOriginalQuality) {
            wallpaper.url ?: wallpaper.previewUrl ?: ""
        } else {
            wallpaper.previewUrl ?: wallpaper.url ?: ""
        }
        val url = URL(chosen).toString()
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", "Obscura/1.0")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Failed to download image: ${response.code}")
            val body = response.body ?: throw IOException("Empty image response body")
            val contentType = body.contentType()?.toString()
            val ext = when {
                contentType?.contains("png", ignoreCase = true) == true -> "png"
                contentType?.contains("webp", ignoreCase = true) == true -> "webp"
                else -> "jpg"
            }
            val displayName = "${sanitizeFileName(wallpaper.title)}_${System.currentTimeMillis()}.$ext"
            val mime = when (ext) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
            val contentLength = body.contentLength()
            var bytesReadTotal = 0L
            val rawBytes = ByteArrayOutputStream().use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        out.write(buffer, 0, bytesRead)
                        bytesReadTotal += bytesRead
                        if (contentLength > 0) {
                            val progress = bytesReadTotal.toFloat() / contentLength
                            progressCallback.onProgressUpdate(progress)
                        }
                    }
                }
                out.toByteArray()
            }
            val decodedBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Obscura")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                    if (decodedBitmap != null) {
                        put(MediaStore.Images.Media.WIDTH, decodedBitmap.width)
                        put(MediaStore.Images.Media.HEIGHT, decodedBitmap.height)
                    }
                }
                val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val uri = context.contentResolver.insert(contentUri, values)
                    ?: throw IOException("Failed to create image media entry")
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    if (decodedBitmap != null) {
                        when (ext) {
                            "png" -> decodedBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                            "webp" -> decodedBitmap.compress(Bitmap.CompressFormat.WEBP, 100, output)
                            else -> decodedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
                        }
                    } else {
                        output.write(rawBytes)
                    }
                    output.flush()
                } ?: throw IOException("Failed to open output stream for image")
                val finalize = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                context.contentResolver.update(uri, finalize, null, null)
                uri.toString()
            } else {
                val baseOld = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val dir = File(baseOld, "Obscura/images"); if (!dir.exists()) dir.mkdirs()
                val targetFile = File(dir, displayName)
                FileOutputStream(targetFile).use { output ->
                    if (decodedBitmap != null) {
                        when (ext) {
                            "png" -> decodedBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                            "webp" -> decodedBitmap.compress(Bitmap.CompressFormat.WEBP, 100, output)
                            else -> decodedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
                        }
                    } else {
                        output.write(rawBytes)
                    }
                    output.flush()
                }
                addImageToGallery(targetFile)
                targetFile.absolutePath
            }
        }
    }
    private suspend fun downloadVideoFile(
        wallpaper: Wallpaper, downloadOriginalQuality: Boolean, progressCallback: DownloadProgressCallback
    ): String {
        val chosen = if (downloadOriginalQuality) {
            wallpaper.downloadHdUrl ?: wallpaper.downloadUrl ?: wallpaper.url ?: ""
        } else {
            wallpaper.downloadSdUrl ?: wallpaper.downloadUrl ?: wallpaper.url ?: ""
        }
        val url = URL(chosen).toString()
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", "Obscura/1.0")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Failed to download video: ${response.code}")
            val body = response.body ?: throw IOException("Empty video response body")
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "${sanitizeFileName(wallpaper.title)}_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Obscura")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                } else {
                    // 对于旧版本，仍写入公共存储路径
                    val baseOld = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val oldFile = File(File(baseOld, "Obscura/videos"), "${sanitizeFileName(wallpaper.title)}_${System.currentTimeMillis()}.mp4")
                    oldFile.parentFile?.mkdirs()
                    put(MediaStore.Video.Media.DATA, oldFile.absolutePath)
                }
            }
            val contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val uri = context.contentResolver.insert(contentUri, values)
                ?: throw IOException("Failed to create video media entry")
            val contentLength = body.contentLength()
            var bytesReadTotal = 0L
            body.byteStream().use { input ->
                context.contentResolver.openOutputStream(uri)?.use { output ->
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
                } ?: throw IOException("Failed to open output stream for video")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val finalize = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                context.contentResolver.update(uri, finalize, null, null)
            }
            uri.toString()
        }
    }
    private fun createDownloadDirectory(type: String): File {
        val baseDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(baseDir, "Obscura/$type"); if (!dir.exists()) dir.mkdirs(); return dir
    }
    private fun sanitizeFileName(name: String?): String {
        return name?.replace(Regex("[^a-zA-Z0-9_\\-]"), "_") ?: "wallpaper"
    }
    private suspend fun downloadFileWithProgress(url: String, targetFile: File, progressCallback: DownloadProgressCallback) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", "Obscura/1.0")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Failed to download file: ${response.code}")
            val body = response.body
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
                val mime = when {
                    file.name.endsWith(".png", true) -> "image/png"
                    file.name.endsWith(".webp", true) -> "image/webp"
                    else -> "image/jpeg"
                }
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Obscura")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                } else {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                }
            }
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.insert(contentUri, values)?.let { uri ->
                FileInputStream(file).use { input ->
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE); var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) { output.write(buffer, 0, bytesRead) }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val finalize = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                    context.contentResolver.update(uri, finalize, null, null)
                }
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to add image to gallery", e) }
    }
    private fun addVideoToGallery(file: File) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Obscura")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                } else {
                    put(MediaStore.Video.Media.DATA, file.absolutePath)
                }
            }
            val contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.insert(contentUri, values)?.let { uri ->
                FileInputStream(file).use { input ->
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE); var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) { output.write(buffer, 0, bytesRead) }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val finalize = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                    context.contentResolver.update(uri, finalize, null, null)
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
            val request = Request.Builder().url(url)
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", "Obscura/1.0")
                .build()
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
