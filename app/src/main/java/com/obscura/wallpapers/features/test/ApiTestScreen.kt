package com.obscura.wallpapers.features.test

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.billingclient.api.ProductDetails
import com.obscura.wallpapers.ui.components.GlassTopAppBar
import com.obscura.wallpapers.ui.components.LocalHazeState
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import com.obscura.wallpapers.ui.theme.stringResource
import com.obscura.wallpapers.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTestScreen(
    onBackPressed: () -> Unit,
    viewModel: ApiTestViewModel = hiltViewModel()
) {
    val testResults by viewModel.testResults.collectAsState()
    val isTestingPexels by viewModel.isTestingPexels.collectAsState()
    val isTestingUnsplash by viewModel.isTestingUnsplash.collectAsState()
    val resultMessage by viewModel.resultMessage.collectAsState()
    
    val isPremium by viewModel.isPremium.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()
    val availableSubscriptions by viewModel.availableSubscriptions.collectAsState()
    val availableCoins by viewModel.availableCoins.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalActivity.current as Activity

    LaunchedEffect(resultMessage) {
        resultMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearResultMessage()
        }
    }

    val (contentModifier, topBar) = GlassTopAppBar(
        title = "Debug & Test Center",
        onBackPressed = onBackPressed
    )

    val hazeState = LocalHazeState.current ?: remember { HazeState() }

    Scaffold(
        topBar = { topBar() },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = contentModifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 状态面板 (高级感渐变) ---
            item {
                StatusPanel(isPremium, coinBalance)
            }

            // --- 计费模拟控制区 ---
            item {
                TestSectionHeader(title = "Billing Simulator", icon = Icons.Default.Refresh)
            }

            item {
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SimulatorButton(
                                text = if (isPremium) "Revoke Premium" else "Grant Premium",
                                icon = if (isPremium) Icons.Default.Close else Icons.Default.CheckCircle,
                                onClick = { viewModel.toggleMockPremium() },
                                modifier = Modifier.weight(1f),
                                contentColor = if (isPremium) Color.Red.copy(alpha = 0.8f) else Color.Green.copy(alpha = 0.8f)
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SimulatorButton(
                                text = "Refill +100",
                                icon = Icons.Default.Add,
                                onClick = { viewModel.addMockCoins(100) },
                                modifier = Modifier.weight(1f)
                            )
                            SimulatorButton(
                                text = "Spend -50",
                                icon = Icons.Default.KeyboardArrowDown,
                                onClick = { viewModel.consumeMockCoins(50) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // --- 真实 Play 商店商品 ---
            if (availableSubscriptions.isNotEmpty() || availableCoins.isNotEmpty()) {
                item {
                    TestSectionHeader(title = "Google Play Store Items", icon = Icons.Default.ShoppingCart)
                }

                items(availableSubscriptions) { product ->
                    RealProductCard(product, isSub = true) { viewModel.testRealPurchase(context, product) }
                }

                items(availableCoins) { product ->
                    RealProductCard(product, isSub = false) { viewModel.testRealPurchase(context, product) }
                }
            }

            // --- API 测试区 ---
            item {
                TestSectionHeader(title = "API Connectivity", icon = Icons.Default.ThumbUp)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ApiTestCard(
                        name = "Pexels",
                        isLoading = isTestingPexels,
                        onClick = { viewModel.testPexelsApi() },
                        modifier = Modifier.weight(1f)
                    )
                    ApiTestCard(
                        name = "Unsplash",
                        isLoading = isTestingUnsplash,
                        onClick = { viewModel.testUnsplashApi() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- 实时日志输出 ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TestSectionHeader(title = "Live Logs", icon = Icons.Default.Info)
                    if (testResults.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearTestResults() }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            if (testResults.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No logs yet. Run a test to see output.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(testResults.reversed()) { result ->
                    LogCard(result)
                }
            }
        }
    }
}

@Composable
fun StatusPanel(isPremium: Boolean, coinBalance: Int) {
    val hazeState = LocalHazeState.current ?: remember { HazeState() }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    tint = HazeTint(if (isPremium) Color(0xFFD4AF37).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f)),
                    blurRadius = 30.dp,
                    noiseFactor = 0.15f
                )
            )
            .background(
                if (isPremium) {
                    Brush.linearGradient(listOf(Color(0xFFD4AF37).copy(alpha = 0.1f), Color(0xFF9A7B1D).copy(alpha = 0.05f)))
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.05f)))
                }
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.premium_user_status),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPremium) ObscuraIcons.Crown else ObscuraIcons.Person,
                        contentDescription = null,
                        tint = if (isPremium) Color(0xFFD4AF37) else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isPremium) "PREMIUM" else "STANDARD",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "V-COINS",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$coinBalance",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = ObscuraIcons.Coin,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val hazeState = LocalHazeState.current ?: remember { HazeState() }
    
    Box(
        modifier = modifier
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    tint = HazeTint(Color.White.copy(alpha = 0.12f)),
                    blurRadius = 30.dp,
                    noiseFactor = 0.15f
                )
            )
            .background(Color.White.copy(alpha = 0.05f), shape = shape)
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = shape
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun TestSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun SimulatorButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent, // 不再需要实心色
    contentColor: Color = Color.White
) {
    val hazeState = LocalHazeState.current ?: remember { HazeState() }

    Box(
        modifier = modifier
            .height(52.dp)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    tint = HazeTint(Color.White.copy(alpha = 0.15f)),
                    blurRadius = 30.dp,
                    noiseFactor = 0.15f
                )
            )
            .background(Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp))
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = contentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
        }
    }
}

@Composable
fun RealProductCard(product: ProductDetails, isSub: Boolean, onClick: () -> Unit) {
    val hazeState = LocalHazeState.current ?: remember { HazeState() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    tint = HazeTint(Color.White.copy(alpha = 0.12f)),
                    blurRadius = 30.dp,
                    noiseFactor = 0.15f
                )
            )
            .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(20.dp))
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isSub) Color(0xFFD4AF37).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSub) ObscuraIcons.Crown else ObscuraIcons.Coin,
                    contentDescription = null,
                    tint = if (isSub) Color(0xFFD4AF37) else Color(0xFFFFD700)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(product.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.White.copy(alpha = 0.5f))
            }
            Text(
                text = product.oneTimePurchaseOfferDetails?.formattedPrice ?: "Detail",
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun ApiTestCard(name: String, isLoading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hazeState = LocalHazeState.current ?: remember { HazeState() }

    Box(
        modifier = modifier
            .height(80.dp)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    tint = HazeTint(Color.White.copy(alpha = 0.12f)),
                    blurRadius = 30.dp,
                    noiseFactor = 0.15f
                )
            )
            .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(20.dp))
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = Color.White)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Build, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun LogCard(result: String) {
    val isError = result.contains("❌")
    val isSuccess = result.contains("✅")
    val hazeState = LocalHazeState.current ?: remember { HazeState() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    tint = HazeTint(
                        when {
                            isError -> Color.Red.copy(alpha = 0.1f)
                            isSuccess -> Color.Green.copy(alpha = 0.1f)
                            else -> Color.White.copy(alpha = 0.12f)
                        }
                    ),
                    blurRadius = 30.dp,
                    noiseFactor = 0.15f
                )
            )
            .background(
                color = when {
                    isError -> Color.Red.copy(alpha = 0.05f)
                    isSuccess -> Color.Green.copy(alpha = 0.05f)
                    else -> Color.White.copy(alpha = 0.05f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                0.5.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Text(
            text = result,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            lineHeight = 18.sp,
            color = Color.White
        )
    }
}
