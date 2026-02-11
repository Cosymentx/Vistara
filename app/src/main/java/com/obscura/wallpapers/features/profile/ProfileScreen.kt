package com.obscura.wallpapers.features.profile

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.components.HomeTabScaffold
import com.obscura.wallpapers.ui.components.LoginPromptDialog
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onFavoritesClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onAutoChangeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onTestToolsClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val username by viewModel.username.collectAsState()
    val userPhotoUrl by viewModel.userPhotoUrl.collectAsState()
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
    val isDebugMode by viewModel.isDebugMode.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val needLoginAction by viewModel.needLoginAction.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentViewModel = rememberUpdatedState(viewModel)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentViewModel.value.refreshUserData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { viewModel.refreshUserData() }

    needLoginAction?.let { action ->
        val message = when (action) {
            ProfileViewModel.LoginAction.FAVORITES -> stringResource(R.string.likes_login_required)
            ProfileViewModel.LoginAction.DOWNLOADS -> stringResource(R.string.library_login_required)
            ProfileViewModel.LoginAction.AUTO_WALLPAPER -> stringResource(R.string.cycler_login_required)
        }
        LoginPromptDialog(onDismiss = { viewModel.clearNeedLoginAction() }, onConfirm = {
            viewModel.clearNeedLoginAction(); onLoginClick()
        }, message = message)
    }

    HomeTabScaffold(
        title = null,
        showTopBar = false
    ) { paddingValues, contentModifier ->
        Column(
            modifier = contentModifier
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            ProfileHeader(
                username = username,
                userPhotoUrl = userPhotoUrl,
                isPremiumUser = isPremiumUser,
                isLoggedIn = isLoggedIn,
                onLoginClick = onLoginClick,
            )

            if (!isPremiumUser && isLoggedIn) {
//                PremiumBanner(
//                    onClick = { onUpgradeClick() },
//                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
//                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FeatureList(
                items = listOf(
                    FeatureEntry(
                        icon = ObscuraIcons.Favorite,
                        titleRes = R.string.my_favorites,
                        subtitleRes = R.string.my_favorites_desc,
                        onClick = { viewModel.checkLoginAndExecute(ProfileViewModel.LoginAction.FAVORITES) { onFavoritesClick() } }),
                    FeatureEntry(
                        icon = ObscuraIcons.Download,
                        titleRes = R.string.my_downloads,
                        subtitleRes = R.string.my_downloads_desc,
                        onClick = { viewModel.checkLoginAndExecute(ProfileViewModel.LoginAction.DOWNLOADS) { onDownloadsClick() } }),
                    FeatureEntry(
                        icon = ObscuraIcons.Refresh,
                        titleRes = R.string.cycler_auto_change_wallpaper,
                        subtitleRes = R.string.cycler_auto_change_wallpaper_desc,
                        onClick = { viewModel.checkLoginAndExecute(ProfileViewModel.LoginAction.AUTO_WALLPAPER) { onAutoChangeClick() } })
                )
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )

            FeatureList(
                items = listOf(
                    FeatureEntry(
                        icon = ObscuraIcons.Settings,
                        titleRes = R.string.settings_title,
                        subtitleRes = R.string.settings_desc,
                        onClick = onSettingsClick
                    ), FeatureEntry(
                        icon = ObscuraIcons.Star,
                        titleRes = R.string.feedback_title,
                        subtitleRes = R.string.feedback_desc,
                        onClick = onFeedbackClick
                    ), FeatureEntry(
                        icon = ObscuraIcons.Info,
                        titleRes = R.string.info_title,
                        subtitleRes = R.string.info_desc,
                        onClick = onAboutClick
                    )
                )
            )

            if (isDebugMode) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                FeatureList(
                    items = listOf(
                        FeatureEntry(
                            icon = Icons.Default.Build,
                            titleRes = R.string.test_title,
                            subtitleRes = R.string.test_desc,
                            onClick = onTestToolsClick
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

data class FeatureEntry(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int? = null,
    val onClick: () -> Unit
)

@Composable
private fun FeatureList(
    items: List<FeatureEntry>, modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            Surface(
                onClick = item.onClick,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 2.dp,
                shadowElevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
 
                    Spacer(modifier = Modifier.width(14.dp))
 
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(item.titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        item.subtitleRes?.let {
                            Text(
                                text = stringResource(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
 
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    username: String,
    userPhotoUrl: String?,
    isPremiumUser: Boolean,
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
) {
    val glowTransition = rememberInfiniteTransition(label = "premiumGlow")
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.7f, animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)
        ) {
            if (isPremiumUser) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            shadowElevation = 12f
                            shape = CircleShape
                            clip = true
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.6f),
                                    Color.Transparent
                                )
                            )
                        ))
            }
            Surface(shape = CircleShape, tonalElevation = 2.dp) {
                if (userPhotoUrl.isNullOrEmpty()) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(userPhotoUrl)
                            .crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
//                Surface(
//                    shape = RoundedCornerShape(12.dp),
//                    tonalElevation = 1.dp,
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(12.dp))
//                        .clickable { onDiamondClick() }
//                ) {
//                    Row(
//                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(
//                            imageVector = ObscuraIcons.Diamond,
//                            contentDescription = null,
//                            tint = MaterialTheme.colorScheme.primary
//                        )
//                        Spacer(modifier = Modifier.width(6.dp))
//                        Text(
//                            text = diamondBalance.toString(),
//                            style = MaterialTheme.typography.labelMedium
//                        )
//                    }
//                }

                if (!isLoggedIn) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        onClick = onLoginClick,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = stringResource(R.string.login),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
