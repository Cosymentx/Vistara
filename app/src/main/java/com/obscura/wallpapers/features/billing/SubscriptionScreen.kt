package com.obscura.wallpapers.features.billing

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.billingclient.api.ProductDetails
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import androidx.core.net.toUri

/**
 * 订阅页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit, viewModel: BillingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isPremium by viewModel.isPremium.collectAsState()
    val availableSubscriptions by viewModel.availableSubscriptions.collectAsState()
    val backendSubscriptionProducts by viewModel.subscriptionProducts.collectAsState()
    val isLoadingProducts by viewModel.isLoadingProducts.collectAsState()
    val uiState by viewModel.subscriptionUiState.collectAsState()

    var selectedProductIndex by remember { mutableIntStateOf(1) } // 默认选中年度订阅
    var showChannelDialog by remember { mutableStateOf(false) }
    var selectedProductForChannels by remember {
        mutableStateOf<com.obscura.wallpapers.core.data.remote.service.CoinProduct?>(
            null
        )
    }

    val isDark = isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(
            Color(0xFF0F0F1A), Color(0xFF16213E), Color(0xFF0F0F1A)
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surface
        )
    }

    LaunchedEffect(uiState) {
        if (uiState is BillingUiState.Error) {
            android.widget.Toast.makeText(
                context,
                (uiState as BillingUiState.Error).message,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                Text(
                    stringResource(R.string.subscription_title),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }, navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }, colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
            )
            )
        }, containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(gradientColors))
                .padding(paddingValues)
        ) {
            if (isPremium) {
                // 已订阅状态
                PremiumActiveContent(
                    onRestorePurchases = { viewModel.restorePurchases() })
            } else {
                // 未订阅状态
                SubscriptionContent(
                    availableProducts = availableSubscriptions,
                    backendProducts = backendSubscriptionProducts,
                    selectedProductIndex = selectedProductIndex,
                    onProductSelected = { selectedProductIndex = it },
                    onSubscribe = {
                        if (backendSubscriptionProducts.isNotEmpty() && selectedProductIndex < backendSubscriptionProducts.size) {
                            val product = backendSubscriptionProducts[selectedProductIndex]
                            viewModel.startPurchaseFlow(
                                activity = context as Activity,
                                product = product,
                                availablePlayProducts = availableSubscriptions,
                                onShowChannelDialog = {
                                    selectedProductForChannels = it
                                    showChannelDialog = true
                                })
                        }
                    },
                    onRestorePurchases = { viewModel.restorePurchases() },
                    isLoading = isLoadingProducts && backendSubscriptionProducts.isEmpty()
                )
            }
        }

        // 渠道选择弹窗
        if (showChannelDialog && selectedProductForChannels != null) {
            ChannelSelectionBottomSheet(
                product = selectedProductForChannels!!,
                onDismiss = { showChannelDialog = false },
                onChannelSelected = { channel ->
                    showChannelDialog = false
                    viewModel.purchaseWithChannel(
                        context as Activity, selectedProductForChannels!!, channel
                    )
                })
        }

        // 全局加载遮罩
        if (uiState is BillingUiState.Loading || (isLoadingProducts && backendSubscriptionProducts.isEmpty())) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false) {}, contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFFD700))
            }
        }
    }
}

@Composable
private fun PremiumActiveContent(
    onRestorePurchases: () -> Unit
) {
    val accentColor = Color(0xFFFFD700)
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(accentColor.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ObscuraIcons.Crown,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.subscription_already_premium).uppercase(),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.subscription_thank_you),
            fontSize = 16.sp,
            color = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 管理订阅按钮
        Button(
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data =
                        "https://play.google.com/store/account/subscriptions".toUri()
                    setPackage("com.android.vending")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // 如果没安装 Play Store，尝试通过浏览器打开
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            "https://play.google.com/store/account/subscriptions".toUri()
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isDark) accentColor else MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.subscription_manage_link),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onRestorePurchases) {
            Text(
                text = stringResource(R.string.restore_purchases),
                color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 详细说明
        SubscriptionDescriptionSection()
    }
}

@Composable
private fun SubscriptionContent(
    availableProducts: List<ProductDetails>,
    backendProducts: List<com.obscura.wallpapers.core.data.remote.service.CoinProduct>,
    selectedProductIndex: Int,
    onProductSelected: (Int) -> Unit,
    onSubscribe: () -> Unit,
    onRestorePurchases: () -> Unit,
    isLoading: Boolean
) {
    val accentColor = Color(0xFFFFD700)
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header Icon with Glow
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape)
            )
            Icon(
                imageVector = ObscuraIcons.Crown,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 标题
        Text(
            text = stringResource(R.string.subscription_unlock_all_features).uppercase(),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp,
            lineHeight = 38.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.subscription_enjoy_full_experience),
            fontSize = 15.sp,
            color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 功能列表
        FeaturesList()

        Spacer(modifier = Modifier.height(40.dp))

        // 订阅方案 - 以后端数据为准
        if (backendProducts.isNotEmpty()) {
            backendProducts.forEachIndexed { index, backendItem ->
                // 尝试匹配 Google Play 的实时价格信息
                val playProduct =
                    availableProducts.find { it.productId == backendItem.sku || it.productId == backendItem.id?.toString() }

                PlanCard(
                    productDetails = playProduct,
                    backendInfo = backendItem,
                    isSelected = index == selectedProductIndex,
                    onClick = { onProductSelected(index) })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 订阅按钮
        Button(
            onClick = onSubscribe,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .border(
                    width = 1.dp, brush = Brush.linearGradient(
                        listOf(
                            if (isDark) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                            Color.Transparent
                        )
                    ), shape = RoundedCornerShape(18.dp)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                disabledContainerColor = accentColor.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(18.dp),
            enabled = !isLoading && backendProducts.isNotEmpty(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.subscription_start).uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 恢复购买
        Text(
            text = stringResource(R.string.restore_purchases),
            color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onRestorePurchases)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 说明文字
        Text(
            text = stringResource(R.string.subscription_auto_renew_notice),
            fontSize = 11.sp,
            color = if (isDark) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.6f
            ),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        SubscriptionDescriptionSection()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SubscriptionDescriptionSection() {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.subscription_description_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.subscription_description_content),
            fontSize = 12.sp,
            color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun FeaturesList() {
    val accentColor = Color(0xFFFFD700)
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isDark) Color.Black.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.4f
                )
            )
            .border(
                0.5.dp, Brush.verticalGradient(
                    listOf(
                        if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outline.copy(
                            alpha = 0.1f
                        ), Color.Transparent
                    )
                ), RoundedCornerShape(24.dp)
            )
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        listOf(
            R.string.feature_no_ads,
            R.string.feature_premium_wallpapers,
            R.string.feature_auto_wallpaper,
            R.string.feature_hd_wallpapers,
            R.string.feature_exclusive_content
        ).forEach { featureRes ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(accentColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(featureRes),
                    fontSize = 16.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    productDetails: ProductDetails?,
    backendInfo: com.obscura.wallpapers.core.data.remote.service.CoinProduct,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = Color(0xFFFFD700)
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            .border(
                width = if (isSelected) 2.dp else 0.5.dp, brush = if (isSelected) {
                    Brush.linearGradient(
                        listOf(accentColor, accentColor.copy(alpha = 0.3f))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            if (isDark) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.2f
                            ), if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent
                        )
                    )
                }, shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // 后端配置的额外信息（如赠送金币）
        if (backendInfo.coins > 0) {
            Surface(
                color = accentColor,
                shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 0.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "+${backendInfo.coins / 100}",
                        modifier = Modifier.padding(end = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    Icon(
                        imageVector = ObscuraIcons.Coin,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(accentColor.copy(alpha = 0.1f), Color.Transparent)
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = backendInfo.title ?: productDetails?.name ?: "Premium Plan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 优先显示 Google Play 格式化后的本地价格，否则显示后端配置价格
                    val playPrice =
                        productDetails?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    val displayPrice = playPrice ?: backendInfo.showPrice ?: ""

                    Text(
                        text = displayPrice,
                        fontSize = 16.sp,
                        color = if (isSelected) accentColor else if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (displayPrice.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.subscription_weekly),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }
            }

            RadioButton(
                selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(
                    selectedColor = accentColor,
                    unselectedColor = if (isDark) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(
                        alpha = 0.3f
                    )
                )
            )
        }
    }
}
