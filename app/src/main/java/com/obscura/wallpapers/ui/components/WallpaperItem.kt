package com.obscura.wallpapers.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.Resolution
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.theme.stringResource

/**
 * 壁纸列表项组件
 * @param wallpaper 壁纸数据
 * @param onClick 点击事件回调
 * @param modifier 可选修饰符
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WallpaperItem(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPremiumUser: Boolean = false,
    isWallpaperPurchased: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val isOwned = isWallpaperPurchased || (wallpaper.isPremium && isPremiumUser)

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 壁纸图片
            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = wallpaper.title ?: stringResource(R.string.app_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .run {
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
            )

            // 动态壁纸标记
            if (wallpaper.isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Live Wallpaper",
                        tint = Color.White
                    )
                }
            }

            // 顶部右角徽章 - 显示购买/订阅状态 (Glassmorphism Style)
            if ((wallpaper.isPremium && !isPremiumUser) || (wallpaper.requiresPurchase && !isWallpaperPurchased) || isOwned) {
                val accentColor = when {
                    wallpaper.isPremium && !isPremiumUser -> Color(0xFFFFD700)
                    wallpaper.requiresPurchase && !isWallpaperPurchased -> Color(0xFF00E5FF)
                    else -> Color(0xFF4CAF50)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(
                                width = 0.5.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.5f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            accentColor.copy(alpha = 0.15f),
                                            Color.Transparent,
                                            accentColor.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                // Subtle glow behind icon
                                Surface(
                                    modifier = Modifier.size(14.dp),
                                    color = accentColor.copy(alpha = 0.2f),
                                    shape = CircleShape
                                ) {}
                                Icon(
                                    imageVector = when {
                                        wallpaper.isPremium && !isPremiumUser -> ObscuraIcons.Crown
                                        wallpaper.requiresPurchase && !isWallpaperPurchased -> ObscuraIcons.Diamond
                                        else -> Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(10.dp)
                                )
                            }

                            val badgeText = when {
                                wallpaper.isPremium && !isPremiumUser -> stringResource(R.string.premium)
                                wallpaper.requiresPurchase && !isWallpaperPurchased -> wallpaper.purchasePrice.toString()
                                else -> stringResource(R.string.wallpaper_purchased)
                            }

                            if (badgeText.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = badgeText.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            }

            // 添加底部渐变和标题
            if (!wallpaper.title.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent, Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = wallpaper.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun WallpaperItemPreview() {
    ObscuraTheme {
        WallpaperItem(
            wallpaper = Wallpaper(
                id = "1",
                title = "Beautiful Landscape",
                url = "https://example.com/wallpaper.jpg",
                thumbnailUrl = "https://example.com/wallpaper_thumb.jpg",
                author = "John Doe",
                source = "Unsplash",
                isPremium = true,
                isLive = false,
                tags = listOf("nature", "landscape"),
                resolution = Resolution(1920, 1080)
            ), onClick = {})
    }
}
