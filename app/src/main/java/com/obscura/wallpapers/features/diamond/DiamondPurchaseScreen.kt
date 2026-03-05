package com.obscura.wallpapers.features.diamond

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
 * 金币购买页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiamondPurchaseScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiamondPurchaseViewModel = hiltViewModel()
) {
    var selectedPackageIndex by remember { mutableStateOf(1) } // 默认选中中等套餐
    val isPurchasing by viewModel.isPurchasing.collectAsState()
    val purchaseSuccess by viewModel.purchaseSuccess.collectAsState()

    val gradientColors = listOf(
        Color(0xFF0F2027),
        Color(0xFF203A43),
        Color(0xFF2C5364)
    )

    val diamondPackages = remember {
        listOf(
            DiamondPackage(amount = 100, price = "$0.99", bonus = 0),
            DiamondPackage(amount = 500, price = "$4.99", bonus = 50),
            DiamondPackage(amount = 1200, price = "$9.99", bonus = 200),
            DiamondPackage(amount = 3000, price = "$19.99", bonus = 600),
            DiamondPackage(amount = 6500, price = "$49.99", bonus = 1500)
        )
    }

    LaunchedEffect(purchaseSuccess) {
        if (purchaseSuccess) {
            onNavigateBack()
            viewModel.resetPurchaseStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diamond_purchase_title)) },
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
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Header Icon
                Icon(
                    imageVector = ObscuraIcons.Diamond,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.diamond_get_more_coins),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.diamond_description),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Diamond Packages
                diamondPackages.forEachIndexed { index, pkg ->
                    DiamondPackageCard(
                        diamondPackage = pkg,
                        isSelected = selectedPackageIndex == index,
                        onClick = { selectedPackageIndex = index }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Purchase Button
                Button(
                    onClick = {
                        viewModel.purchasePackage(diamondPackages[selectedPackageIndex])
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isPurchasing
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.diamond_buy_now),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.diamond_instant_delivery),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DiamondPackageCard(
    diamondPackage: DiamondPackage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.2f)
    val backgroundColor = if (isSelected) 
        Color(0xFF00E5FF).copy(alpha = 0.15f) 
    else 
        Color.White.copy(alpha = 0.05f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = ObscuraIcons.Diamond,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(32.dp)
                )
                
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${diamondPackage.amount}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.diamond_coins),
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    if (diamondPackage.bonus > 0) {
                        Text(
                            text = stringResource(R.string.diamond_bonus, diamondPackage.bonus),
                            fontSize = 12.sp,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = diamondPackage.price,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF00E5FF) else Color.White
            )
        }

        if (diamondPackage.bonus > 0) {
            Surface(
                color = Color(0xFFFFD700),
                shape = RoundedCornerShape(topEnd = 20.dp, bottomStart = 12.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = stringResource(R.string.diamond_popular),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

data class DiamondPackage(
    val amount: Int,
    val price: String,
    val bonus: Int
)
