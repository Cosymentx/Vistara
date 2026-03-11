package com.obscura.wallpapers.features.billing

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.icons.ObscuraIcons

/**
 * 通用付费墙弹窗 - 优化为毛玻璃效果
 */
@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToCoinStore: () -> Unit,
) {
    val goldAccent = Color(0xFFFFD700)
    val cyanAccent = Color(0xFF00E5FF)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(
        onDismissRequest = onDismiss, properties = DialogProperties(
            dismissOnBackPress = true, dismissOnClickOutside = true, usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(0.8f),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(32.dp))
                .border(
                    width = 1.dp, brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f)
                        )
                    ), shape = RoundedCornerShape(32.dp)
                )
        ) {
            // 背景发光效果
            Box(
                modifier = Modifier.background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            goldAccent.copy(alpha = 0.1f), Color.Transparent
                        ), radius = 800f
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
                    contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)
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
                    color = textColor,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.paywall_description),
                    fontSize = 15.sp,
                    color = textColor.copy(alpha = 0.6f),
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
                        containerColor = goldAccent, contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = ObscuraIcons.Crown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
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
                        Icon(
                            imageVector = ObscuraIcons.Coin,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
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
                    color = textColor.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }
        }
    }
}
