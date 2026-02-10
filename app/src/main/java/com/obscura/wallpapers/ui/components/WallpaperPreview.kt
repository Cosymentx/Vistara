package com.obscura.wallpapers.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.Resolution
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.theme.stringResource

@Composable
fun WallpaperPreview(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    isInfoExpanded: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    onBackPressed: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleInfo: () -> Unit = {},
    onSetWallpaper: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onPreview: () -> Unit = {},
    modifier: Modifier = Modifier,
    isPremiumUser: Boolean = false,
    editedBitmap: Bitmap? = null,
    isProcessingWallpaper: Boolean = false
) {
    var showControls by remember { mutableStateOf(true) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 壁纸图片或视频 - 全屏显示，支持缩放
        if (editedBitmap != null) {
            // 显示编辑后的图片，使用可缩放组件
            ZoomableBitmapImage(
                bitmap = editedBitmap,
                contentDescription = wallpaper.title ?: "",
                modifier = Modifier.fillMaxSize(),
                onTap = { showControls = !showControls })
        } else if (wallpaper.isLive) {
            // 显示动态壁纸（视频）
            // 使用remember确保在wallpaper.id变化时重建组件
            val videoKey = remember { wallpaper.id }
            LiveVideoPlayer(
                wallpaper = wallpaper, modifier = Modifier.fillMaxSize(), onTap = { showControls = !showControls })
        } else {
            // 显示原始图片
            ZoomableImage(
                imageUrl = wallpaper.url ?: "",
                contentDescription = wallpaper.title ?: "",
                modifier = Modifier.fillMaxSize(),
                onTap = { showControls = !showControls })
        }

        // 顶部控制栏 (状态栏区域) - 半透明渐变背景
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { -it / 3 },
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                animationSpec = tween(
                    300
                )
            ) { -it / 3 },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.4f), Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding() // 确保内容不会被状态栏遮挡
                    .height(56.dp)
            ) {
                IconButton(
                    onClick = onBackPressed, modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "", tint = Color.White
                    )
                }

                // 如果壁纸有标题，显示标题
                wallpaper.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 56.dp) // 留出两边按钮的空间
                    )
                }

                // 如果是图片壁纸，显示预览按钮
                if (!wallpaper.isLive) {
                    IconButton(
                        onClick = onPreview,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .size(40.dp)
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

        // 底部控制栏 - 半透明渐变背景
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { it / 3 },
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                animationSpec = tween(
                    300
                )
            ) { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent, Color.Black.copy(alpha = 0.5f), Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .navigationBarsPadding() // 确保内容不会被导航栏遮挡
                    .padding(16.dp)
            ) {
                // 作者信息和来源
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.author, wallpaper.author ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )

                        Text(
                            text = stringResource(R.string.source, wallpaper.source ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // 展开的详细信息
                AnimatedVisibility(visible = isInfoExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        // 分辨率
                        Text(
                            text = stringResource(R.string.resolution, wallpaper.resolution?.width ?: 0, wallpaper.resolution?.height ?: 0),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        // 壁纸类型
                        Text(
                            text = stringResource(
                                R.string.wallpaper_type,
                                if (wallpaper.isLive) stringResource(R.string.live_wallpaper) else stringResource(R.string.static_wallpaper)
                            ), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f)
                        )

                        // 标签显示
                        if (wallpaper.tags.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.tags),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(bottom = 4.dp)
                            ) {
                                wallpaper.tags.forEach { tag ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                // 操作按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 收藏按钮
                    IconButton(
                        onClick = onToggleFavorite, modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "取消收藏" else "收藏",
                            tint = if (isFavorite) Color.Red else Color.White
                        )
                    }

                    // 编辑按钮 - 仅对静态壁纸显示且不是动态壁纸时
                    if (!wallpaper.isLive) {
                        // 如果壁纸不是高级壁纸，或者用户是高级用户，或者壁纸已经购买（isPremium为false），则可以编辑
                        val canEdit = !wallpaper.isPremium || isPremiumUser
                        IconButton(
                            onClick = onEdit,
                            enabled = canEdit,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (canEdit) 0.2f else 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit),
                                tint = if (canEdit) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // 下载按钮
                    // 如果壁纸不是高级壁纸，或者用户是高级用户，或者壁纸已经购买（isPremium为false），则可以下载
                    val canDownload = !wallpaper.isPremium || isPremiumUser
                    Box(
                        contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)
                    ) {
                        // 下载进度指示器
                        if (isDownloading) {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.2f),
                                strokeWidth = 2.dp
                            )
                        }

                        IconButton(
                            onClick = onDownload,
                            enabled = !isDownloading,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (!isDownloading) 0.2f else 0.1f))
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_download),
                                    contentDescription = stringResource(R.string.download),
                                    tint =Color.White
//                                    tint = if (canDownload && !isDownloading) Color.White else Color.White.copy(
//                                        alpha = 0.5f
//                                    )
                                )
                            }
                        }

                        // 添加标识图标
