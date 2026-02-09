package com.obscura.wallpapers.core.common

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import com.obscura.wallpapers.ui.EntryActivity
import kotlinx.coroutines.runBlocking
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefsRepository: UserPrefsRepository
) {
    companion object {
        private const val CHANNEL_ID_WALLPAPER = "wallpaper_channel"
        private const val CHANNEL_ID_DOWNLOAD = "download_channel"
        private const val CHANNEL_ID_LIVE_WALLPAPER = "live_wallpaper_channel"

        private const val NOTIFICATION_ID_WALLPAPER_CHANGED = 1001
        private const val NOTIFICATION_ID_DOWNLOAD_PROGRESS = 1002
        private const val NOTIFICATION_ID_DOWNLOAD_COMPLETE = 1003
        private const val NOTIFICATION_ID_LIVE_WALLPAPER_PENDING = 1004
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val wallpaperChannel = NotificationChannel(
                CHANNEL_ID_WALLPAPER,
                context.getString(R.string.notification_channel_wallpaper),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_wallpaper_desc)
                setShowBadge(false)
            }

            val downloadChannel = NotificationChannel(
                CHANNEL_ID_DOWNLOAD,
                context.getString(R.string.notification_channel_download),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_download_desc)
                setShowBadge(true)
            }

            val liveWallpaperChannel = NotificationChannel(
                CHANNEL_ID_LIVE_WALLPAPER,
                context.getString(R.string.notification_channel_live_wallpaper),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_live_wallpaper_desc)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(listOf(wallpaperChannel, downloadChannel, liveWallpaperChannel))
        }
    }

    suspend fun shouldShowWallpaperChangedNotification(): Boolean {
        return userPrefsRepository.getUserSettings().showWallpaperChangeNotification
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showWallpaperChangedNotification(wallpaper: com.obscura.wallpapers.core.data.model.Wallpaper) {
        val intent = Intent(context, EntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_WALLPAPER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_wallpaper_changed_title))
            .setContentText(wallpaper.title ?: context.getString(R.string.notification_wallpaper_changed_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_WALLPAPER_CHANGED, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showDownloadProgressNotification(wallpaper: com.obscura.wallpapers.core.data.model.Wallpaper, progress: Int) {
        val showNotification = runBlocking { userPrefsRepository.getUserSettings().showDownloadNotification }
        if (!showNotification) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DOWNLOAD)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(context.getString(R.string.notification_download_progress_title))
            .setContentText(wallpaper.title ?: context.getString(R.string.notification_download_progress_text))
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DOWNLOAD_PROGRESS, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showDownloadCompleteNotification(wallpaper: com.obscura.wallpapers.core.data.model.Wallpaper, filePath: String) {
        val showNotification = runBlocking { userPrefsRepository.getUserSettings().showDownloadNotification }
        if (!showNotification) {
            return
        }
        val intent = Intent(context, EntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("file_path", filePath)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DOWNLOAD)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(context.getString(R.string.notification_download_complete_title))
            .setContentText(wallpaper.title ?: context.getString(R.string.notification_download_complete_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_DOWNLOAD_PROGRESS)
        notificationManager.notify(NOTIFICATION_ID_DOWNLOAD_COMPLETE, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showLiveWallpaperPendingNotification(wallpaper: com.obscura.wallpapers.core.data.model.Wallpaper, pendingIntent: PendingIntent) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_LIVE_WALLPAPER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_live_wallpaper_pending_title))
            .setContentText(context.getString(R.string.notification_live_wallpaper_pending_text, wallpaper.title ?: ""))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_LIVE_WALLPAPER_PENDING, notification)
    }

    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun apiCancelAll() {
        println("apiCancelAll")
        cancelAllNotifications()
    }
    fun apiNotifyWallpaperChanged(wallpaper: com.obscura.wallpapers.core.data.model.Wallpaper) {
        println("apiNotifyWallpaperChanged")
        if (canPostNotifications()) {
            try {
                showWallpaperChangedNotification(wallpaper)
            } catch (e: SecurityException) {
            }
        }
    }
    fun apiNotifyDownloadProgress(wallpaper: com.obscura.wallpapers.core.data.model.Wallpaper, progress: Int) {
        println("apiNotifyDownloadProgress")
        if (canPostNotifications()) {
            try {
                showDownloadProgressNotification(wallpaper, progress)
            } catch (e: SecurityException) {
            }
        }
    }
    fun apiNotifyDownloadComplete(wallpaper: com.obscura.wallpapers.core.data.model.Wallpaper, filePath: String) {
        println("apiNotifyDownloadComplete")
        if (canPostNotifications()) {
            try {
                showDownloadCompleteNotification(wallpaper, filePath)
            } catch (e: SecurityException) {
            }
        }
    }
    fun apiNotifyLivePending(wallpaper: com.obscura.wallpapers.core.data.model.Wallpaper, pendingIntent: PendingIntent) {
        println("apiNotifyLivePending")
        if (canPostNotifications()) {
            try {
                showLiveWallpaperPendingNotification(wallpaper, pendingIntent)
            } catch (e: SecurityException) {
            }
        }
    }
    fun apiEnsureChannels() {
        println("apiEnsureChannels")
        createNotificationChannels()
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
