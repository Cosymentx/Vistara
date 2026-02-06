package com.obscura.wallpapers.features.search

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.model.WallpaperCategory
import com.obscura.wallpapers.ui.components.ErrorState
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.components.CategoryChip
import com.obscura.wallpapers.ui.components.SearchBar
import com.obscura.wallpapers.ui.components.WallpaperItem
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.theme.stringResource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Composable
fun SearchScreen(
    onWallpaperClick: (Wallpaper) -> Unit, onBackClick: () -> Unit, viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val searchSuggestions by viewModel.searchSuggestions.collectAsState()
    val hotSearches by viewModel.hotSearches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val hazeState = rememberHazeState()
    Scaffold(
        topBar = {
            Column(modifier = Modifier.statusBarsPadding().hazeEffect(state = hazeState)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 16.dp, top = 8.dp)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back)
                        )
                    }

                    SearchBar(
                        query = query,
                        onQueryChange = viewModel::updateQuery,
                        onSearch = viewModel::search,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    )
                }

                if (query.isEmpty() && (searchHistory.isNotEmpty() || hotSearches.isNotEmpty())) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(hotSearches) { category ->
                            CategoryChip(
                                category = stringResource(category.titleRes), onClick = {
                                    viewModel.selectFromHistory(category)
                                    viewModel.search(category.apiValue)
                                })
                        }
                    }
                } else if (query.isNotEmpty() && searchSuggestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        searchSuggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectSuggestion(suggestion.apiValue)
                                        viewModel.search(suggestion.apiValue)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    text = stringResource(suggestion.titleRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 80.dp)
                .hazeSource(state = hazeState)
        ) {

            if (query.isEmpty() && searchResults.isEmpty()) {
                InitialSearchState(
                    hotSearches = hotSearches, onCategorySelected = viewModel::selectFromHistory, modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else if (isLoading) {
                LoadingState()
            } else if (error != null) {
                ErrorState(
                    message = error ?: stringResource(R.string.unknown_error),
                    onRetry = { viewModel.search(query) }
                )
            } else if (searchResults.isEmpty() && query.isNotEmpty()) {
                EmptyResultBox()
            } else {
                SearchResultList(
                    results = searchResults,
                    onWallpaperClick = onWallpaperClick
                )
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
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.no_wallpapers_found),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.try_different_keywords),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SearchResultList(
    results: List<Wallpaper>,
    onWallpaperClick: (Wallpaper) -> Unit
) {
    val listState = rememberLazyListState()
    val rememberedResults = remember(results) { results }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = stringResource(R.string.found_results, rememberedResults.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(rememberedResults.chunked(2)) { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEachIndexed { index, wallpaper ->
                    WallpaperItem(
                        wallpaper = wallpaper, onClick = { onWallpaperClick(wallpaper) }, modifier = Modifier
                            .weight(1f)
                            .height(240.dp)
                    )

                    if (index == 0 && rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
@Composable
private fun InitialSearchState(
    hotSearches: List<WallpaperCategory>, onCategorySelected: (WallpaperCategory) -> Unit, modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.hot_searches),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(hotSearches) { category ->
                CategoryChip(
                    category = stringResource(category.titleRes), onClick = { onCategorySelected(category) })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.search_tips),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchTip(
                title = stringResource(R.string.use_specific_descriptions), description = stringResource(R.string.specific_description_examples)
            )

            SearchTip(
                title = stringResource(R.string.try_different_languages), description = stringResource(R.string.english_keywords_tip)
            )

            SearchTip(
                title = stringResource(R.string.combine_keywords), description = stringResource(R.string.keyword_combination_examples)
            )
        }
    }
}

@Composable
private fun SearchTip(
    title: String, description: String, modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
