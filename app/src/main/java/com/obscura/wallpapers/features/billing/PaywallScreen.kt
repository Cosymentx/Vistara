package com.obscura.wallpapers.features.billing

import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * 通用付费墙弹窗 - 优化为毛玻璃效果
 */
@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToCoinStore: () -> Unit,
    hazeState: HazeState? = null
) {
    val goldAccent = Color(0xFFFFD700)
    val cyanAccent = Color(0xFF00E5FF)
    val localHazeState = hazeState ?: remember { HazeState() }
    val view = LocalView.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        LaunchedEffect(Unit) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                window.setDimAmount(0f) // Remove default dialog dim to let haze handle it
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .run {
                    if (hazeState == null) hazeSource(state = localHazeState) else this
                }
                .background(Color.Black.copy(alpha = 0.3f)) // Custom scrim
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(32.dp))
                    .hazeEffect(
                        state = localHazeState,
                        style = HazeStyle(
                            tint = HazeTint(Color.White.copy(alpha = 0.15f)),
                            blurRadius = 30.dp,
                            noiseFactor = 0.15f
                        )
                    )
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                // 背景发光效果
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    goldAccent.copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                radius = 800f
                            )
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(vertical = 40.dp, horizontal = 28.dp)
                        .fillMaxWidth()
                ) {
                    // Header with Icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(goldAccent.copy(alpha = 0.1f), CircleShape)
                                .border(0.5.dp, goldAccent.copy(alpha = 0.2f), CircleShape)
                        )
                        Icon(
                            imageVector = ObscuraIcons.Crown,
                            contentDescription = null,
                            tint = goldAccent,
                            modifier = Modifier.size(50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.paywall_unlock_premium_content),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.paywall_description),
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Option 1: Subscription (Recommended)
                    Button(
                        onClick = onNavigateToSubscription,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = goldAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = ObscuraIcons.Crown, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                text = stringResource(R.string.paywall_get_premium_access),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 2: Individual Purchase with Coins
                    OutlinedButton(
                        onClick = onNavigateToCoinStore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        border = BorderStroke(
                            1.dp,
                            Brush.linearGradient(listOf(cyanAccent, cyanAccent.copy(alpha = 0.3f)))
                        ),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cyanAccent)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = ObscuraIcons.Coin, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                text = stringResource(R.string.paywall_unlock_with_coins),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = stringResource(R.string.paywall_maybe_later),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onDismiss)
                    )
                }
                
                // Close Button Top Right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
