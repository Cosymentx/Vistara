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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleEventObserver
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.core.data.model.WallpaperTarget
import com.obscura.wallpapers.features.billing.BillingViewModel
import com.obscura.wallpapers.features.billing.PaywallScreen
import com.obscura.wallpapers.ui.components.ConfirmDialog
import com.obscura.wallpapers.ui.components.ErrorState
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.components.LoginPromptDialog
import com.obscura.wallpapers.ui.components.WallpaperPreview
import com.obscura.wallpapers.ui.components.WallpaperSetOptions
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

/**
 * Screen that displays a preview of a wallpaper and provides options to set it as home/lock screen,
 * download it, share it, or edit it.
 */
@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalSharedTransitionApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
@Composable
fun WallpaperPreviewScreen(
    onBackPressed: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToCoinStore: () -> Unit = {},
    viewModel: WallpaperPreviewViewModel = hiltViewModel(),
    billingViewModel: BillingViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val wallpaperState by viewModel.wallpaperState.collectAsState()
    val isFavorite by viewModel.isFavorite
    val isPremiumUser by viewModel.isPremiumUser
    val isWallpaperPurchased by viewModel.isWallpaperPurchased
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
            Toast.makeText(context, R.string.preview_start_download_wallpaper, Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(
                context, R.string.preview_download_failed_permission_denied, Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshEditedImage()
        viewModel.refreshPurchaseState()
    }

    LaunchedEffect(navigateToUpgrade) {
        if (navigateToUpgrade) {
            viewModel.resetNavigateToUpgrade()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var showPaywall by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }

    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(state = hazeState)
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            containerColor = Color.Transparent
        ) { paddingValues ->
            AnimatedContent(
                targetState = wallpaperState,
                transitionSpec = {
                    // Faster transition for smoother feel
                    (fadeIn(animationSpec = tween(300)) + scaleIn(
                        initialScale = 1.02f, animationSpec = tween(300)
                    )).togetherWith(
                        fadeOut(animationSpec = tween(250)) + scaleOut(
                            targetScale = 0.98f, animationSpec = tween(250)
                        )
                    )
                },
                label = "WallpaperStateTransition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { targetState ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (targetState) {
                        is UiState.Loading -> {
                            // Keep loading indicator very simple to avoid clashing with next loading
                            LoadingState()
                        }

                        is UiState.Success -> {
                            val wallpaper = targetState.data
                            val editedBitmap by viewModel.editedBitmap
                            val isLocked =
                                (wallpaper.isPremium && !isPremiumUser) || (wallpaper.requiresPurchase && !isWallpaperPurchased)

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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    )
                                } else {
                                    LaunchedEffect(wallpaper.id) {
                                        viewModel.loadBlurredBackground()
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                )
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
                                    if (isLocked) {
                                        showPaywall = true
                                        return@WallpaperPreview
                                    }
                                    if (!isLoggedIn) {
                                        viewModel.setNeedLoginAction(WallpaperPreviewViewModel.LoginAction.SET_WALLPAPER)
                                        return@WallpaperPreview
                                    }
                                    viewModel.showSetWallpaperOptions(activity)
                                },
                                onDownload = {
                                    if (isLocked) {
                                        showPaywall = true
                                        return@WallpaperPreview
                                    }
                                    viewModel.download()
                                },
                                onShare = { viewModel.share(activity) },
                                onEdit = {
                                    if (isLocked) {
                                        showPaywall = true
                                        return@WallpaperPreview
                                    }
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
                                isWallpaperPurchased = isWallpaperPurchased,
                                editedBitmap = editedBitmap,
                                isProcessingWallpaper = isProcessingWallpaper,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )

                            val sheetState =
                                androidx.compose.material3.rememberModalBottomSheetState(
                                    skipPartiallyExpanded = true
                                )

                            if (showSetWallpaperOptions) {
                                ModalBottomSheet(
                                    onDismissRequest = { viewModel.hideSetWallpaperOptions() },
                                    sheetState = sheetState,
                                    containerColor = MaterialTheme.colorScheme.surface.copy(0.8f),
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    tonalElevation = 0.dp,
                                    dragHandle = null, // 我们在 WallpaperSetOptions 里自定了 Handle
                                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                                ) {
                                    WallpaperSetOptions(
                                        onSetHomeScreen = {
                                        viewModel.setWallpaper(activity!!, WallpaperTarget.HOME)
                                    }, onSetLockScreen = {
                                        viewModel.setWallpaper(activity!!, WallpaperTarget.LOCK)
                                    }, onSetBoth = {
                                        viewModel.setWallpaper(activity!!, WallpaperTarget.BOTH)
                                    }, onDismiss = {
                                        viewModel.hideSetWallpaperOptions()
                                    }, isProcessing = isProcessingWallpaper
                                    )
                                }
                            }
                        }

                        is UiState.Error -> {
                            ErrorState(
                                message = targetState.message, onRetry = { onBackPressed() })
                        }
                    }
                }
            }
        }

        if (needStoragePermission) {
            ConfirmDialog(
                onDismiss = { viewModel.resetPermissionRequest() },
                onConfirm = { permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) },
                title = stringResource(R.string.preview_storage_permission_required),
                message = stringResource(R.string.preview_storage_permission_rationale),
                confirmText = com.obscura.wallpapers.ui.theme.stringResource(R.string.preview_grant_permission),
                dismissText = com.obscura.wallpapers.ui.theme.stringResource(R.string.cancel)
            )
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

        if (showPaywall) {
            PaywallScreen(onDismiss = { showPaywall = false }, onNavigateToSubscription = {
                showPaywall = false
                onNavigateToSubscription()
            }, onNavigateToCoinStore = {
                showPaywall = false
                onNavigateToCoinStore()
            })
        }
    }
}
