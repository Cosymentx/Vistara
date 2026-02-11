package com.obscura.wallpapers.features.photo

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.ui.components.CategorySelector
import com.obscura.wallpapers.ui.components.ErrorState
import com.obscura.wallpapers.ui.components.HomeTabScaffold
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.components.WallpaperStaggeredGrid
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun PhotoLibraryScreen(
    onWallpaperClick: (Wallpaper) -> Unit,
    onSearchClick: () -> Unit = {},
    viewModel: PhotoLibraryViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val wallpapersState by viewModel.wallpapersState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories = viewModel.categories
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val canLoadMore by viewModel.canLoadMore.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing, onRefresh = { viewModel.refresh() })

    HomeTabScaffold(
        title = stringResource(R.string.nav_photo),
        showTopBar = true,
        headerExtra = {
            CategorySelector(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    viewModel.filterByCategory(category)
                })
            Spacer(Modifier.height(8.dp))
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            }
        }
    ) { _, contentModifier ->
        Box(
            modifier = contentModifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            val currentData = (wallpapersState as? UiState.Success)?.data ?: emptyList()

            when (val state = wallpapersState) {
                is UiState.Loading -> LoadingState()
                is UiState.Success, is UiState.Error -> {
                    StaticGridContent(
                        wallpapers = currentData,
                        isLoadingMore = isLoadingMore,
                        canLoadMore = canLoadMore,
                        onLoadMore = { viewModel.loadMore() },
                        onWallpaperClick = onWallpaperClick,
                        // Content padding unified: 16dp horizontal
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 160.dp,
                            bottom = 100.dp
                        ),
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )

                    if (state is UiState.Error && currentData.isEmpty()) {
                        ErrorState(
                            message = state.message,
                            onRetry = { viewModel.refresh() })
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun StaticGridContent(
    wallpapers: List<Wallpaper>,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onWallpaperClick: (Wallpaper) -> Unit,
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
        val gridState = rememberLazyStaggeredGridState()
        WallpaperStaggeredGrid(
            wallpapers = wallpapers,
            onWallpaperClick = onWallpaperClick,
            onLoadMore = onLoadMore,
            isLoadingMore = isLoadingMore,
            canLoadMore = canLoadMore,
            showEndMessage = !canLoadMore,
            gridState = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}
