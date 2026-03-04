package com.obscura.wallpapers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium 徽章组件
 * 用于标识需要订阅的内容
 */
@Composable
fun PremiumBadge(
    modifier: Modifier = Modifier,
    showText: Boolean = false
) {
    val gradientColors = listOf(
        Color(0xFFFFD700), // 金色
        Color(0xFFFFA500)  // 橙金色
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(gradientColors)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        
        if (showText) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "PRO",
                color = Color.White,
                fontSize = 10.sp,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * 简单的皇冠图标
 */
@Composable
fun CrownIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFFFD700)
) {
    Icon(
        imageVector = Icons.Default.Star,
        contentDescription = "Premium",
        tint = tint,
        modifier = modifier
    )
}
