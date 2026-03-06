package com.obscura.wallpapers.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.ui.theme.stringResource
import kotlinx.coroutines.flow.collectLatest

/**
 * 瀑布流壁纸网格组件
 * 使用LazyVerticalStaggeredGrid实现瀑布流布局
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WallpaperStaggeredGrid(
    wallpapers: List<Wallpaper>,
    onWallpaperClick: (Wallpaper) -> Unit,
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    canLoadMore: Boolean = true,
    showEndMessage: Boolean = false,
    isPremiumUser: Boolean = false,
    purchasedIds: Set<String> = emptySet(),
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    videoPlaybackManager: VideoPlaybackManager? = null,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {

    val visibleWallpaperIds = remember { mutableStateListOf<String>() }

    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex > 0 && lastVisibleItemIndex >= totalItemsNumber - 5
        }
    }

    LaunchedEffect(shouldLoadMore.value, isLoadingMore, canLoadMore) {
        if (shouldLoadMore.value && !isLoadingMore && canLoadMore) {
            onLoadMore()
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo }.collectLatest { visibleItems ->
            val oldVisibleIds = visibleWallpaperIds.toList()
            visibleWallpaperIds.clear()
            val newVisibleIds = visibleItems.mapNotNull { info ->
                wallpapers.getOrNull(info.index)?.id
            }
            visibleWallpaperIds.addAll(newVisibleIds)

            videoPlaybackManager?.let { manager ->
                oldVisibleIds.forEach { id ->
                    if (id !in newVisibleIds) {
                        manager.removeVisibleVideo(id)
                    }
                }
                newVisibleIds.forEach { id ->
                    val wallpaper = wallpapers.find { it.id == id }
                    if (wallpaper?.isLive == true) {
                        manager.addVisibleVideo(id)
                    }
                }
            }
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        state = gridState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        items(items = wallpapers, key = { wallpaper -> wallpaper.id }) { wallpaper ->
            val itemHeight = remember(wallpaper.id) {
                val baseHeight = 180
                val randomVariation = (wallpaper.id.hashCode() % 120).let { if (it < 0) -it else it }
                val calculatedHeight = baseHeight + randomVariation
                // 确保高度至少为 220dp 以获得更好的视觉效果
                maxOf(calculatedHeight, 220).dp
            }

            WallpaperItem(
                wallpaper = wallpaper,
                onClick = { onWallpaperClick(wallpaper) },
                isPremiumUser = isPremiumUser,
                isWallpaperPurchased = purchasedIds.contains(wallpaper.id),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }

        if (isLoadingMore) {
            val loadingWallpapers = List(columns) { index ->
                Wallpaper(id = "loading_$index", title = null, thumbnailUrl = null, author = null)
            }
            items(items = loadingWallpapers, key = { it.id }) { _ ->
                Box(modifier = Modifier.height(0.dp)) {}
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            }
        }

        if (showEndMessage) {
            val endSpacerWallpapers = List(columns) { index ->
                Wallpaper(id = "end_spacer_$index", title = null, thumbnailUrl = null, author = null)
            }
            items(items = endSpacerWallpapers, key = { it.id }) { _ ->
                Box(modifier = Modifier.height(0.dp)) {}
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.common_end_of_list),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun calculateAspectRatio(wallpaper: Wallpaper): Float {
    return if (wallpaper.width > 0 && wallpaper.height > 0) {
        wallpaper.width.toFloat() / wallpaper.height.toFloat()
    } else if (wallpaper.resolution != null && wallpaper.resolution.width > 0 && wallpaper.resolution.height > 0) {
        wallpaper.resolution.width.toFloat() / wallpaper.resolution.height.toFloat()
    } else {
        0.75f
    }
}
