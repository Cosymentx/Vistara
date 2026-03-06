package com.obscura.wallpapers.features.billing

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
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
    var selectedPackageIndex by remember { mutableIntStateOf(1) }
    val isPurchasing by viewModel.isPurchasingCoins.collectAsState()
    val purchaseSuccess by viewModel.coinPurchaseSuccess.collectAsState()

    val cyanAccent = Color(0xFF00E5FF)
    val backgroundGradient = listOf(
        Color(0xFF000000),
        Color(0xFF00151A),
        Color(0xFF000000)
    )

    val coinPackages = remember {
        listOf(
            CoinPackage(amount = 100, price = "$0.99", bonus = 0),
            CoinPackage(amount = 500, price = "$4.99", bonus = 50),
            CoinPackage(amount = 1200, price = "$9.99", bonus = 200),
            CoinPackage(amount = 3000, price = "$19.99", bonus = 600),
            CoinPackage(amount = 6500, price = "$49.99", bonus = 1500)
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
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(backgroundGradient))
                .padding(paddingValues)
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
                        imageVector = ObscuraIcons.Diamond,
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
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.coin_description),
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Coin Packages
                coinPackages.forEachIndexed { index, pkg ->
                    CoinPackageCard(
                        coinPackage = pkg,
                        isSelected = selectedPackageIndex == index,
                        onClick = { selectedPackageIndex = index }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Purchase Button
                Button(
                    onClick = {
                        viewModel.purchaseCoins(coinPackages[selectedPackageIndex])
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cyanAccent,
                        contentColor = Color.Black,
                        disabledContainerColor = cyanAccent.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isPurchasing,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.coin_buy_now),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
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
    }
}

@Composable
private fun CoinPackageCard(
    coinPackage: CoinPackage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cyanAccent = Color(0xFF00E5FF)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = if (isSelected) 0.12f else 0.05f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) {
                    Brush.linearGradient(
                        listOf(cyanAccent, cyanAccent.copy(alpha = 0.5f))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f))
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
                        imageVector = ObscuraIcons.Diamond,
                        contentDescription = null,
                        tint = if (isSelected) cyanAccent else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${coinPackage.amount}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.coin_coins).uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    
                    if (coinPackage.bonus > 0) {
                        Text(
                            text = stringResource(R.string.coin_bonus, coinPackage.bonus),
                            fontSize = 13.sp,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = coinPackage.price,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) cyanAccent else Color.White
            )
        }

        // Popular Badge
        if (coinPackage.amount == 1200) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(topEnd = 24.dp, bottomStart = 16.dp)),
                color = cyanAccent
            ) {
                Text(
                    text = stringResource(R.string.coin_popular).uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
