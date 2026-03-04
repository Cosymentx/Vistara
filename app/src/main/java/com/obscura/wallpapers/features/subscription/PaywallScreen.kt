package com.obscura.wallpapers.features.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.obscura.wallpapers.core.billing.model.PremiumFeature

/**
 * 付费墙屏幕
 * 当用户尝试访问高级功能时显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    feature: PremiumFeature,
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = listOf(
        Color(0xFF1A1A2E),
        Color(0xFF16213E)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(gradientColors))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 关闭按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 标题
        Text(
            text = "🌟 解锁高级功能",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 功能描述
        Text(
            text = feature.getDescription(),
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 功能列表
        FeatureList()

        Spacer(modifier = Modifier.height(48.dp))

        // 订阅按钮
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
                text = "立即订阅",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 说明文字
        Text(
            text = "3天免费试用 • 随时取消",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeatureList() {
    val features = listOf(
        "✨ 壁纸编辑功能" to "裁剪、滤镜、调色等强大工具",
        "🎬 动态视频壁纸" to "让你的屏幕动起来",
        "🎨 高级壁纸" to "解锁所有精选壁纸",
        "🚫 无广告体验" to "享受纯净的使用体验",
        "☁️ 云端同步" to "多设备同步收藏"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        features.forEach { (title, description) ->
            FeatureItem(title = title, description = description)
        }
    }
}

@Composable
private fun FeatureItem(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
