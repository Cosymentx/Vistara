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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    val uiState by viewModel.subscriptionUiState.collectAsState()

    var showChannelDialog by remember { mutableStateOf(false) }
    var selectedProductForChannels by remember {
        mutableStateOf<com.obscura.wallpapers.core.data.remote.service.CoinProduct?>(
            null
        )
    }

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

    LaunchedEffect(uiState) {
        if (uiState is BillingUiState.Error) {
            android.widget.Toast.makeText(context, (uiState as BillingUiState.Error).message, android.widget.Toast.LENGTH_SHORT).show()
        }
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
                title = {
                    Text(
                        stringResource(R.string.coin_purchase_title),
                        fontWeight = FontWeight.ExtraBold
                    )
                },
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
                if (backendProducts.isNotEmpty()) {
                    backendProducts.forEachIndexed { index, product ->
                        // 寻找对应的 Google Play 产品详情（如果存在）
                        val productDetails = availableInAppProducts.find {
                            it.productId == product.sku || it.productId == product.id?.toString()
                        }

                        CoinBackendProductCard(
                            product = product,
                            productDetails = productDetails,
                            isSelected = selectedPackageIndex == index,
                            onClick = {
                                selectedPackageIndex = index
                                viewModel.startPurchaseFlow(
                                    activity = context as Activity,
                                    product = product,
                                    availablePlayProducts = availableInAppProducts,
                                    onShowChannelDialog = {
                                        selectedProductForChannels = it
                                        showChannelDialog = true
                                    }
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else if (!isLoadingProducts) {
                    // 仅在非加载状态且后端无数据时，回退到 GP 本地列表
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
        if (isPurchasing || (isLoadingProducts && backendProducts.isEmpty())) {
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
                    viewModel.purchaseWithChannel(
                        context as Activity,
                        selectedProductForChannels!!,
                        channel
                    )
                }
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
                        listOf(
                            if (isDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.1f
                            ), if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent
                        )
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
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.6f
                            ),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Text(
                        text = product.showInitialPrice ?: "",
                        fontSize = 13.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.7f
                        ),
                    )
                }
            }

            Text(
                text = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                    ?: "${product.showPrice}",
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
                        listOf(
                            if (isDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.1f
                            ), if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent
                        )
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
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.7f
                        )
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
