package com.obscura.wallpapers.features.discover

/* Moved from features/home to features/discover to align folder with package */

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.Banner
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.model.WallpaperCategory
import com.obscura.wallpapers.ui.components.Carousel
import com.obscura.wallpapers.ui.components.CategorySelector
import com.obscura.wallpapers.ui.components.ErrorState
import com.obscura.wallpapers.ui.components.FeaturedWallpaperSection
import com.obscura.wallpapers.ui.components.GlassScaffold
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.components.WallpaperItem
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onWallpaperClick: (Wallpaper) -> Unit,
    onSearch: (String) -> Unit = {},
    onBannerClick: (Banner) -> Unit = {},
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val banners by viewModel.banners.collectAsState()
    val featuredWallpapers by viewModel.featuredWallpapers.collectAsState()
    val staticWallpapers by viewModel.staticWallpapers.collectAsState()
    val liveWallpapers by viewModel.liveWallpapers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    GlassScaffold(title = stringResource(R.string.home)) { paddingValues ->
        if (isLoading) {
            LoadingState()
            return@GlassScaffold
        }

        if (error != null && featuredWallpapers.isEmpty() && staticWallpapers.isEmpty() && liveWallpapers.isEmpty()) {
            ErrorState(
                message = error ?: stringResource(R.string.unknown_error),
                onRetry = { viewModel.refresh() }
            )
            return@GlassScaffold
        }

        LazyColumn(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(top = 64.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (error != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.partial_data_loading_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                onClick = { viewModel.refresh() },
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.refresh),
                                    color = MaterialTheme.colorScheme.onError,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .clickable { onSearch("") },
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_icon),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = stringResource(R.string.search_high_quality_wallpapers),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (banners.isNotEmpty()) {
                item {
                    Carousel(
                        banners = banners,
                        onBannerClick = onBannerClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(horizontal = 10.dp)
                            .clip(RoundedCornerShape(15.dp))
                    )
                }
            }

            if (featuredWallpapers.isNotEmpty()) {
                item {
                    FeaturedWallpaperSection(
                        wallpaper = featuredWallpapers.first(),
                        onWallpaperClick = onWallpaperClick,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                CategorySection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onWallpaperClick = onWallpaperClick
                )
            }

            val sections = buildSections(
                staticItems = staticWallpapers,
                liveItems = liveWallpapers,
                latestItems = featuredWallpapers
            )
            items(sections.size) { index ->
                val s = sections[index]
                TwoColumnSection(
                    title = when (s.type) {
                        SectionType.Photo -> if (s.items.isNotEmpty()) stringResource(R.string.hot_static) else null
                        SectionType.Video -> if (s.items.isNotEmpty()) stringResource(R.string.cool_dynamic) else null
                        SectionType.Latest -> if (s.items.isNotEmpty()) stringResource(R.string.latest_uploads) else null
                    },
                    wallpapers = s.items,
                    onWallpaperClick = onWallpaperClick,
                    titlePadding = when (s.type) {
                        SectionType.Video -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        else -> PaddingValues(horizontal = 16.dp)
                    }
                )
            }
        }
    }
}

enum class SectionType { Photo, Video, Latest }

@Immutable
data class Section(val type: SectionType, val items: List<Wallpaper>)

private fun buildSections(
    staticItems: List<Wallpaper>,
    liveItems: List<Wallpaper>,
    latestItems: List<Wallpaper>
): List<Section> {
    return listOf(
        Section(SectionType.Photo, staticItems),
        Section(SectionType.Video, liveItems),
        Section(SectionType.Latest, latestItems)
    )
}

@Composable
private fun TwoColumnSection(
    title: String?,
    wallpapers: List<Wallpaper>,
    onWallpaperClick: (Wallpaper) -> Unit,
    titlePadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    Column {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(titlePadding)
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            wallpapers.chunked(2).forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    rowItems.forEachIndexed { index, wallpaper ->
                        WallpaperItem(
                            wallpaper = wallpaper,
                            onClick = { onWallpaperClick(wallpaper) },
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp)
                        )
                        if (index == 0 && rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySection(
    modifier: Modifier = Modifier,
    onWallpaperClick: (Wallpaper) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categoryWallpapers by viewModel.categoryWallpapers.collectAsState()
    val isCategoryLoading by viewModel.isCategoryLoading.collectAsState()

    Column(modifier = modifier) {
        CategorySelector(
            categories = WallpaperCategory.values().toList(),
            selectedCategory = selectedCategory ?: WallpaperCategory.ALL,
            onCategorySelected = { viewModel.loadWallpapersByCategory(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isCategoryLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (categoryWallpapers.isEmpty()) {
            Text(
                text = stringResource(R.string.no_wallpapers_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            TwoColumnSection(
                title = null,
                wallpapers = categoryWallpapers,
                onWallpaperClick = onWallpaperClick
            )
        }
    }
}
