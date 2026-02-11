package com.obscura.wallpapers.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 视频播放调度管理器。
 *
 * 负责在列表/瀑布流等场景中，根据「可见区域」「滚动状态」「播放模式」
 * 自动控制哪些视频允许播放，从而：
 *
 * - 避免多视频同时播放造成性能压力
 * - 支持顺序播放 / 同时播放两种模式
 * - 在滚动时自动暂停，停止后恢复
 * - 支持视频播放完成后的自动切换
 *
 * 设计目标：
 * 1. 单一数据源：visibleVideoIds 作为当前可播放候选集
 * 2. playingVideoIds 作为 UI / Player 层的播放依据
 * 3. currentPlayingId 在顺序播放模式下作为游标
 *
 * 所有调度逻辑均运行在主线程协程作用域中，保证状态一致性。
 */
class VideoPlaybackManager {

    /** 主线程作用域，用于调度播放切换、滚动延迟等逻辑 */
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** 当前处于可见区域的视频 ID 集合 */
    private val visibleVideoIds = mutableSetOf<String>()

    /** 当前被允许播放的视频 ID 集合（UI / Player 层查询此状态） */
    private val playingVideoIds = mutableSetOf<String>()

    /** 顺序播放模式下，当前正在播放的视频 ID */
    private var currentPlayingId: String? = null

    /** 是否正在滚动中（滚动时强制停止播放） */
    private var isScrolling = false

    /** 是否启用顺序播放（true：一次只播放一个） */
    private var useSequentialPlayback = true

    /** 视频播放完成回调 */
    private var onVideoCompleteListener: ((String) -> Unit)? = null

    /** 滚动停止后的延迟恢复播放任务 */
    private var scrollStopTimer: Job? = null

    /** 上一次更新 visible 集合的时间，用于节流 */
    private var lastUpdateTime = 0L

    /** 可见区域更新最小间隔（毫秒） */
    private val UPDATE_INTERVAL = 150L

    /**
     * 添加一个进入可见区域的视频。
     *
     * 内部带节流控制，避免在快速滚动中频繁触发调度。
     *
     * @param id 视频唯一标识
     */
    fun addVisibleVideo(id: String) {
        if (System.currentTimeMillis() - lastUpdateTime < UPDATE_INTERVAL) return
        lastUpdateTime = System.currentTimeMillis()

        if (visibleVideoIds.add(id)) {
            updatePlayingVideos()
        }
    }

    /**
     * 移除一个离开可见区域的视频。
     *
     * 若该视频正是当前播放项，则会自动重新选择播放对象。
     *
     * @param id 视频唯一标识
     */
    fun removeVisibleVideo(id: String) {
        if (System.currentTimeMillis() - lastUpdateTime < UPDATE_INTERVAL) return
        lastUpdateTime = System.currentTimeMillis()

        visibleVideoIds.remove(id)
        playingVideoIds.remove(id)

        if (id == currentPlayingId) {
            currentPlayingId = null
            updatePlayingVideos()
        }
    }

    /**
     * 清空当前所有可见与播放状态。
     *
     * 常用于：
     * - 页面切换
     * - Activity/Fragment 暂停
     */
    fun clearVisibleVideos() {
        visibleVideoIds.clear()
        playingVideoIds.clear()
        currentPlayingId = null
    }

    /**
     * 根据当前状态重新计算可播放视频集合。
     *
     * 规则：
     * 1. 滚动中：不播放任何视频
     * 2. 无可见视频：不播放
     * 3. 顺序模式：只选择一个 currentPlayingId
     * 4. 并发模式：播放所有可见视频
     */
    private fun updatePlayingVideos() {
        if (isScrolling) {
            playingVideoIds.clear()
            currentPlayingId = null
            return
        }

        if (visibleVideoIds.isEmpty()) {
            playingVideoIds.clear()
            currentPlayingId = null
            return
        }

        if (useSequentialPlayback) {
            if (currentPlayingId == null || currentPlayingId !in visibleVideoIds) {
                currentPlayingId = visibleVideoIds.firstOrNull()
                playingVideoIds.clear()
                currentPlayingId?.let { playingVideoIds.add(it) }
            }
        } else {
            playingVideoIds.clear()
            playingVideoIds.addAll(visibleVideoIds)
        }
    }

    /**
     * 判断指定视频当前是否应当播放。
     *
     * @param id 视频唯一标识
     * @return true 表示应播放，false 表示应暂停
     */
    fun shouldPlayVideo(id: String): Boolean = id in playingVideoIds

    /**
     * 设置是否启用顺序播放模式。
     *
     * @param useSequential true 表示一次只播放一个视频
     */
    fun setSequentialPlayback(useSequential: Boolean) {
        if (useSequentialPlayback != useSequential) {
            useSequentialPlayback = useSequential
            updatePlayingVideos()
        }
    }

    /**
     * 设置视频播放完成监听器。
     *
     * @param listener 回调，参数为完成播放的视频 ID
     */
    fun setVideoCompleteListener(listener: (String) -> Unit) {
        onVideoCompleteListener = listener
    }

    /**
     * 通知管理器某个视频已播放完成。
     *
     * 在顺序播放模式下，会自动切换到下一个可见视频。
     *
     * @param videoId 已完成的视频 ID
     */
    fun notifyVideoComplete(videoId: String) {
        if (videoId != currentPlayingId) return

        scope.launch {
            currentPlayingId = null
            playingVideoIds.clear()
            onVideoCompleteListener?.invoke(videoId)

            if (useSequentialPlayback && visibleVideoIds.isNotEmpty()) {
                val currentIndex = visibleVideoIds.indexOf(videoId)
                currentPlayingId = when {
                    currentIndex != -1 && currentIndex < visibleVideoIds.size - 1 ->
                        visibleVideoIds.elementAt(currentIndex + 1)
                    else -> visibleVideoIds.firstOrNull()
                }
                currentPlayingId?.let { playingVideoIds.add(it) }
            } else {
                updatePlayingVideos()
            }
        }
    }

    /**
     * 设置滚动状态。
     *
     * - 滚动中：立即停止所有播放
     * - 停止滚动：延迟恢复播放，避免抖动
     *
     * @param scrolling true 表示正在滚动
     */
    fun setScrolling(scrolling: Boolean) {
        if (isScrolling == scrolling) return

        scope.launch {
            isScrolling = scrolling
            scrollStopTimer?.cancel()

            if (scrolling) {
                playingVideoIds.clear()
            } else {
                scrollStopTimer = scope.launch {
                    delay(300)
                    updatePlayingVideos()
                }
            }
        }
    }

    /** @return 当前是否处于滚动状态 */
    fun isScrolling(): Boolean = isScrolling

    /**
     * 释放资源，取消所有协程并清空状态。
     *
     * 必须在页面销毁时调用。
     */
    fun dispose() {
        scope.cancel()
        clearVisibleVideos()
    }
}

/**
 * 创建并记住一个 [VideoPlaybackManager] 实例，
 * 并与当前生命周期绑定。
 *
 * 行为：
 * - ON_PAUSE：清空播放状态
 * - ON_DESTROY：自动释放资源
 *
 * @return 与生命周期绑定的视频播放管理器
 */
@Composable
fun rememberVideoPlaybackManager(): VideoPlaybackManager {
    val lifecycleOwner = LocalLifecycleOwner.current
    val manager = remember { VideoPlaybackManager() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> manager.clearVisibleVideos()
                Lifecycle.Event.ON_DESTROY -> manager.dispose()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            manager.dispose()
        }
    }

    return manager
}
