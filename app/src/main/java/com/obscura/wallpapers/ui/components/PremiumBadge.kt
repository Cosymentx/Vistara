package com.obscura.wallpapers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium 徽章组件
 * 用于标识高级内容
 */
@Composable
fun PremiumBadge(
    modifier: Modifier = Modifier,
    text: String = "PRO"
) {
    Row(
        modifier = modifier
            .background(
                color = Color(0xFFFFD700),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "👑",
            fontSize = 12.sp
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/**
 * 皇冠图标
 */
@Composable
fun CrownIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFFFD700)
) {
    Text(
        text = "👑",
        fontSize = 16.sp,
        modifier = modifier
    )
}
