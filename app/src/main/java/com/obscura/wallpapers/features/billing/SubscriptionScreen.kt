package com.obscura.wallpapers.features.billing

import android.app.Activity
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
import androidx.compose.material.icons.filled.Check
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

/**
 * 订阅页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit,
    viewModel: BillingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isPremium by viewModel.isPremium.collectAsState()
    val availableSubscriptions by viewModel.availableSubscriptions.collectAsState()
    val uiState by viewModel.subscriptionUiState.collectAsState()
    
    var selectedProductIndex by remember { mutableStateOf(1) } // 默认选中年度订阅

    val isDark = isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(
            Color(0xFF0F0F1A),
            Color(0xFF16213E),
            Color(0xFF0F0F1A)
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surface
        )
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
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(gradientColors))
                .padding(paddingValues)
                .navigationBarsPadding()
        ) {
            if (isPremium) {
                // 已订阅状态
                PremiumActiveContent(
                    onRestorePurchases = { viewModel.restorePurchases() }
                )
            } else {
                // 未订阅状态
                SubscriptionContent(
                    availableProducts = availableSubscriptions,
                    selectedProductIndex = selectedProductIndex,
                    onProductSelected = { selectedProductIndex = it },
                    onSubscribe = {
                        if (availableSubscriptions.isNotEmpty() && selectedProductIndex < availableSubscriptions.size) {
                            viewModel.purchaseSubscription(
                                context as Activity,
                                availableSubscriptions[selectedProductIndex]
                            )
                        }
                    },
                    onRestorePurchases = { viewModel.restorePurchases() },
                    isLoading = uiState is BillingUiState.Loading
                )
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
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant)
                .border(0.5.dp, if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .clickable(onClick = onRestorePurchases)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.restore_purchases),
                color = if (isDark) accentColor else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SubscriptionContent(
    availableProducts: List<ProductDetails>,
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
        
        // 订阅方案
        if (availableProducts.isNotEmpty()) {
            availableProducts.forEachIndexed { index, product ->
                PlanCard(
                    productDetails = product,
                    isSelected = index == selectedProductIndex,
                    onClick = { onProductSelected(index) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // Placeholder for empty products
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Loading plans...", color = if (isDark) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant)
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
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(if (isDark) Color.White.copy(alpha = 0.5f) else Color.Transparent, Color.Transparent)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                disabledContainerColor = accentColor.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(18.dp),
            enabled = !isLoading && availableProducts.isNotEmpty(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.subscription_start).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    letterSpacing = 1.5.sp
                )
            }
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
            color = if (isDark) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
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
            .background(if (isDark) Color.Black.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                0.5.dp, 
                Brush.verticalGradient(listOf(if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), Color.Transparent)),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        listOf(
            R.string.feature_wallpaper_edit,
            R.string.feature_video_wallpaper,
            R.string.feature_premium_wallpapers,
            R.string.feature_no_ads,
            R.string.feature_cloud_sync
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
    productDetails: ProductDetails,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = Color(0xFFFFD700)
    val isDark = isSystemInDarkTheme()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color.Black.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                brush = if (isSelected) {
                    Brush.linearGradient(
                        listOf(accentColor, accentColor.copy(alpha = 0.3f))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(if (isDark) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                    )
                },
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
    ) {
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
                    text = productDetails.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val price = productDetails.subscriptionOfferDetails?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: ""
                    Text(
                        text = price,
                        fontSize = 16.sp,
                        color = if (isSelected) accentColor else if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = accentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
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
            
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accentColor,
                    unselectedColor = if (isDark) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        }
    }
}
