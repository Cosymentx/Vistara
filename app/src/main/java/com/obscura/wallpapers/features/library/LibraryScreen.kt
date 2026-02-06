package com.obscura.wallpapers.features.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.obscura.wallpapers.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBackPressed: () -> Unit,
    onWallpaperClick: (Wallpaper) -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val downloadsState by viewModel.downloadsState.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_downloads)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                })
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (val state = downloadsState) {
                is UiState.Loading -> {
                    LoadingState()
                }
                is UiState.Success -> {
                    val wallpapers = state.data
                    if (wallpapers.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_downloaded_wallpapers),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        WallpaperGrid(
                            wallpapers = wallpapers,
                            onWallpaperClick = onWallpaperClick,
                            contentPadding = PaddingValues(4.dp),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is UiState.Error -> {
                    if (!isLoggedIn) {
                        LoginPromptDialog(
                            onDismiss = { onBackPressed() },
                            onConfirm = { onNavigateToLogin() },
                            message = stringResource(R.string.downloads_login_required)
                        )
                    } else {
                        ErrorState(
                            message = state.message ?: stringResource(R.string.error_network_check),
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }
            }
        }
    }
}
