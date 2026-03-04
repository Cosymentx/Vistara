package com.obscura.wallpapers.features.subscription

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

/**
 * 订阅页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isPremium by viewModel.isPremium.collectAsState()
    val availableProducts by viewModel.availableProducts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedProductIndex by remember { mutableStateOf(1) } // 默认选中年度订阅

    val gradientColors = listOf(
        Color(0xFF1A1A2E),
        Color(0xFF16213E)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.subscription_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
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
        ) {
            if (isPremium) {
                // 已订阅状态
                PremiumActiveContent(
                    onRestorePurchases = { viewModel.restorePurchases() }
                )
            } else {
                // 未订阅状态
                SubscriptionContent(
                    availableProducts = availableProducts,
                    selectedProductIndex = selectedProductIndex,
                    onProductSelected = { selectedProductIndex = it },
                    onSubscribe = {
                        if (availableProducts.isNotEmpty() && selectedProductIndex < availableProducts.size) {
                            viewModel.purchaseProduct(
                                context as Activity,
                                availableProducts[selectedProductIndex]
                            )
                        }
                    },
                    onRestorePurchases = { viewModel.restorePurchases() },
                    isLoading = uiState is SubscriptionUiState.Loading
                )
            }
        }
    }
}

@Composable
private fun PremiumActiveContent(
    onRestorePurchases: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            fontSize = 64.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.subscription_already_premium),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.subscription_thank_you),
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        TextButton(onClick = onRestorePurchases) {
            Text(
                text = stringResource(R.string.restore_purchases),
                color = Color(0xFFFFD700)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = stringResource(R.string.subscription_unlock_all_features),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.subscription_enjoy_full_experience),
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 功能列表
        FeaturesList()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 订阅方案
        if (availableProducts.isNotEmpty()) {
            Text(
                text = stringResource(R.string.subscription_choose_plan),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            availableProducts.forEachIndexed { index, product ->
                PlanCard(
                    productDetails = product,
                    isSelected = index == selectedProductIndex,
                    onClick = { onProductSelected(index) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 订阅按钮
        Button(
            onClick = onSubscribe,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD700)
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading && availableProducts.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.Black
                )
            } else {
                Text(
                    text = stringResource(R.string.subscription_start),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 恢复购买
        TextButton(onClick = onRestorePurchases) {
            Text(
                text = stringResource(R.string.restore_purchases),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 说明文字
        Text(
            text = stringResource(R.string.subscription_auto_renew_notice),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeaturesList() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(featureRes),
                    fontSize = 16.sp,
                    color = Color.White
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
    val borderColor = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f)
    val backgroundColor = if (isSelected) 
        Color(0xFFFFD700).copy(alpha = 0.1f) 
    else 
        Color.White.copy(alpha = 0.05f)
    
    val price = productDetails.subscriptionOfferDetails?.firstOrNull()
        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: ""
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = productDetails.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = price,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected),
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
