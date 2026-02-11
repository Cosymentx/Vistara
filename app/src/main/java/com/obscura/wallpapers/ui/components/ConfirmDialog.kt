package com.obscura.wallpapers.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DividerDefaults

/**
 * 通用确认对话框
 * 用于各种需要用户确认的场景
 *
 * @param onDismiss 取消回调
 * @param onConfirm 确认回调
 * @param title 对话框标题
 * @param message 对话框消息
 * @param confirmText 确认按钮文本
 * @param dismissText 取消按钮文本
 * @param isLoading 是否正在加载中
 * @param properties 对话框属性
 */
@Composable
fun ConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    isLoading: Boolean = false,
    properties: DialogProperties = DialogProperties()
) {
    Dialog(onDismissRequest = onDismiss, properties = properties) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        Color.Transparent
                    )
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                val shimmer = rememberInfiniteTransition(label = "dialogShimmer")
                val shimmerX by shimmer.animateFloat(
                    initialValue = -140f,
                    targetValue = 420f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1800, easing = LinearOutSlowInEasing)
                    ),
                    label = "dialogShimmerX"
                )

                Box {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.18f),
                                        Color.Transparent
                                    ),
                                    start = Offset(shimmerX, 0f),
                                    end = Offset(shimmerX + 160f, 0f)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(
                    thickness = DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .height(44.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = dismissText,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .height(44.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Box {
                            Text(
                                text = confirmText,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.22f),
                                                Color.Transparent
                                            ),
                                            start = Offset(shimmerX, 0f),
                                            end = Offset(shimmerX + 120f, 0f)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
