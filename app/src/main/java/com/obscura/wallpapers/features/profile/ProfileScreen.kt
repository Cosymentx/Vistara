package com.obscura.wallpapers.features.profile

import android.graphics.Shader
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import android.graphics.RenderEffect as AndroidRenderEffect

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
        title = null, showTopBar = false
    ) { paddingValues, contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            ProfileHeader(
                username = username,
                userPhotoUrl = userPhotoUrl,
                isPremiumUser = isPremiumUser,
                isLoggedIn = isLoggedIn,
                onLoginClick = onLoginClick,
            )

            // Section Title
            SectionHeader(title = "MY GALLERY", modifier = Modifier.padding(top = 30.dp))

            FeatureGrid(
                items = listOf(
                    FeatureEntry(
                        icon = ObscuraIcons.Favorite,
                        titleRes = R.string.my_favorites,
                        subtitleRes = R.string.my_favorites_desc,
                        accentColor = Color(0xFFFF4081),
                        onClick = { viewModel.checkLoginAndExecute(ProfileViewModel.LoginAction.FAVORITES) { onFavoritesClick() } }),
                    FeatureEntry(
                        icon = ObscuraIcons.Download,
                        titleRes = R.string.my_downloads,
                        subtitleRes = R.string.my_downloads_desc,
                        accentColor = Color(0xFF00E5FF),
                        onClick = { viewModel.checkLoginAndExecute(ProfileViewModel.LoginAction.DOWNLOADS) { onDownloadsClick() } }),
                    FeatureEntry(
                        icon = ObscuraIcons.Refresh,
                        titleRes = R.string.cycler_auto_change_wallpaper,
                        subtitleRes = R.string.cycler_auto_change_wallpaper_desc,
                        accentColor = Color(0xFF7C4DFF),
                        onClick = { viewModel.checkLoginAndExecute(ProfileViewModel.LoginAction.AUTO_WALLPAPER) { onAutoChangeClick() } })
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(title = stringResource(R.string.settings_title))

            FeatureCardList(
                items = listOf(
                    FeatureEntry(
                        icon = ObscuraIcons.Settings,
                        titleRes = R.string.settings_title,
                        subtitleRes = R.string.settings_desc,
                        onClick = onSettingsClick
                    ), FeatureEntry(
                        icon = Icons.Default.Star,
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
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "DEVELOPER OPTIONS")
                FeatureCardList(
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

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

data class FeatureEntry(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int? = null,
    val accentColor: Color = Color.Unspecified,
    val onClick: () -> Unit
)

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Black, letterSpacing = 1.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun FeatureGrid(
    items: List<FeatureEntry>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            Surface(
                onClick = item.onClick,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (item.accentColor != Color.Unspecified) item.accentColor.copy(
                                    alpha = 0.15f
                                )
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (item.accentColor != Color.Unspecified) item.accentColor
                            else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(item.titleRes),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCardList(
    items: List<FeatureEntry>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            Surface(
                onClick = item.onClick,
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(item.titleRes),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        item.subtitleRes?.let {
                            Text(
                                text = stringResource(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
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
        initialValue = 0.3f, targetValue = 0.8f, animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        // Blurred Background
        if (!userPhotoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(userPhotoUrl)
                    .crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            val effect = AndroidRenderEffect.createBlurEffect(
                                60f, 60f, Shader.TileMode.CLAMP
                            )
                            renderEffect = effect.asComposeRenderEffect()
                        }
                        alpha = 0.5f
                    })
        }

        // Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
//                .background(
//                    Brush.verticalGradient(
//                        colors = listOf(
//                            Color.Transparent,
//                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
//                            MaterialTheme.colorScheme.background
//                        )
//                    )
//                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)
            ) {
                if (isPremiumUser) {
                    Box(modifier = Modifier
                        .size(94.dp)
                        .graphicsLayer {
                            shadowElevation = 20f
                            shape = CircleShape
                            clip = true
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.7f),
                                    Color.Transparent
                                )
                            )
                        ))
                }

                Surface(
                    shape = CircleShape,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    border = if (isPremiumUser) BorderStroke(
                        2.dp, MaterialTheme.colorScheme.primary
                    ) else null
                ) {
                    if (userPhotoUrl.isNullOrEmpty()) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(userPhotoUrl)
                                .crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                if (isPremiumUser) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = username,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!isLoggedIn) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    onClick = onLoginClick,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = stringResource(R.string.login),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            } else if (isPremiumUser) {
                Text(
                    text = "PREMIUM MEMBER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black, letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
