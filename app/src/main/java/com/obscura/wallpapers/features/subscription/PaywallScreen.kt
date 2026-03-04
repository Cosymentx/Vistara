package com.obscura.wallpapers.features.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.billing.model.PremiumFeature

/**
 * 付费墙弹窗
 * 当用户尝试访问高级功能时显示
 */
@Composable
fun PaywallScreen(
    feature: PremiumFeature,
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            // 关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                // 图标
                Text(
                    text = "👑",
                    fontSize = 64.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 标题
                Text(
                    text = feature.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 描述
                Text(
                    text = feature.description,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 升级按钮
                Button(
                    onClick = onSubscribe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.paywall_upgrade_to_premium),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 稍后再说
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.paywall_later),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
