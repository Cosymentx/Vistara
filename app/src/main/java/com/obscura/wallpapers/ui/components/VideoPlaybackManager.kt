package com.obscura.wallpapers.ui.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

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
     * 释放资源，取消所有协程并清空状态。
     *
     * 必须在页面销毁时调用。
     */
    fun dispose() {
        scope.cancel()
        clearVisibleVideos()
    }
}