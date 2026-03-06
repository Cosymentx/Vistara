package com.obscura.wallpapers.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * 通用确认对话框 - 升级为全屏沉浸式毛玻璃效果
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
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false
    ),
    hazeState: HazeState? = null
) {
    val globalHazeState = LocalHazeState.current
    val localHazeState = hazeState ?: globalHazeState ?: remember { HazeState() }
    val view = LocalView.current

    Dialog(onDismissRequest = onDismiss, properties = properties) {
        LaunchedEffect(Unit) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                window.setDimAmount(0f)
                window.setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .run {
                    if (hazeState == null && globalHazeState == null) hazeSource(state = localHazeState) else this
                }
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(28.dp))
                    .hazeEffect(
                        state = localHazeState,
                        style = HazeStyle(
                            tint = HazeTint(Color.White.copy(alpha = 0.12f)),
                            blurRadius = 30.dp,
                            noiseFactor = 0.15f
                        )
                    )
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .clickable(enabled = false) { }
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 24.dp)
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
                            color = Color.White
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

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(50.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = dismissText,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Button(
                            onClick = onConfirm,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .height(50.dp)
                                .weight(1f)
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
}
