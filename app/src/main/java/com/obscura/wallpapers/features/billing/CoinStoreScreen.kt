package com.obscura.wallpapers.features.billing

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.billingclient.api.ProductDetails
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import coil.compose.AsyncImage

/**
 * Coin Store Screen - Refined with Glassmorphism and Cyan accents
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinStoreScreen(
    onNavigateBack: () -> Unit,
    viewModel: BillingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedPackageIndex by remember { mutableIntStateOf(-1) }
    val isPurchasing by viewModel.isPurchasingCoins.collectAsState()
    val purchaseSuccess by viewModel.coinPurchaseSuccess.collectAsState()
    val availableInAppProducts by viewModel.availableInAppProducts.collectAsState()
    val backendProducts by viewModel.backendProducts.collectAsState()
    val isLoadingProducts by viewModel.isLoadingProducts.collectAsState()
    
    var showChannelDialog by remember { mutableStateOf(false) }
    var selectedProductForChannels by remember { mutableStateOf<com.obscura.wallpapers.core.data.remote.service.CoinProduct?>(null) }

    val isDark = isSystemInDarkTheme()
    val cyanAccent = Color(0xFF00E5FF)
    val backgroundGradient = if (isDark) {
        listOf(
            Color(0xFF000000),
            Color(0xFF00151A),
            Color(0xFF000000)
        )
    } else {
        listOf(
            cyanAccent.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surface
        )
    }

    LaunchedEffect(purchaseSuccess) {
        if (purchaseSuccess) {
            onNavigateBack()
            viewModel.resetCoinPurchaseStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.coin_purchase_title), fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(backgroundGradient))
                .padding(paddingValues)
                .navigationBarsPadding()
        ) {
            // Background Glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(cyanAccent.copy(alpha = 0.05f), Color.Transparent),
                            radius = 1200f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Header Icon with Glow
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(cyanAccent.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                    )
                    Icon(
                        imageVector = ObscuraIcons.Coin,
                        contentDescription = null,
                        tint = cyanAccent,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.coin_get_more_coins),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.coin_description),
                    fontSize = 15.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Coin Packages
                if (isLoadingProducts) {
                    CircularProgressIndicator(color = cyanAccent)
                } else if (backendProducts.isNotEmpty()) {
                    backendProducts.forEachIndexed { index, product ->
                        // 寻找对应的 Google Play 产品详情（如果存在）
                        val productDetails = availableInAppProducts.find { it.productId == product.id }
                        
                        CoinBackendProductCard(
                            product = product,
                            productDetails = productDetails,
                            isSelected = selectedPackageIndex == index,
                            onClick = { 
                                selectedPackageIndex = index
                                // 直接触发购买逻辑
                                if (!product.channels.isNullOrEmpty()) {
                                    if (product.channels.size == 1 && product.channels[0].channel == "1") {
                                        // 只有 Google Play 渠道，直接发起
                                        viewModel.purchaseWithChannel(context as Activity, product, product.channels[0])
                                    } else {
                                        selectedProductForChannels = product
                                        showChannelDialog = true
                                    }
                                } else {
                                    // 备退方案
                                    val fallbackDetails = availableInAppProducts.find { it.productId == product.sku }
                                    if (fallbackDetails != null) {
                                        viewModel.purchaseCoinsWithOrder(context as Activity, product, fallbackDetails)
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    // Fallback to static or GP only
                    availableInAppProducts.forEachIndexed { index, product ->
                        CoinProductCard(
                            productDetails = product,
                            isSelected = selectedPackageIndex == index,
                            onClick = { 
                                selectedPackageIndex = index
                                viewModel.purchaseCoins(context as Activity, product)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.coin_instant_delivery),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // 全局加载遮罩
        if (isPurchasing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = cyanAccent)
            }
        }

        // 渠道选择弹窗
        if (showChannelDialog && selectedProductForChannels != null) {
            ChannelSelectionBottomSheet(
                product = selectedProductForChannels!!,
                onDismiss = { showChannelDialog = false },
                onChannelSelected = { channel ->
                    showChannelDialog = false
                    viewModel.purchaseWithChannel(context as Activity, selectedProductForChannels!!, channel)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSelectionBottomSheet(
    product: com.obscura.wallpapers.core.data.remote.service.CoinProduct,
    onDismiss: () -> Unit,
    onChannelSelected: (com.obscura.wallpapers.core.data.remote.service.PayChannel) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cyanAccent = Color(0xFF00E5FF)
    
    // 高级感配色：深色模式使用深邃黑，浅色模式使用纯净白
    val surfaceColor = MaterialTheme.colorScheme.background
    val onSurfaceColor = MaterialTheme.colorScheme.background
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { 
            BottomSheetDefaults.DragHandle(
                color = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
            ) 
        },
        containerColor = surfaceColor,
        contentColor = onSurfaceColor,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        tonalElevation = 0.dp, // 禁用自带的抬升色，防止颜色变灰
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(surfaceColor, surfaceColor.copy(alpha = 0.95f))
                    )
                )
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
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
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Gray,
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

            product.channels?.forEach { channel ->
                ChannelItem(
                    channel = channel,
                    onClick = { onChannelSelected(channel) }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 底部安全背书
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
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
                        color = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelItem(
    channel: com.obscura.wallpapers.core.data.remote.service.PayChannel,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cyanAccent = Color(0xFF00E5FF)
    val containerColor = if (isDark) Color(0xFF25282B) else Color(0xFFF8F9FA)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent
        ),
        shadowElevation = if (isDark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 图标容器 - 增加微弱投影
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier.padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = if (channel.icon?.startsWith("http") == true) channel.icon else "https://img.obscura.com/${channel.icon}",
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
                        color = if (isDark) Color.White else Color.Black
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val statusText = if (channel.channel == "1") "Recommended" else "Instant Delivery"
                        val statusColor = if (channel.channel == "1") cyanAccent else if (isDark) Color.White.copy(alpha = 0.4f) else Color.Gray
                        
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
                tint = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CoinBackendProductCard(
    product: com.obscura.wallpapers.core.data.remote.service.CoinProduct,
    productDetails: ProductDetails?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cyanAccent = Color(0xFF00E5FF)
    val isDark = isSystemInDarkTheme()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDark) Color.White.copy(alpha = if (isSelected) 0.12f else 0.05f) else MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) {
                    Brush.linearGradient(
                        listOf(cyanAccent, cyanAccent.copy(alpha = 0.5f))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(if (isDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                    )
                },
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon with subtle glow if selected
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(cyanAccent.copy(alpha = 0.2f), CircleShape)
                        )
                    }
                    Icon(
                        imageVector = ObscuraIcons.Coin,
                        contentDescription = null,
                        tint = if (isSelected) cyanAccent else if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${product.coins}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.coin_coins).uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    
                    Text(
                        text = product.showInitialPrice?:"",
                        fontSize = 13.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }

            Text(
                text = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: "${product.showPrice}",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) cyanAccent else if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CoinProductCard(
    productDetails: ProductDetails,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cyanAccent = Color(0xFF00E5FF)
    val isDark = isSystemInDarkTheme()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDark) Color.White.copy(alpha = if (isSelected) 0.12f else 0.05f) else MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) {
                    Brush.linearGradient(
                        listOf(cyanAccent, cyanAccent.copy(alpha = 0.5f))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(if (isDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                    )
                },
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon with subtle glow if selected
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(cyanAccent.copy(alpha = 0.2f), CircleShape)
                        )
                    }
                    Icon(
                        imageVector = ObscuraIcons.Coin,
                        contentDescription = null,
                        tint = if (isSelected) cyanAccent else if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column {
                    Text(
                        text = productDetails.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = productDetails.description,
                        fontSize = 12.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: "",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) cyanAccent else if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
