package com.obscura.wallpapers.features.preview

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
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

/**
 * Screen that displays a preview of a wallpaper and provides options to set it as home/lock screen,
 * download it, share it, or edit it.
 *
 * This screen handles:
 * - Displaying the wallpaper (static or blurred background for live).
 * - Toggling favorite status.
 * - Downloading wallpapers with permission handling.
 * - Navigating to the editor.
 * - Showing wallpaper metadata.
 * - Handling login-restricted actions.
 *
 * @param onBackPressed Callback to navigate back to the previous screen.
 * @param onNavigateToEdit Callback to navigate to the wallpaper editor screen with the given wallpaper ID.
 * @param onNavigateToLogin Callback to navigate to the login screen.
 * @param viewModel The [WallpaperPreviewViewModel] that manages the state for this screen.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun WallpaperPreviewScreen(
    onBackPressed: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: WallpaperPreviewViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
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
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = Color.Black // Set background to black for better transition
    ) { paddingValues ->
        AnimatedContent(
            targetState = wallpaperState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 1.05f, animationSpec = tween(500)))
                    .togetherWith(fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 0.95f, animationSpec = tween(400)))
            },
            label = "WallpaperStateTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { targetState ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (targetState) {
                    is UiState.Loading -> {
                        LoadingState()
                    }
                    is UiState.Success -> {
                        val wallpaper = targetState.data
                        val editedBitmap by viewModel.editedBitmap

                        // Background Layer
                        if (!wallpaper.isLive) {
                            val blurredBitmap by viewModel.blurredBackgroundBitmap
                            if (blurredBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = blurredBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Dark overlay for better text readability
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f))
                                )
                            } else {
                                LaunchedEffect(wallpaper.id) {
                                    viewModel.loadBlurredBackground()
                                }
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
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
                                val wallpaperId = wallpaper.id
                                onNavigateToEdit(wallpaperId)
                            },
                            onPreview = {
                                viewModel.preview(activity)
                            },
                            isPremiumUser = isPremiumUser,
                            editedBitmap = editedBitmap,
                            isProcessingWallpaper = isProcessingWallpaper,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
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
                            message = targetState.message,
                            onRetry = { onBackPressed() }
                        )
                    }
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
