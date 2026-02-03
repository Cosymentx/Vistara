package com.obscura.wallpapers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.obscura.wallpapers.data.model.Resolution
import com.obscura.wallpapers.data.model.Wallpaper
import com.obscura.wallpapers.ui.theme.VistaraTheme

/**
 * 壁纸列表项组件
 * @param wallpaper 壁纸数据
 * @param isPremium 是否为高级壁纸
 * @param onClick 点击事件回调
 * @param modifier 可选修饰符
 */
@Composable
fun WallpaperItem(
    wallpaper: Wallpaper,
    isPremium: Boolean = wallpaper.isPremium,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                contentDescription = wallpaper.title ?: "Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 高级标记
            if (isPremium) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "👑",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            } else if (wallpaper.isLive) {
                // 普通动态壁纸显示钻石图标
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "💎",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // 动态壁纸标记
            if (wallpaper.isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Live Wallpaper", tint = Color.White)
                }
            }

            // 添加底部渐变和标题
            if (wallpaper.title != null && wallpaper.title.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
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
    VistaraTheme {
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
