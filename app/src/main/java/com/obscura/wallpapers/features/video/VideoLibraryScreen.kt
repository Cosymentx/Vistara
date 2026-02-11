package com.obscura.wallpapers.features.video

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.ui.components.CategorySelector
import com.obscura.wallpapers.ui.components.HomeTabScaffold
import com.obscura.wallpapers.ui.components.LiveVideoGrid
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.components.ErrorState
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.theme.stringResource
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.math.abs


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalSharedTransitionApi::class)
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

    val snackbarHostState = remember { SnackbarHostState() }

    val exoPlayer = rememberExoPlayerInstance(context)
    var playingIndex by remember { mutableIntStateOf(-1) }
    var wasPausedForLifecycle by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    val currentWallpapers = remember(wallpapersUiState) {
        when (wallpapersUiState) {
            is UiState.Success -> (wallpapersUiState as UiState.Success<List<Wallpaper>>).data
            is UiState.Error -> emptyList()
            is UiState.Loading -> {
                (wallpapersUiState as? UiState.Success<List<Wallpaper>>)?.data ?: emptyList()
            }
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

    DisposableEffect(exoPlayer, currentWallpapers.size, gridState) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && !gridState.isScrollInProgress && !wasPausedForLifecycle) {
                    val visibleItemsInfo = gridState.layoutInfo.visibleItemsInfo
                    if (visibleItemsInfo.isEmpty() || playingIndex == -1) {
                        playingIndex = -1
                        return
                    }
                    val currentVisibleItem = visibleItemsInfo.find { it.index == playingIndex }
                    val currentVisibleListIndex = visibleItemsInfo.indexOf(currentVisibleItem)
                    if (currentVisibleListIndex != -1) {
                        val nextVisibleListIndex = (currentVisibleListIndex + 1) % visibleItemsInfo.size
                        val nextVisibleItemInfo = visibleItemsInfo[nextVisibleListIndex]
                        val nextPlayingIndex = nextVisibleItemInfo.index
                        if (nextPlayingIndex < currentWallpapers.size) {
                            val nextVideo = currentWallpapers[nextPlayingIndex]
                            if (nextVideo.url != null) {
                                playingIndex = nextPlayingIndex
                                exoPlayer.setMediaItem(
                                    MediaItem.Builder().setUri(Uri.parse(nextVideo.url))
                                        .setMediaId(nextVideo.url).build()
                                )
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            } else {
                                playingIndex = -1
                            }
                        } else {
                            playingIndex = -1
                        }
                    } else {
                        playingIndex = -1
                    }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                playingIndex = -1
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(gridState, currentWallpapers.size) {
        snapshotFlow { gridState.isScrollInProgress }.distinctUntilChanged().collect { isScrolling ->
            if (isScrolling) {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                }
            } else {
                if (wasPausedForLifecycle) return@collect
                val visibleItemsInfo = gridState.layoutInfo.visibleItemsInfo
                if (visibleItemsInfo.isNotEmpty()) {
                    val bestVisibleIndex = findBestVisibleItemToPlay(visibleItemsInfo, gridState.layoutInfo)
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

    LaunchedEffect(currentWallpapers, gridState) {
        if (currentWallpapers.isNotEmpty()) {
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
                .filter { it.isNotEmpty() && !gridState.isScrollInProgress && playingIndex == -1 && !wasPausedForLifecycle }
                .map { findBestVisibleItemToPlay(it, gridState.layoutInfo) }
                .distinctUntilChanged()
                .collect { bestVisibleIndex ->
                    if (bestVisibleIndex != -1 && bestVisibleIndex < currentWallpapers.size) {
                        val videoToPlay = currentWallpapers[bestVisibleIndex]
                        if (videoToPlay.url != null) {
                            playingIndex = bestVisibleIndex
                            exoPlayer.setMediaItem(
                                MediaItem.Builder().setUri(Uri.parse(videoToPlay.url))
                                    .setMediaId(videoToPlay.url).build()
                            )
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        }
                    }
                }
        }
    }

    HomeTabScaffold(
        title = stringResource(R.string.nav_video),
        showTopBar = true,
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_hint))
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues, contentModifier ->
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
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding()
                    )
            ) {
                CategorySelector(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        playingIndex = -1
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        viewModel.filterByCategory(category)
                    })
                Spacer(Modifier.height(8.dp))
                when (val state = wallpapersUiState) {
                    is UiState.Loading -> LoadingState()
                    is UiState.Success -> {
                        LiveGridContent(
                            wallpapers = currentWallpapers,
                            onWallpaperClick = onWallpaperClick,
                            gridState = gridState,
                            exoPlayer = exoPlayer,
                            playingIndex = playingIndex,
                            isLoadingMore = isLoadingMore,
                            canLoadMore = canLoadMore,
                            onLoadMore = { viewModel.loadMore() },
                            modifier = Modifier.weight(1f),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                    is UiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            if (currentWallpapers.isEmpty()) {
                                ErrorState(
                                    message = state.message,
                                    onRetry = { viewModel.refresh() }
                                )
                            } else {
                                LiveGridContent(
                                    wallpapers = currentWallpapers,
                                    onWallpaperClick = onWallpaperClick,
                                    gridState = gridState,
                                    exoPlayer = exoPlayer,
                                    playingIndex = playingIndex,
                                    isLoadingMore = false,
                                    canLoadMore = false,
                                    onLoadMore = {},
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        }
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
                    .padding(top = 30.dp)
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
    val viewportEndY = layoutInfo.viewportSize.height
    val viewportStartX = 0
    val viewportEndX = layoutInfo.viewportSize.width
    val candidate = visibleItems.filter { item ->
        val itemStartY = item.offset.y
        val itemEndY = itemStartY + item.size.height
        val itemStartX = item.offset.x
        val itemEndX = itemStartX + item.size.width
        val vertVisible = itemEndY > viewportStartY && itemStartY < viewportEndY
        val horizVisible = itemEndX > viewportStartX && itemStartX < viewportEndX
        vertVisible && horizVisible
    }.minByOrNull { item ->
        val yDist = kotlin.math.abs(item.offset.y - viewportStartY)
        val xDist = kotlin.math.abs(item.offset.x - viewportStartX)
        yDist * 1000 + xDist
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
        LiveVideoGrid(
            wallpapers = wallpapers,
            onWallpaperClick = onWallpaperClick,
            gridState = gridState,
            exoPlayer = exoPlayer,
            playingIndex = playingIndex,
            isLoadingMore = isLoadingMore,
            canLoadMore = canLoadMore,
            onLoadMore = onLoadMore,
            modifier = modifier,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun VideoLibraryScreenPreview() {
    ObscuraTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("Video Wallpapers Preview") }) }) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text("Preview Content Area")
            }
        }
    }
}
