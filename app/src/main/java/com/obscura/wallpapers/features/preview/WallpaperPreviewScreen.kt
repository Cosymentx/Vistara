package com.obscura.wallpapers.features.preview

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.WallpaperTarget
import com.obscura.wallpapers.ui.components.ErrorState
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.components.LoginPromptDialog
import com.obscura.wallpapers.ui.components.WallpaperPreview
import com.obscura.wallpapers.ui.components.WallpaperSetOptions
import com.obscura.wallpapers.ui.theme.stringResource
import kotlinx.coroutines.launch

@Composable
fun WallpaperPreviewScreen(
    onBackPressed: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: WallpaperPreviewViewModel = hiltViewModel()
) {
    val wallpaperState by viewModel.wallpaperState.collectAsState()
    val isFavorite by viewModel.isFavorite
    val isPremiumUser by viewModel.isPremiumUser
    val showSetWallpaperOptions by viewModel.showSetWallpaperOptions
    val navigateToUpgrade by viewModel.navigateToUpgrade
    val isDownloading by viewModel.isDownloading
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val isInfoExpanded by viewModel.isInfoExpanded
    val needStoragePermission by viewModel.needStoragePermission
    val upgradeResult by viewModel.upgradeResult.collectAsState()
    val isProcessingWallpaper by viewModel.isProcessingWallpaper
    val wallpaperSetSuccess by viewModel.wallpaperSetSuccess.collectAsState()

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val needLoginAction by viewModel.needLoginAction.collectAsState()

    val context = LocalContext.current
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(upgradeResult) {
        upgradeResult?.let { result ->
            when (result) {
                is WallpaperPreviewViewModel.UpgradeResult.Success -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(result.message)
                    }
                }
                is WallpaperPreviewViewModel.UpgradeResult.Error -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(result.message)
                    }
                }
            }
            viewModel.clearUpgradeResult()
        }
    }

    LaunchedEffect(wallpaperSetSuccess) {
        wallpaperSetSuccess?.let { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            viewModel.clearWallpaperSetSuccess()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.continueDownloadAfterPermissionGranted()
            Toast.makeText(context, R.string.preview_start_download_wallpaper, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.preview_download_failed_permission_denied, Toast.LENGTH_SHORT)
                .show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshEditedImage()
    }

    LaunchedEffect(navigateToUpgrade) {
        if (navigateToUpgrade) {
            viewModel.resetNavigateToUpgrade()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (wallpaperState) {
                is UiState.Loading -> {
                    LoadingState()
                }
                is UiState.Success -> {
                    val wallpaper = (wallpaperState as UiState.Success).data
                    val editedBitmap by viewModel.editedBitmap

                    if (wallpaper.isLive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    } else {
                        val blurredBitmap by viewModel.blurredBackgroundBitmap
                        if (blurredBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = blurredBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LaunchedEffect(wallpaper.id) {
                                viewModel.loadBlurredBackground()
                            }
                        }
                    }

                    WallpaperPreview(
                        wallpaper = wallpaper,
                        isFavorite = isFavorite,
                        isInfoExpanded = isInfoExpanded,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        onBackPressed = onBackPressed,
                        onToggleFavorite = { viewModel.toggleFavorite() },
                        onToggleInfo = { viewModel.toggleInfoExpanded() },
                        onSetWallpaper = {
                            if (!isLoggedIn) {
                                viewModel.setNeedLoginAction(WallpaperPreviewViewModel.LoginAction.SET_WALLPAPER)
                                return@WallpaperPreview
                            }
                            viewModel.showSetWallpaperOptions(activity)
                        },
                        onDownload = {
                            viewModel.download()
                        },
                        onShare = { viewModel.share() },
                        onEdit = {
                            if (!isLoggedIn) {
                                viewModel.setNeedLoginAction(WallpaperPreviewViewModel.LoginAction.EDIT)
                                return@WallpaperPreview
                            }
                            if (wallpaper.isLive) {
                                Toast.makeText(
                                    context,
                                    R.string.editor_live_wallpaper_edit_not_supported,
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@WallpaperPreview
                            }
//                            if (wallpaper.isPremium && !isPremiumUser) {
//                                viewModel.showPremiumPrompt()
//                                return@WallpaperPreview
//                            }
                            val wallpaperId = wallpaper.id
                            onNavigateToEdit(wallpaperId)
                        },
                        onPreview = {
                            viewModel.preview(activity)
                        },
                        isPremiumUser = isPremiumUser,
                        editedBitmap = editedBitmap,
                        isProcessingWallpaper = isProcessingWallpaper
                    )

                    if (showSetWallpaperOptions) {
                        Dialog(
                            onDismissRequest = { viewModel.hideSetWallpaperOptions() },
                            properties = DialogProperties(
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true,
                                usePlatformDefaultWidth = false
                            )
                        ) {
                            WallpaperSetOptions(onSetHomeScreen = {
                                viewModel.setWallpaper(activity, WallpaperTarget.HOME)
                            }, onSetLockScreen = {
                                viewModel.setWallpaper(activity, WallpaperTarget.LOCK)
                            }, onSetBoth = {
                                viewModel.setWallpaper(activity, WallpaperTarget.BOTH)
                            }, onDismiss = {
                                viewModel.hideSetWallpaperOptions()
                            })
                        }
                    }
                }
                is UiState.Error -> {
                    ErrorState(
                        message = (wallpaperState as UiState.Error).message,
                        onRetry = { onBackPressed() }
                    )
                }
            }
        }
    }

    if (needStoragePermission) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.preview_storage_permission_required)) },
            text = { Text(stringResource(R.string.preview_storage_permission_rationale)) },
            confirmButton = {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }) {
                    Text(stringResource(R.string.preview_grant_permission))
                }
            },
            dismissButton = {
                Button(onClick = {
                    Toast.makeText(
                        context, R.string.preview_download_failed_permission_required, Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetPermissionRequest()
                }) {
                    Text(stringResource(R.string.cancel))
                }
            })
    }

    needLoginAction?.let { action ->
        val message = when (action) {
            WallpaperPreviewViewModel.LoginAction.FAVORITE -> stringResource(R.string.preview_favorite_login_required)
            WallpaperPreviewViewModel.LoginAction.DOWNLOAD -> stringResource(R.string.preview_download_login_required)
            WallpaperPreviewViewModel.LoginAction.SET_WALLPAPER -> stringResource(R.string.preview_set_wallpaper_login_required)
            WallpaperPreviewViewModel.LoginAction.EDIT -> stringResource(R.string.preview_edit_login_required)
        }

        LoginPromptDialog(
            onDismiss = { viewModel.clearNeedLoginAction() }, onConfirm = {
            viewModel.clearNeedLoginAction()
            onNavigateToLogin()
        }, message = message
        )
    }
}
