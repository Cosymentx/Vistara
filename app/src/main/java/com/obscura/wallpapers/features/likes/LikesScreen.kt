package com.obscura.wallpapers.features.likes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.obscura.wallpapers.ui.theme.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.obscura.wallpapers.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.ui.components.ErrorState
import com.obscura.wallpapers.ui.components.LoginPromptDialog
import com.obscura.wallpapers.ui.components.WallpaperGrid
import com.obscura.wallpapers.ui.components.GlassScaffold
import com.obscura.wallpapers.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikesScreen(
    onBackPressed: () -> Unit,
    onWallpaperClick: (Wallpaper) -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: LikesViewModel = hiltViewModel()
) {
    val favoritesState by viewModel.favoritesState.collectAsState()

    GlassScaffold(
        title = stringResource(R.string.my_favorites),
        onBackPressed = onBackPressed
    ) { paddingValues ->
        FavoritesBody(
            state = favoritesState,
            onWallpaperClick = onWallpaperClick,
            onRetry = { viewModel.refresh() },
            onNavigateToLogin = onNavigateToLogin,
            onBackPressed = onBackPressed,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, bottom = 80.dp)
        )
    }
}

@Composable
private fun FavoritesBody(
    state: UiState<*>,
    onWallpaperClick: (Wallpaper) -> Unit,
    onRetry: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (state) {
            is UiState.Loading -> {
                LoadingState()
            }

            is UiState.Error -> {
                val errorMessage = state.message
                if (errorMessage == stringResource(R.string.login_required_to_view_favorites)) {
                    LoginPromptDialog(
                        onDismiss = { onBackPressed() },
                        onConfirm = { onNavigateToLogin() },
                        message = stringResource(R.string.favorites_login_required)
                    )
                } else {
                    ErrorState(
                        message = errorMessage,
                        onRetry = onRetry
                    )
                }
            }

            is UiState.Success<*> -> {
                val wallpapers = state.data as? List<Wallpaper> ?: emptyList()
                if (wallpapers.isEmpty()) {
                    EmptyFavoritesContent()
                } else {
                    WallpaperGrid(
                        wallpapers = wallpapers,
                        onWallpaperClick = onWallpaperClick,
                        contentPadding = PaddingValues(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFavoritesContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.3f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_favorite_wallpapers),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.browse_and_favorite_tip),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.weight(0.7f))
    }
}
