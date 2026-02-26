package com.obscura.wallpapers.features.discover

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.Banner
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.ui.components.FeaturedWallpaperSection
import com.obscura.wallpapers.ui.components.HomeTabScaffold
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.components.WallpaperItem
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverScreen(
    onWallpaperClick: (Wallpaper) -> Unit,
    onSearch: (String) -> Unit = {},
    onBannerClick: (Banner) -> Unit = {},
    viewModel: DiscoverViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val featuredWallpapers by viewModel.featuredWallpapers.collectAsState()
    val staticWallpapers by viewModel.staticWallpapers.collectAsState()
    val liveWallpapers by viewModel.liveWallpapers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    HomeTabScaffold(
        showTopBar = true, title = null, headerExtra = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.1f), // Very subtle background, Haze handles the rest
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                onClick = { onSearch("") }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.home_search_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
//            Spacer(modifier = Modifier.height(4.dp))
        }) { _, _ ->
        if (isLoading) {
            LoadingState()
            return@HomeTabScaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (featuredWallpapers.isNotEmpty()) {
                    item {
                        FeaturedWallpaperSection(
                            wallpaper = featuredWallpapers.first(),
                            onWallpaperClick = onWallpaperClick,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
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
                            SectionType.Photo -> if (s.items.isNotEmpty()) stringResource(R.string.home_hot_static) else null
                            SectionType.Video -> if (s.items.isNotEmpty()) stringResource(R.string.home_cool_dynamic) else null
                            SectionType.Latest -> if (s.items.isNotEmpty()) stringResource(R.string.home_latest_uploads) else null
                        },
                        wallpapers = s.items,
                        onWallpaperClick = onWallpaperClick,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }
        }
    }
}

enum class SectionType { Photo, Video, Latest }

@Immutable
data class Section(val type: SectionType, val items: List<Wallpaper>)

private fun buildSections(
    staticItems: List<Wallpaper>, liveItems: List<Wallpaper>, latestItems: List<Wallpaper>
): List<Section> {
    return listOf(
        Section(SectionType.Photo, staticItems),
        Section(SectionType.Video, liveItems),
        Section(SectionType.Latest, latestItems)
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TwoColumnSection(
    title: String?,
    wallpapers: List<Wallpaper>,
    onWallpaperClick: (Wallpaper) -> Unit,
    titlePadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    Column {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(titlePadding)
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            wallpapers.chunked(2).forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // Unified item spacing
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp, vertical = 6.dp
                        ) // Unified row spacing (6+6=12)
                ) {
                    rowItems.forEachIndexed { index, wallpaper ->
                        WallpaperItem(
                            wallpaper = wallpaper,
                            onClick = { onWallpaperClick(wallpaper) },
                            modifier = Modifier
                                .weight(1f)
                                .height(220.dp),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
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
