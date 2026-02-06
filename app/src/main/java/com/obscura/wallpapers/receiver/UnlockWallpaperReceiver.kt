package com.obscura.wallpapers.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.obscura.wallpapers.core.data.model.AutoChangeFrequency
import com.obscura.wallpapers.core.data.model.AutoChangeHistory
import com.obscura.wallpapers.core.data.model.AutoChangeSource
import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import com.obscura.wallpapers.core.data.repository.WallpaperRepository
import com.obscura.wallpapers.manager.AppWallpaperManager
import com.obscura.wallpapers.core.common.ConnectivityHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 监听【用户解锁屏幕】事件的广播接收器
 *
 * 当系统发送 [Intent.ACTION_USER_PRESENT]（用户解锁设备）时：
 * - 根据用户的自动更换壁纸配置
 * - 校验网络状态、频率规则
 * - 随机获取一张符合条件的壁纸
 * - 并自动设置到指定的屏幕（桌面 / 锁屏 / 双屏）
 *
 * 该接收器通过 Hilt 注入相关仓库与工具类，
 * 所有耗时操作均在 IO 协程线程中执行，避免阻塞主线程。
 */
@AndroidEntryPoint
class UnlockWallpaperReceiver : BroadcastReceiver() {

    /** 用户偏好设置仓库 */
    @Inject
    lateinit var userPrefsRepository: UserPrefsRepository

    /** 壁纸数据仓库（收藏 / 分类 / 下载 / 热门） */
    @Inject
    lateinit var wallpaperRepository: WallpaperRepository

    /** 网络连接检测工具 */
    @Inject
    lateinit var connectivityHelper: ConnectivityHelper

    /** 壁纸设置管理器（负责实际设置系统壁纸） */
    @Inject
    lateinit var appWallpaperManager: AppWallpaperManager

    companion object {
        private const val TAG = "UnlockWallpaperReceiver"
    }

    /**
     * 接收到系统广播时回调
     *
     * @param context 当前上下文
     * @param intent  系统广播 Intent
     */
    override fun onReceive(context: Context, intent: Intent) {

        // 仅处理【用户解锁设备】的广播
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d(TAG, "Screen unlocked, checking auto change settings")

            // 在 IO 线程中执行耗时逻辑（数据库 / 网络 / 文件操作）
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. 获取用户自动更换壁纸配置
                    val userSettings = userPrefsRepository.getUserSettings()
                    Log.d(
                        TAG,
                        "用户设置获取成功: autoChangeEnabled=${userSettings.autoChangeEnabled}, " +
                                "frequency=${userSettings.autoChangeFrequency}, " +
                                "source=${userSettings.autoChangeSource}, " +
                                "target=${userSettings.autoChangeTarget}, " +
                                "wifiOnly=${userSettings.autoChangeWifiOnly}"
                    )

                    // 2. 是否启用自动更换
                    if (!userSettings.autoChangeEnabled) {
                        Log.d(TAG, "自动更换壁纸功能未启用，不执行更换")
                        return@launch
                    }

                    // 3. 是否设置为“每次解锁更换”
                    if (userSettings.autoChangeFrequency != AutoChangeFrequency.EACH_UNLOCK) {
                        Log.d(
                            TAG,
                            "自动更换频率不是每次解锁，当前设置为: ${userSettings.autoChangeFrequency}"
                        )
                        return@launch
                    }

                    // 4. 校验 WiFi 网络（如果用户开启了仅 WiFi）
                    val wifiConnected = connectivityHelper.hasWifi()
                    Log.d(
                        TAG,
                        "WiFi连接状态: $wifiConnected, 是否需要WiFi: ${userSettings.autoChangeWifiOnly}"
                    )
                    if (userSettings.autoChangeWifiOnly && !wifiConnected) {
                        Log.d(TAG, "需要WiFi连接但当前未连接，不执行更换")
                        return@launch
                    }

                    Log.d(TAG, "所有检查通过，开始获取壁纸")

                    // 5. 根据用户设置的来源随机获取壁纸
                    val wallpaper = when (userSettings.autoChangeSource) {

                        // 从收藏中随机获取
                        AutoChangeSource.FAVORITES -> {
                            Log.d(TAG, "从收藏中随机获取壁纸")
                            wallpaperRepository.getRandomFavoriteWallpaper()
                        }

                        // 从指定分类中随机获取
                        AutoChangeSource.CATEGORY -> {
                            val categoryId = userSettings.autoChangeCategory
                            Log.d(TAG, "从分类中随机获取壁纸，分类ID: $categoryId")
                            categoryId?.let { id ->
                                wallpaperRepository.getRandomWallpaperByCategory(id)
                            } ?: run {
                                Log.d(TAG, "分类ID为空，使用随机壁纸")
                                wallpaperRepository.getRandomWallpaper()
                            }
                        }

                        // 从下载记录中随机获取
                        AutoChangeSource.DOWNLOADED -> {
                            Log.d(TAG, "从下载中随机获取壁纸")
                            wallpaperRepository.getRandomDownloadedWallpaper()
                        }

                        // 从热门壁纸中随机获取
                        AutoChangeSource.TRENDING -> {
                            Log.d(TAG, "从热门壁纸中随机获取")
                            wallpaperRepository.getRandomTrendingWallpaper()
                        }

                        // 默认：随机壁纸
                        else -> {
                            Log.d(TAG, "使用默认来源，从随机壁纸中获取")
                            wallpaperRepository.getRandomWallpaper()
                        }
                    }

                    // 6. 未获取到任何壁纸则终止
                    if (wallpaper == null) {
                        Log.e(TAG, "未找到可设置的壁纸，取消操作")
                        return@launch
                    }

                    Log.d(TAG, "成功获取壁纸: id=${wallpaper.id}, title=${wallpaper.title}")

                    // 7. 记录自动更换历史
                    val history = AutoChangeHistory(
                        wallpaperId = wallpaper.id,
                        wallpaperUrl = wallpaper.url ?: "",
                        success = true,
                        targetScreen = userSettings.autoChangeTarget.name
                    )
                    wallpaperRepository.recordAutoChangeHistory(history)

                    // 8. 设置壁纸到系统
                    Log.d(TAG, "开始设置壁纸，目标屏幕: ${userSettings.autoChangeTarget}")
                    appWallpaperManager.setWallpaper(
                        wallpaper,
                        target = userSettings.autoChangeTarget
                    ) { success ->
                        if (success) {
                            Log.d(TAG, "壁纸设置成功")
                        } else {
                            Log.e(TAG, "壁纸设置失败")
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "自动更换壁纸过程中出错: ${e.message}", e)
                }
            }
        }
    }
}

