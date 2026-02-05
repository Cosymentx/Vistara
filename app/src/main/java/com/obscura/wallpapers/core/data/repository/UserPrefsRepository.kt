package com.obscura.wallpapers.core.data.repository

import com.obscura.wallpapers.core.data.model.AutoChangeFrequency
import com.obscura.wallpapers.core.data.model.AutoChangeSource
import com.obscura.wallpapers.core.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface UserPrefsRepository {
    suspend fun getUserSettings(): UserSettings
    fun getUserSettingsFlow(): Flow<UserSettings>
    suspend fun saveUserSettings(settings: UserSettings)
    suspend fun updateAutoChangeSettings(
        enabled: Boolean? = null,
        frequency: AutoChangeFrequency? = null,
        wifiOnly: Boolean? = null,
        source: AutoChangeSource? = null,
        categoryId: String? = null
    )
    suspend fun updateNotificationSettings(
        showDownloadNotification: Boolean? = null,
        showWallpaperChangeNotification: Boolean? = null
    )
    suspend fun getSearchHistory(): List<String>
    suspend fun saveSearchHistory(history: List<String>)
    suspend fun clearSearchHistory()
    suspend fun setPendingLiveWallpaper(wallpaperId: String)
    suspend fun getPendingLiveWallpaper(): String?
    suspend fun clearPendingLiveWallpaper()
    suspend fun clearUserSettings()
}