//                        if (wallpaper.isPremium && !isPremiumUser) {
//                            // 高级壁纸显示皇冠图标
//                            Text(
//                                text = "👑",
//                                style = MaterialTheme.typography.labelSmall,
//                                modifier = Modifier
//                                    .align(Alignment.TopEnd)
//                                    .offset(x = 1.dp, y = (-5).dp)
//                            )
//                        } else if (wallpaper.isLive && !isPremiumUser) {
//                            // 普通动态壁纸显示钻石图标
//                            Text(
//                                text = "💎",
//                                style = MaterialTheme.typography.labelSmall,
//                                modifier = Modifier
//                                    .align(Alignment.TopEnd)
//                                    .offset(x = 1.dp, y = (-5).dp)
//                            )
//                        }
                    }

                    // 分享按钮
                    IconButton(
                        onClick = onShare, modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share, contentDescription = stringResource(R.string.share), tint = Color.White
                        )
                    }

                    // 信息展开/收起按钮
                    IconButton(
                        onClick = onToggleInfo, modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (isInfoExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isInfoExpanded) stringResource(R.string.preview_collapse_info) else stringResource(R.string.preview_expand_info),
                            tint = Color.White
                        )
                    }
                }

                // 设置壁纸按钮 - 主要操作
                // 如果壁纸不是高级壁纸，或者用户是高级用户，或者壁纸已经购买（isPremium为false），则可以设置壁纸
                val canSetWallpaper = !wallpaper.isPremium || isPremiumUser
                Button(
                    onClick = onSetWallpaper,
                    // 当正在处理壁纸时禁用按钮，防止重复点击
                    enabled = !isProcessingWallpaper,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        // 自定义禁用状态的颜色，使其保持高可见度
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        // 自定义禁用状态的文字颜色，使其保持高可见度
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        // 正常状态的颜色
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isProcessingWallpaper) {
                            // 显示加载指示器，使用高对比度的颜色
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary, // 确保加载指示器清晰可见
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else if (wallpaper.isPremium) {
                            // 对于高级壁纸和非高级用户，显示皇冠图标
//                            Text(
//                                text = "👑", // 皇冠emoji
//                                style = MaterialTheme.typography.bodyMedium,
//                                modifier = Modifier.padding(end = 4.dp)
//                            )
                        } else if (wallpaper.isLive && !isPremiumUser) {
                            // 对于普通动态壁纸和非高级用户，显示钻石图标
//                            Text(
//                                text = "💎", // 钻石emoji
//                                style = MaterialTheme.typography.bodyMedium,
//                                modifier = Modifier.padding(end = 4.dp)
//                            )
                        }
                        Text(
                            text = if (isProcessingWallpaper) stringResource(R.string.preview_setting_wallpaper)
                            else  stringResource(R.string.preview_set_as_wallpaper),
//                            else stringResource(R.string.preview_upgrade_to_unlock),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            // 不需要显式设置颜色，因为我们已经在ButtonDefaults中设置了disabledContentColor
                        )
                    }
                }
            }
        }

        // 注意：点击屏幕切换控制栏显示状态的功能已移至ZoomableImage组件中
    }
}

@Composable
fun WallpaperSetOptions(
    onSetHomeScreen: () -> Unit, onSetLockScreen: () -> Unit, onSetBoth: () -> Unit, onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.preview_set_as), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = onSetHomeScreen, modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(text = stringResource(R.string.preview_home_screen_wallpaper))
            }

            Button(
                onClick = onSetLockScreen, modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(text = stringResource(R.string.preview_lock_screen_wallpaper))
            }

            Button(
                onClick = onSetBoth, modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(text = stringResource(R.string.preview_home_and_lock_screen))
            }

            TextButton(
                onClick = onDismiss, modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(text = stringResource(R.string.cancel))
            }
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
