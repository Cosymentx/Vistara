package com.obscura.wallpapers.features.statics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.ui.components.CategorySelector
import com.obscura.wallpapers.ui.components.ErrorState
import com.obscura.wallpapers.ui.components.GlassTopAppBar
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.components.WallpaperStaggeredGrid
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun StaticLibraryScreen(
    onWallpaperClick: (Wallpaper) -> Unit,
    onSearchClick: () -> Unit = {},
    viewModel: StaticLibraryViewModel = hiltViewModel()
) {
    val wallpapersState by viewModel.wallpapersState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories = viewModel.categories
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val canLoadMore by viewModel.canLoadMore.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing, onRefresh = { viewModel.refresh() })

    val (contentModifier, topBar) = GlassTopAppBar(
        title = stringResource(R.string.category_static), actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_hint)
                )
            }
        })
    Scaffold(topBar = { topBar() }) { paddingValues ->
        Box(
            modifier = contentModifier
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp)
                    .padding(bottom = 80.dp)
            ) {
                CategorySelector(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        viewModel.filterByCategory(category)
                    })

                when (wallpapersState) {
                    is UiState.Loading -> LoadingState()
                    is UiState.Success -> StaticGridContent(
                        wallpapers = (wallpapersState as UiState.Success<List<Wallpaper>>).data,
                        isLoadingMore = isLoadingMore,
                        canLoadMore = canLoadMore,
                        onLoadMore = { viewModel.loadMore() },
                        onWallpaperClick = onWallpaperClick
                    )
                    is UiState.Error -> ErrorState(
                        message = (wallpapersState as UiState.Error).message,
                        onRetry = { viewModel.refresh() })
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
private fun StaticGridContent(
    wallpapers: List<Wallpaper>,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onWallpaperClick: (Wallpaper) -> Unit
) {
    if (wallpapers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.no_wallpapers_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                val gridState = rememberLazyStaggeredGridState()
                val rememberedWallpapers = remember(wallpapers) { wallpapers }
                val rememberedIsLoadingMore = remember(isLoadingMore) { isLoadingMore }
                val rememberedCanLoadMore = remember(canLoadMore) { canLoadMore }
                WallpaperStaggeredGrid(
                    wallpapers = rememberedWallpapers,
                    onWallpaperClick = onWallpaperClick,
                    onLoadMore = onLoadMore,
                    isLoadingMore = rememberedIsLoadingMore,
                    canLoadMore = rememberedCanLoadMore,
                    showEndMessage = !rememberedCanLoadMore,
                    gridState = gridState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StaticLibraryScreenPreview() {
    ObscuraTheme {
        StaticLibraryScreen(onWallpaperClick = {}, onSearchClick = {})
    }
}
