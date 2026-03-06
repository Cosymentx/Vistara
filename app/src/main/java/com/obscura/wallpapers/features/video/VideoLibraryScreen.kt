package com.obscura.wallpapers.features.video

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.ui.components.CategorySelector
import com.obscura.wallpapers.ui.components.ErrorState
import com.obscura.wallpapers.ui.components.HomeTabScaffold
import com.obscura.wallpapers.ui.components.WallpaperVideoGrid
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.theme.stringResource
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun VideoLibraryScreen(
    onWallpaperClick: (Wallpaper) -> Unit,
    onSearchClick: () -> Unit = {},
    viewModel: VideoLibraryViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val wallpapersUiState by viewModel.wallpapersState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories = viewModel.categories
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val canLoadMore by viewModel.canLoadMore.collectAsState()

    val exoPlayer = rememberExoPlayerInstance(context)
    var playingIndex by remember { mutableIntStateOf(-1) }
    var wasPausedForLifecycle by remember { mutableStateOf(false) }
    val gridStates = categories.associateWith { rememberLazyGridState() }
    val gridState = gridStates[selectedCategory] ?: rememberLazyGridState()

    val currentWallpapers = remember(wallpapersUiState) {
        when (val state = wallpapersUiState) {
            is UiState.Success -> state.data
            else -> emptyList()
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (exoPlayer.isPlaying) {
                        wasPausedForLifecycle = true
                        exoPlayer.pause()
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (wasPausedForLifecycle && playingIndex != -1 && !gridState.isScrollInProgress) {
                        exoPlayer.playWhenReady = true
                    }
                    wasPausedForLifecycle = false
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
    }

    LaunchedEffect(gridState, currentWallpapers.size) {
        snapshotFlow { gridState.isScrollInProgress }.distinctUntilChanged()
            .collect { isScrolling ->
                if (isScrolling) {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    }
                } else {
                    if (wasPausedForLifecycle) return@collect
                    val visibleItemsInfo = gridState.layoutInfo.visibleItemsInfo
                    if (visibleItemsInfo.isNotEmpty()) {
                        val bestVisibleIndex =
                            findBestVisibleItemToPlay(visibleItemsInfo, gridState.layoutInfo)
                        if (bestVisibleIndex != -1 && bestVisibleIndex < currentWallpapers.size) {
                            if (bestVisibleIndex != playingIndex || (!exoPlayer.isPlaying && exoPlayer.playbackState == Player.STATE_IDLE)) {
                                val videoToPlay = currentWallpapers[bestVisibleIndex]
                                if (videoToPlay.url != null) {
                                    playingIndex = bestVisibleIndex
                                    exoPlayer.setMediaItem(
                                        MediaItem.Builder().setUri(Uri.parse(videoToPlay.url))
                                            .setMediaId(videoToPlay.url).build()
                                    )
                                    exoPlayer.prepare()
                                    exoPlayer.playWhenReady = true
                                } else {
                                    if (playingIndex == bestVisibleIndex) playingIndex = -1
                                }
                            } else if (bestVisibleIndex == playingIndex && !exoPlayer.isPlaying && exoPlayer.playbackState != Player.STATE_ENDED) {
                                exoPlayer.playWhenReady = true
                            }
                        } else {
                            playingIndex = -1
                            exoPlayer.stop()
                        }
                    } else {
                        playingIndex = -1
                        exoPlayer.stop()
                    }
                }
            }
    }

    HomeTabScaffold(
        title = stringResource(R.string.nav_video),
        showTopBar = true,
        headerExtra = {
            CategorySelector(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    playingIndex = -1
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    viewModel.filterByCategory(category)
                })
//            Spacer(Modifier.height(8.dp))
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = null)
            }
        }
    ) { _, contentModifier ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = {
                playingIndex = -1
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                viewModel.refresh()
            }
        )
        Box(
            modifier = contentModifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pullRefresh(pullRefreshState)
        ) {
            when (val state = wallpapersUiState) {
                is UiState.Loading -> LoadingState()
                is UiState.Success, is UiState.Error -> {
                    LiveGridContent(
                        wallpapers = currentWallpapers,
                        onWallpaperClick = onWallpaperClick,
                        gridState = gridState,
                        exoPlayer = exoPlayer,
                        playingIndex = playingIndex,
                        isLoadingMore = isLoadingMore,
                        canLoadMore = canLoadMore,
                        onLoadMore = { viewModel.loadMore() },
                        modifier = Modifier.fillMaxSize(),
                        // Content padding unified: 16dp horizontal
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 150.dp,
                            bottom = 100.dp
                        ),
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )

                    if (state is UiState.Error && currentWallpapers.isEmpty()) {
                        ErrorState(message = state.message, onRetry = { viewModel.refresh() })
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 160.dp)
            )
        }
    }
}

@Composable
fun rememberExoPlayerInstance(context: Context): ExoPlayer {
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 0f
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    return exoPlayer
}

fun findBestVisibleItemToPlay(
    visibleItems: List<LazyGridItemInfo>, layoutInfo: LazyGridLayoutInfo
): Int {
    if (visibleItems.isEmpty()) return -1
    val viewportStartY = 0
    val candidate = visibleItems.filter { item ->
        val itemStartY = item.offset.y
        val itemEndY = itemStartY + item.size.height
        itemEndY > viewportStartY && itemStartY < layoutInfo.viewportSize.height
    }.minByOrNull { item ->
        abs(item.offset.y - viewportStartY)
    }
    return candidate?.index ?: -1
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun LiveGridContent(
    wallpapers: List<Wallpaper>,
    onWallpaperClick: (Wallpaper) -> Unit,
    gridState: LazyGridState,
    exoPlayer: ExoPlayer,
    playingIndex: Int,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    if (wallpapers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.home_no_wallpapers_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        WallpaperVideoGrid(
            wallpapers = wallpapers,
            onWallpaperClick = onWallpaperClick,
            gridState = gridState,
            exoPlayer = exoPlayer,
            playingIndex = playingIndex,
            isLoadingMore = isLoadingMore,
            canLoadMore = canLoadMore,
            onLoadMore = onLoadMore,
            modifier = modifier,
            contentPadding = contentPadding,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}
