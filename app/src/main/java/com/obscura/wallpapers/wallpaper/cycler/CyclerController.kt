package com.obscura.wallpapers.wallpaper.cycler

import com.obscura.wallpapers.core.common.ConnectivityHelper
import com.obscura.wallpapers.core.data.model.AutoChangeSource
import com.obscura.wallpapers.core.data.model.AutoChangeHistory
import com.obscura.wallpapers.core.data.model.WallpaperTarget
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import com.obscura.wallpapers.core.data.repository.WallpaperRepository
import com.obscura.wallpapers.wallpaper.core.AppWallpaperManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CyclerController @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val connectivityHelper: ConnectivityHelper,
    private val appWallpaperManager: AppWallpaperManager,
) {
    suspend fun performAutoChange(): Boolean = withContext(Dispatchers.IO) {
        val settings = userPrefsRepository.getUserSettings()
        if (!settings.autoChangeEnabled) return@withContext false
        if (settings.autoChangeWifiOnly && !connectivityHelper.hasWifi()) return@withContext false

        val wallpaper = when (settings.autoChangeSource) {
            AutoChangeSource.FAVORITES -> wallpaperRepository.getRandomFavoriteWallpaper()
            AutoChangeSource.CATEGORY -> {
                settings.autoChangeCategory?.let { categoryId ->
                    wallpaperRepository.getRandomWallpaperByCategory(categoryId)
                } ?: wallpaperRepository.getRandomWallpaper()
            }
            AutoChangeSource.DOWNLOADED -> wallpaperRepository.getRandomDownloadedWallpaper()
            AutoChangeSource.TRENDING -> wallpaperRepository.getRandomTrendingWallpaper()
            else -> wallpaperRepository.getRandomWallpaper()
        } ?: return@withContext false

        val history = AutoChangeHistory(
            wallpaperId = wallpaper.id,
            wallpaperUrl = wallpaper.url ?: "",
            targetScreen = settings.autoChangeTarget?.name
        )
        wallpaperRepository.recordAutoChangeHistory(history)

        appWallpaperManager.setWallpaper(
            wallpaper,
            target = settings.autoChangeTarget ?: WallpaperTarget.BOTH,
            onComplete = {}
        )
        true
    }
}
