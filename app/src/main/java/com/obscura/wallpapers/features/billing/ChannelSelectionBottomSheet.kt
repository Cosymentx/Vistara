package com.obscura.wallpapers.features.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.obscura.wallpapers.core.data.remote.service.CoinProduct
import com.obscura.wallpapers.core.data.remote.service.PayChannel
import com.obscura.wallpapers.ui.icons.ObscuraIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSelectionBottomSheet(
    product: CoinProduct, onDismiss: () -> Unit, onChannelSelected: (PayChannel) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cyanAccent = Color(0xFF00E5FF)

    // 使用 MaterialTheme.colorScheme.surface 作为基础，但在深色模式下强制使用更深的颜色以匹配设计
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor =
        if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxSheetHeight = screenHeight * 0.75f // 设置最大高度为屏幕的 75%

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = onSurfaceColor.copy(alpha = 0.2f)
            )
        },
        containerColor = surfaceColor,
        contentColor = onSurfaceColor,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .background(surfaceColor)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            // 头部保持固定
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Complete Purchase",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = onSurfaceColor,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Trusted by millions of users worldwide",
                        fontSize = 13.sp,
                        color = secondaryTextColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // 价格浮标 - 胶囊设计
                Surface(
                    color = cyanAccent.copy(alpha = 0.12f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, cyanAccent.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ObscuraIcons.Coin,
                            contentDescription = null,
                            tint = cyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = product.showPrice ?: "",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = cyanAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "PAYMENT METHOD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = cyanAccent.copy(alpha = 0.7f),
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 渠道列表使用 LazyColumn，并占据剩余空间
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false) // 关键：使用 weight 使列表可滚动且不强制填满
            ) {
                items(product.channels ?: emptyList()) { channel ->
                    ChannelItem(
                        channel = channel,
                        onClick = { onChannelSelected(channel) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 底部安全背书保持固定
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(
                            alpha = 0.02f
                        )
                    )
                    .padding(vertical = 12.dp), contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50).copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Secured by Bank-Level Encryption",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryTextColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelItem(
    channel: PayChannel, onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cyanAccent = Color(0xFF00E5FF)
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(
            width = 1.dp, color = containerColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(containerColor)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 图标容器
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = if (channel.icon?.startsWith("http") == true) channel.icon else "https://s3.rest-edu.com/${channel.icon}",
                            contentDescription = channel.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        text = channel.name ?: "Unknown",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val statusText =
                            if (channel.channel == "1") "Recommended" else "Instant Delivery"
                        val statusColor =
                            if (channel.channel == "1") cyanAccent else if (isDark) Color.White.copy(
                                alpha = 0.4f
                            ) else Color.Gray

                        if (channel.channel == "1") {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(cyanAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
