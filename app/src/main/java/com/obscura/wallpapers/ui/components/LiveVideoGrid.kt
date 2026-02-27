package com.obscura.wallpapers.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.ui.theme.stringResource
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private const val GRID_COLUMNS = 2
private const val TAG = "LiveVideoGrid"
private const val LOAD_MORE_THRESHOLD = GRID_COLUMNS * 2

/**
 * Displays live wallpapers in a grid. Playback control is handled by the parent.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun WallpaperVideoGrid(
    wallpapers: List<Wallpaper>,
    onWallpaperClick: (Wallpaper) -> Unit,
    gridState: LazyGridState,
    exoPlayer: ExoPlayer,
    playingIndex: Int,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {

    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        state = gridState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        itemsIndexed(
            items = wallpapers,
            key = { index, wallpaper -> "${index}_${wallpaper.id}" }
        ) { index, wallpaper ->

            val isCurrentlyPlaying = index == playingIndex

            LaunchedEffect(gridState, wallpapers.size, canLoadMore, isLoadingMore) {
                snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
                    .map { visibleItems -> visibleItems.lastOrNull()?.index ?: -1 }
                    .distinctUntilChanged()
                    .filter { lastVisibleIndex ->
                        lastVisibleIndex != -1 &&
                                lastVisibleIndex >= wallpapers.size - 1 - LOAD_MORE_THRESHOLD &&
                                canLoadMore &&
                                !isLoadingMore
                    }
                    .collect {
                        onLoadMore()
                    }
            }

            VideoPreviewItem(
                wallpaper = wallpaper,
                exoPlayer = exoPlayer,
                isCurrentlyPlaying = isCurrentlyPlaying,
                onClick = { onWallpaperClick(wallpaper) },
                modifier = Modifier
                    .aspectRatio(8f / 12f)
                    .animateItem(),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }

        if (isLoadingMore || (!canLoadMore && wallpapers.isNotEmpty())) {
            item(span = { GridItemSpan(GRID_COLUMNS) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    } else {
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
}
