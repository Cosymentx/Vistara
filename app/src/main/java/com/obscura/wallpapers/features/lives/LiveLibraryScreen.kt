package com.obscura.wallpapers.features.lives

// Make sure this points to the definition that includes cachedData
// Import VideoItem if it's in components, otherwise define it below or import from correct path
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
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
import com.obscura.wallpapers.ui.components.GlassTopAppBar
import com.obscura.wallpapers.ui.components.LiveVideoGrid
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.theme.VistaraTheme
import com.obscura.wallpapers.ui.theme.stringResource
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.math.abs

private const val TAG = "LiveLibraryScreen"
private const val GRID_COLUMNS = 2

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun LiveLibraryScreen(
    onWallpaperClick: (Wallpaper) -> Unit,
    onSearchClick: () -> Unit = {},
    viewModel: LiveLibraryViewModel = hiltViewModel()
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
                (wallpapersUiState as? UiState.Success<List<Wallpaper>>)?.data ?:
//                (wallpapersUiState as? UiState.Error)?.cachedData ?:
                emptyList()
            }
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (exoPlayer.isPlaying) {
                        Log.d(TAG, "Lifecycle ON_PAUSE, pausing player.")
                        wasPausedForLifecycle = true
                        exoPlayer.pause()
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (wasPausedForLifecycle && playingIndex != -1 && !gridState.isScrollInProgress) {
                        Log.d(TAG, "Lifecycle ON_RESUME, resuming player for index $playingIndex.")
                        exoPlayer.playWhenReady = true
                    }
                    wasPausedForLifecycle = false
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    DisposableEffect(
        exoPlayer, currentWallpapers.size, gridState
    ) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && !gridState.isScrollInProgress && !wasPausedForLifecycle) {
                    Log.d(
                        TAG,
                        "Playback ended for index $playingIndex. Finding next visible item to play."
                    )
                    val visibleItemsInfo = gridState.layoutInfo.visibleItemsInfo
                    if (visibleItemsInfo.isEmpty() || playingIndex == -1) {
                        Log.d(TAG, "No visible items or nothing was playing. Stopping.")
                        playingIndex = -1
                        return
                    }

                    val currentVisibleItem = visibleItemsInfo.find { it.index == playingIndex }
                    val currentVisibleListIndex = visibleItemsInfo.indexOf(currentVisibleItem)

                    if (currentVisibleListIndex != -1) {
                        val nextVisibleListIndex =
                            (currentVisibleListIndex + 1) % visibleItemsInfo.size
                        val nextVisibleItemInfo = visibleItemsInfo[nextVisibleListIndex]
                        val nextPlayingIndex = nextVisibleItemInfo.index

                        Log.d(TAG, "Next visible item to play is at index: $nextPlayingIndex")

                        if (nextPlayingIndex < currentWallpapers.size) {
                            val nextVideo = currentWallpapers[nextPlayingIndex]
                            if (nextVideo.url != null) {
                                Log.d(
                                    TAG,
                                    "Playing next video at index $nextPlayingIndex: ${nextVideo.url}"
                                )
                                playingIndex = nextPlayingIndex
                                exoPlayer.setMediaItem(
                                    MediaItem.Builder().setUri(Uri.parse(nextVideo.url))
                                        .setMediaId(nextVideo.url).build()
                                )
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            } else {
                                Log.w(
                                    TAG,
                                    "Next video item at index $nextPlayingIndex has null URL. Stopping playback."
                                )
                                playingIndex = -1
                            }
                        } else {
                            Log.w(
                                TAG,
                                "Calculated next index $nextPlayingIndex is out of bounds for currentWallpapers size (${currentWallpapers.size}). Stopping playback."
                            )
                            playingIndex = -1
                        }
                    } else {
                        Log.d(
                            TAG,
                            "The item that finished playback (index $playingIndex) is no longer visible. Stopping playback."
                        )
                        playingIndex = -1
                    }
                } else if (playbackState == Player.STATE_BUFFERING) {
                    Log.d(TAG, "Player is BUFFERING for index $playingIndex")
                } else if (playbackState == Player.STATE_READY) {
                    Log.d(
                        TAG,
                        "Player is READY for index $playingIndex. PlayWhenReady=${exoPlayer.playWhenReady}"
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player Error occurred for index $playingIndex: ${error.message}", error)
                playingIndex = -1
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(gridState, currentWallpapers.size) {
        snapshotFlow { gridState.isScrollInProgress }.distinctUntilChanged()
            .collect { isScrolling ->
                if (isScrolling) {
                    if (exoPlayer.isPlaying) {
                        Log.d(
                            TAG, "Scroll detected, pausing player currently at index $playingIndex."
                        )
                        exoPlayer.pause()
                    }
                } else {
                    if (wasPausedForLifecycle) {
                        Log.d(
                            TAG,
                            "Scroll stopped, but player was paused for lifecycle. Resume will be handled by ON_RESUME."
                        )
                        return@collect
                    }

                    Log.d(TAG, "Scroll stopped. Determining best item to play.")
                    val visibleItemsInfo = gridState.layoutInfo.visibleItemsInfo
                    if (visibleItemsInfo.isNotEmpty()) {
                        val bestVisibleIndex =
                            findBestVisibleItemToPlay(visibleItemsInfo, gridState.layoutInfo)

                        if (bestVisibleIndex != -1 && bestVisibleIndex < currentWallpapers.size) {
                            if (bestVisibleIndex != playingIndex || (!exoPlayer.isPlaying && exoPlayer.playbackState == Player.STATE_IDLE)) {
                                val videoToPlay = currentWallpapers[bestVisibleIndex]
                                if (videoToPlay.url != null) {
                                    Log.d(
                                        TAG,
                                        "Scroll stopped. Playing item at target index $bestVisibleIndex: ${videoToPlay.url}"
                                    )
                                    playingIndex = bestVisibleIndex
                                    exoPlayer.setMediaItem(
                                        MediaItem.Builder().setUri(Uri.parse(videoToPlay.url))
                                            .setMediaId(videoToPlay.url).build()
                                    )
                                    exoPlayer.prepare()
                                    exoPlayer.playWhenReady = true
                                } else {
                                    Log.w(
                                        TAG,
                                        "Scroll stopped. Target video at index $bestVisibleIndex has null URL."
                                    )
                                    if (playingIndex == bestVisibleIndex) playingIndex = -1
                                }
                            } else if (bestVisibleIndex == playingIndex && !exoPlayer.isPlaying && exoPlayer.playbackState != Player.STATE_ENDED) {
                                Log.d(
                                    TAG,
                                    "Scroll stopped. Resuming playback for already targeted index $playingIndex."
                                )
                                exoPlayer.playWhenReady = true
                            } else {
                                Log.d(
                                    TAG,
                                    "Scroll stopped. Target $bestVisibleIndex is already playing or preparing."
                                )
                            }
                        } else if (bestVisibleIndex != -1) {
                            Log.w(
                                TAG,
                                "Scroll stopped. Best visible index $bestVisibleIndex is out of bounds (${currentWallpapers.size})."
                            )
                            playingIndex = -1
                            exoPlayer.stop()
                        } else {
                            Log.d(
                                TAG,
                                "Scroll stopped. No suitable visible item found to play. Stopping playback."
                            )
                            playingIndex = -1
                            exoPlayer.stop()
                        }
                    } else {
                        Log.d(TAG, "Scroll stopped, but no items are visible. Stopping playback.")
                        playingIndex = -1
                        exoPlayer.stop()
                    }
                }
            }
    }

    LaunchedEffect(currentWallpapers, gridState) {
        if (currentWallpapers.isNotEmpty()) {
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo }.filter { it.isNotEmpty() && !gridState.isScrollInProgress && playingIndex == -1 && !wasPausedForLifecycle }
                .map {
                    findBestVisibleItemToPlay(
                        it, gridState.layoutInfo
                    )
                }.distinctUntilChanged().collect { bestVisibleIndex ->
                    if (bestVisibleIndex != -1 && bestVisibleIndex < currentWallpapers.size) {
                        val videoToPlay = currentWallpapers[bestVisibleIndex]
                        if (videoToPlay.url != null) {
                            Log.d(
                                TAG,
                                "Initial Play Triggered. Playing item at index $bestVisibleIndex: ${videoToPlay.url}"
                            )
                            playingIndex = bestVisibleIndex
                            exoPlayer.setMediaItem(
                                MediaItem.Builder().setUri(Uri.parse(videoToPlay.url))
                                    .setMediaId(videoToPlay.url).build()
                            )
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        } else {
                            Log.w(
                                TAG,
                                "Initial Play. Target video URL is null at index $bestVisibleIndex"
                            )
                        }
                    } else {
                        Log.d(
                            TAG,
                            "Initial Play. No suitable item found or index $bestVisibleIndex invalid."
                        )
                    }
                }
        }
    }

    val (contentModifier, topBar) = GlassTopAppBar(
        title = stringResource(R.string.category_live), actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    Icons.Default.Search, contentDescription = stringResource(R.string.search_hint)
                )
            }
        })
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { topBar() }) { _ ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing, onRefresh = {
                Log.d(TAG, "Pull to refresh initiated.")
                playingIndex = -1
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                viewModel.refresh()
            })

        Box(
            modifier = contentModifier.pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
            ) {
                CategorySelector(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        Log.d(
                            TAG,
                            "Category selected: ${category.name}. Stopping player and filtering."
                        )
                        playingIndex = -1
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        viewModel.filterByCategory(category)
                    })

                Spacer(Modifier.height(8.dp))

                when (val state = wallpapersUiState) {
                    is UiState.Loading -> {
                        LoadingState()
                    }

                    is UiState.Success -> {
                        if (currentWallpapers.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(R.string.no_wallpapers_found),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LiveVideoGrid(
                                wallpapers = currentWallpapers,
                                onWallpaperClick = onWallpaperClick,
                                gridState = gridState,
                                exoPlayer = exoPlayer,
                                playingIndex = playingIndex,
                                isLoadingMore = isLoadingMore,
                                canLoadMore = canLoadMore,
                                onLoadMore = { viewModel.loadMore() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    is UiState.Error -> {
                        Log.e(TAG, "Error state: ${state.message}")
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            if (currentWallpapers.isEmpty()) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                LaunchedEffect(state.message) {
                                    snackbarHostState.showSnackbar(
                                        message = state.message, duration = SnackbarDuration.Short
                                    )
                                }
                                LiveVideoGrid(
                                    wallpapers = currentWallpapers,
                                    onWallpaperClick = onWallpaperClick,
                                    gridState = gridState,
                                    exoPlayer = exoPlayer,
                                    playingIndex = playingIndex,
                                    isLoadingMore = false,
                                    canLoadMore = false,
                                    onLoadMore = {},
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
            Log.d("ExoPlayerLifecycle", "ExoPlayer instance created.")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("ExoPlayerLifecycle", "Releasing ExoPlayer instance.")
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
        val yDist = abs(item.offset.y - viewportStartY)
        val xDist = abs(item.offset.x - viewportStartX)
        yDist * 1000 + xDist
    }

    Log.d(
        "findBestItem",
        "Candidate for playback: index=${candidate?.index}, offset=${candidate?.offset}"
    )
    return candidate?.index ?: -1
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun LiveLibraryScreenPreview() {
    VistaraTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Live Wallpapers Preview") }) }) { padding ->
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
