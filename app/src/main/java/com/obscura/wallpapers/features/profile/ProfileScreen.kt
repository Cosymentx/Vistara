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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.SolidColor
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
    onSubscriptionClick: () -> Unit = {},
    onCoinPurchaseClick: () -> Unit = {},
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
        showTopBar = false
    ) { _, contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
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

            // Premium & Coin Section
            if (isLoggedIn) {
                PremiumCoinSection(
                    isPremiumUser = isPremiumUser,
                    onSubscriptionClick = onSubscriptionClick,
                    onCoinPurchaseClick = onCoinPurchaseClick
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section Title
            SectionHeader(
                title = "MY GALLERY",
                modifier = Modifier.padding(top = 8.dp)
            )

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

            Spacer(modifier = Modifier.height(120.dp))
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
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = if (item.accentColor != Color.Unspecified) item.accentColor.copy(
                                    alpha = 0.12f
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(item.titleRes),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            Surface(
                onClick = item.onClick,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(item.titleRes),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        item.subtitleRes?.let {
                            Text(
                                text = stringResource(it),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
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
        initialValue = 0.4f, targetValue = 0.9f, animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Blurred Background with deeper immersion
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
                                80f, 80f, Shader.TileMode.CLAMP
                            )
                            renderEffect = effect.asComposeRenderEffect()
                        }
                        alpha = 0.4f
                    })
        }

        // Elegant Gradient Scrim
        val isDark = isSystemInDarkTheme()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.background
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.background
                            )
                        }
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 32.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)
            ) {
                if (isPremiumUser) {
                    // Multi-layered glow
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.4f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .border(
                                width = 2.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .graphicsLayer {
                                rotationZ = glowAlpha * 360f // Subtle rotation
                            }
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 3.dp,
                        brush = if (isPremiumUser) {
                            Brush.linearGradient(
                                listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                            )
                        } else {
                            SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    ),
                    modifier = Modifier.size(86.dp),
                    shadowElevation = 12.dp
                ) {
                    if (userPhotoUrl.isNullOrEmpty()) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(userPhotoUrl)
                                .crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (isPremiumUser) {
                    Surface(
                        color = Color(0xFFFFD700),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            imageVector = ObscuraIcons.Crown,
                            contentDescription = null,
                            tint = Color(0xFF534600),
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = username,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!isLoggedIn) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    onClick = onLoginClick,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = stringResource(R.string.login),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp)
                    )
                }
            } else if (isPremiumUser) {
                Surface(
                    color = Color(0xFFFFD700).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "PREMIUM MEMBER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            fontSize = 10.sp
                        ),
                        color = Color(0xFFD4AF37),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumCoinSection(
    isPremiumUser: Boolean,
    onSubscriptionClick: () -> Unit,
    onCoinPurchaseClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardBackground =
        if (isDark) Color(0xFF1A1A1A).copy(alpha = 0.8f) 
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    val borderColor =
        if (isDark) Color.White.copy(alpha = 0.15f) 
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Subscription Card - Gold/Premium Theme
        Surface(
            onClick = onSubscriptionClick,
//            color = cardBackground,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(
                1.5.dp,
                if (isPremiumUser) Brush.linearGradient(
                    listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFFA500)
                    )
                )
                else SolidColor(borderColor)
            ),
            modifier = Modifier
                .weight(1f)
                .height(120.dp),
            shadowElevation = if (isDark) 0.dp else 4.dp
        ) {
            Box {
                // Background Gradient Glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (isPremiumUser) Color(0xFFFFD700).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize()
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = if (isPremiumUser) Color(0xFFFFD700).copy(alpha = 0.15f) 
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPremiumUser) ObscuraIcons.Crown else Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isPremiumUser) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isPremiumUser) stringResource(R.string.profile_premium_member)
                        else stringResource(R.string.profile_upgrade_membership),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        ),
                        color = if (isPremiumUser) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }

        // Coin Purchase Card - Cyan/Vibrant Theme
        Surface(
            onClick = onCoinPurchaseClick,
//            color = cardBackground,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, borderColor),
            modifier = Modifier
                .weight(1f)
                .height(120.dp),
            shadowElevation = if (isDark) 0.dp else 4.dp
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF00E5FF).copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize()
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = Color(0xFF00E5FF).copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ObscuraIcons.Coin,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.profile_purchase_coins),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        ),
                        color = Color(0xFF00E5FF),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
