package com.obscura.wallpapers.features.search

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.model.WallpaperCategory
import com.obscura.wallpapers.ui.components.CategoryChip
import com.obscura.wallpapers.ui.components.ErrorState
import com.obscura.wallpapers.ui.components.HomeTabScaffold
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.components.SearchBar
import com.obscura.wallpapers.ui.components.WallpaperStaggeredGrid
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    onWallpaperClick: (Wallpaper) -> Unit,
    onBackClick: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val query by viewModel.query.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val searchSuggestions by viewModel.searchSuggestions.collectAsState()
    val hotSearches by viewModel.hotSearches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isPremiumUser by viewModel.isPremiumUser.collectAsState(initial = false)
    val purchasedIds by viewModel.purchasedIds.collectAsState()

    HomeTabScaffold(
        showTopBar = true, onBackPressed = onBackClick, headerExtra = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                SearchBar(
                    query = query,
                    onQueryChange = viewModel::updateQuery,
                    onSearch = viewModel::search,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }
        }) { _, contentModifier ->
        Box(
            modifier = contentModifier
        ) {
            val topPadding = 120.dp

            if (query.isNotEmpty() && searchSuggestions.isNotEmpty() && searchResults.isEmpty()) {
                // Suggestions overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = topPadding)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(horizontal = 16.dp)
                ) {
                    searchSuggestions.forEach { suggestion ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectSuggestion(suggestion.apiValue)
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = stringResource(suggestion.titleRes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (query.isEmpty() && searchResults.isEmpty()) {
                        InitialSearchState(
                            hotSearches = hotSearches,
                            onCategorySelected = viewModel::selectFromHistory,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = topPadding, start = 16.dp, end = 16.dp, bottom = 32.dp
                            )
                        )
                    } else if (isLoading) {
                        Spacer(modifier = Modifier.height(topPadding))
                        LoadingState()
                    } else if (error != null) {
                        Spacer(modifier = Modifier.height(topPadding))
                        ErrorState(
                            message = error ?: stringResource(R.string.common_unknown_error),
                            onRetry = { viewModel.search(query) })
                    } else if (searchResults.isEmpty() && query.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(topPadding))
                        EmptyResultBox()
                    } else {
                        SearchResultList(
                            results = searchResults,
                            onWallpaperClick = onWallpaperClick,
                            isPremiumUser = isPremiumUser,
                            purchasedIds = purchasedIds,
                            contentPadding = PaddingValues(
                                top = topPadding, start = 16.dp, end = 16.dp, bottom = 16.dp
                            ),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyResultBox() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.home_no_wallpapers_found),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_try_different_keywords),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SearchResultList(
    results: List<Wallpaper>,
    onWallpaperClick: (Wallpaper) -> Unit,
    isPremiumUser: Boolean = false,
    purchasedIds: Set<String> = emptySet(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val staggeredGridState = rememberLazyStaggeredGridState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.home_found_results, results.size),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                start = contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = contentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                top = contentPadding.calculateTopPadding(),
                bottom = 8.dp
            )
        )

        WallpaperStaggeredGrid(
            wallpapers = results,
            onWallpaperClick = onWallpaperClick,
            isPremiumUser = isPremiumUser,
            purchasedIds = purchasedIds,
            gridState = staggeredGridState,
            contentPadding = PaddingValues(
                start = contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = contentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                bottom = contentPadding.calculateBottomPadding()
            ),
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun InitialSearchState(
    hotSearches: List<WallpaperCategory>,
    onCategorySelected: (WallpaperCategory) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.home_hot_searches),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                @OptIn(ExperimentalLayoutApi::class) FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hotSearches.forEach { category ->
                        CategoryChip(
                            category = stringResource(category.titleRes),
                            onClick = { onCategorySelected(category) },
                            modifier = Modifier.animateContentSize()
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.home_search_tips),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SearchTip(
                        title = stringResource(R.string.home_use_specific_descriptions),
                        description = stringResource(R.string.home_specific_description_examples)
                    )

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )

                    SearchTip(
                        title = stringResource(R.string.home_try_different_languages),
                        description = stringResource(R.string.home_english_keywords_tip)
                    )

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )

                    SearchTip(
                        title = stringResource(R.string.home_combine_keywords),
                        description = stringResource(R.string.home_keyword_combination_examples)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTip(
    title: String, description: String, modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            lineHeight = 20.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    ObscuraTheme {
        SearchScreen(onWallpaperClick = {}, onBackClick = {})
    }
}
