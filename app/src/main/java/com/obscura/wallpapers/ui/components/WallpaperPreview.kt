package com.obscura.wallpapers.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.Resolution
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WallpaperPreview(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    onBackPressed: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetWallpaper: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    isInfoExpanded: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    onToggleInfo: () -> Unit = {},
    onPreview: () -> Unit = {},
    isPremiumUser: Boolean = false,
    isWallpaperPurchased: Boolean = false,
    editedBitmap: Bitmap? = null,
    isProcessingWallpaper: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    var showControls by remember { mutableStateOf(true) }
    val isLocked = (wallpaper.isPremium && !isPremiumUser) || (wallpaper.requiresPurchase && !isWallpaperPurchased)
    val isOwned = (wallpaper.isPremium && isPremiumUser) || (wallpaper.requiresPurchase && isWallpaperPurchased)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Wallpaper content
        val contentModifier = Modifier.fillMaxSize().run {
            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    this@run.sharedElement(
                        rememberSharedContentState(key = "wallpaper_image_${wallpaper.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            } else {
                this
            }
        }

        if (editedBitmap != null) {
            ZoomableBitmapImage(
                bitmap = editedBitmap,
                contentDescription = wallpaper.title ?: "",
                modifier = contentModifier,
                onTap = { showControls = !showControls })
        } else if (wallpaper.isLive) {
            LiveVideoPlayer(
                wallpaper = wallpaper,
                modifier = contentModifier,
                onTap = { showControls = !showControls })
        } else {
            ZoomableImage(
                imageUrl = wallpaper.url ?: "",
                contentDescription = wallpaper.title ?: "",
                modifier = contentModifier,
                onTap = { showControls = !showControls },
                thumbnailUrl = wallpaper.thumbnailUrl
            )
        }

        // Top Control Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { -it / 2 },
            exit = fadeOut(animationSpec = tween(400)) + slideOutVertically(
                animationSpec = tween(
                    400
                )
            ) { -it / 2 },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .height(64.dp), contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 72.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    wallpaper.title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Premium/Purchase Badge
                    when {
                        wallpaper.isPremium && !isPremiumUser -> {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp)),
                                color = Color(0xFFFFD700).copy(alpha = 0.9f),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "👑",
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.premium),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Black.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        wallpaper.requiresPurchase && !isWallpaperPurchased -> {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp)),
                                color = Color(0xFF00BCD4).copy(alpha = 0.9f),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "💎",
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = wallpaper.purchasePrice.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        isOwned -> {
                            // 已拥有/已订阅状态
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp)),
                                color = Color(0xFF4CAF50).copy(alpha = 0.9f),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.wallpaper_purchased),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (!wallpaper.isLive) {
                    IconButton(
                        onClick = onPreview,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = ObscuraIcons.WallpaperPreview,
                            contentDescription = stringResource(R.string.preview_wallpaper),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Control Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { it / 2 },
            exit = fadeOut(animationSpec = tween(400)) + slideOutVertically(
                animationSpec = tween(
                    400
                )
            ) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Author and Source
                Row(
                    modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.author, wallpaper.author ?: "Unknown"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.source, wallpaper.source ?: "Vistara"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Section
                AnimatedVisibility(visible = isInfoExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(12.dp)
                    ) {
                        InfoRow(
                            label = stringResource(R.string.resolution, 0, 0).split(":")[0],
                            value = "${wallpaper.resolution?.width ?: 0} x ${wallpaper.resolution?.height ?: 0}"
                        )
                        InfoRow(
                            label = stringResource(R.string.wallpaper_type, "").split(":")[0],
                            value = if (wallpaper.isLive) stringResource(R.string.live_wallpaper) else stringResource(
                                R.string.static_wallpaper
                            )
                        )

                        if (wallpaper.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                wallpaper.tags.forEach { tag ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp, vertical = 4.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        onClick = onToggleFavorite,
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF4081) else Color.White
                    )

                    if (!wallpaper.isLive) {
                        GlassIconButton(
                            onClick = onEdit,
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            isLocked = isLocked,
                            tint = if (isLocked) Color.White.copy(alpha = 0.6f) else Color.White
                        )
                    }

                    Box(contentAlignment = Alignment.Center) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(52.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                        }
                        GlassIconButton(
                            onClick = onDownload,
                            enabled = !isDownloading,
                            painter = painterResource(id = R.drawable.ic_download),
                            contentDescription = "Download",
                            isLocked = isLocked
                        )
                    }

                    GlassIconButton(
                        onClick = onShare,
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share"
                    )

                    GlassIconButton(
                        onClick = onToggleInfo,
                        imageVector = if (isInfoExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Info",
                        active = isInfoExpanded
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Modern Primary Set Wallpaper Button
                val buttonBrush = Brush.linearGradient(
                    colors = if (isLocked) {
                        listOf(Color(0xFF607D8B), Color(0xFF455A64)) // 灰色调表示未解锁
                    } else {
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            spotColor = MaterialTheme.colorScheme.primary
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(buttonBrush)
                        .clickable(enabled = !isProcessingWallpaper, onClick = onSetWallpaper),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isProcessingWallpaper) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        } else {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = if (isProcessingWallpaper) stringResource(R.string.preview_setting_wallpaper)
                            else if (isLocked)
                                stringResource(R.string.preview_upgrade_to_unlock)
                            else stringResource(R.string.preview_set_as_wallpaper),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White
        )
    }
}

@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    imageVector: ImageVector? = null,
    contentDescription: String? = null,
    tint: Color = Color.White,
    active: Boolean = false,
    isLocked: Boolean = false
) {
    val scale by animateFloatAsState(if (active) 1.15f else 1f, label = "scale")

    Box(
        modifier = Modifier
            .size(54.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                else Color.White.copy(alpha = 0.15f)
            )
            .border(
                width = 1.dp,
                color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else Color.White.copy(alpha = 0.25f),
                shape = CircleShape
            ), contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (painter != null) {
                    Icon(
                        painter = painter,
                        contentDescription = contentDescription,
                        tint = if (isLocked) tint.copy(alpha = 0.5f) else tint
                    )
                } else if (imageVector != null) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = contentDescription,
                        tint = if (isLocked) tint.copy(alpha = 0.5f) else tint
                    )
                }

                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperSetOptions(
    onSetHomeScreen: () -> Unit,
    onSetLockScreen: () -> Unit,
    onSetBoth: () -> Unit,
    onDismiss: () -> Unit,
    isProcessing: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle for aesthetics
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.preview_set_as),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(32.dp))

            SetOptionItem(
                title = stringResource(R.string.preview_home_screen_wallpaper),
                description = "Apply to your home screen only",
                icon = Icons.Default.Home,
                onClick = onSetHomeScreen,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                enabled = !isProcessing
            )

            Spacer(modifier = Modifier.height(12.dp))

            SetOptionItem(
                title = stringResource(R.string.preview_lock_screen_wallpaper),
                description = "Apply to your lock screen only",
                icon = Icons.Default.Lock,
                onClick = onSetLockScreen,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                enabled = !isProcessing
            )

            Spacer(modifier = Modifier.height(12.dp))

            SetOptionItem(
                title = stringResource(R.string.preview_home_and_lock_screen),
                description = "Set for both screens at once",
                icon = Icons.Default.Star,
                onClick = onSetBoth,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                isPremium = true,
                enabled = !isProcessing
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isProcessing) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.wallpaper_setting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun SetOptionItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    isPremium: Boolean = false,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.1f)),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor
                    )
                    if (isPremium) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview
@Composable
fun WallpaperPreviewPreview() {
    ObscuraTheme {
        WallpaperPreview(
            wallpaper = Wallpaper(
                id = "1",
                title = "Beautiful Landscape",
                url = "https://s3.us-west-2.amazonaws.com/images.unsplash.com/small/photo-1739911013984-8b3bf696a182",
                thumbnailUrl = "https://s3.us-west-2.amazonaws.com/images.unsplash.com/small/photo-1739911013984-8b3bf696a182",
                author = "John Doe",
                source = "Unsplash",
                isPremium = true,
                isLive = false,
                tags = listOf("nature", "landscape"),
                resolution = Resolution(1920, 1080)
            ),
            onBackPressed = {},
            onToggleInfo = {},
            onDownload = {},
            onShare = {},
            onSetWallpaper = {},
            isPremiumUser = false,
            isInfoExpanded = true,
            isDownloading = false,
            isFavorite = false,
            onEdit = {},
            onToggleFavorite = {})
    }
}
