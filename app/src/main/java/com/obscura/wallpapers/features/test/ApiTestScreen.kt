package com.obscura.wallpapers.features.test

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
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

    Scaffold(
        topBar = { topBar() },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = contentModifier
                .fillMaxSize()
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SimulatorButton(
                                text = if (isPremium) "Revoke Premium" else "Grant Premium",
                                icon = Icons.Default.CheckCircle,
                                onClick = { viewModel.toggleMockPremium() },
                                modifier = Modifier.weight(1f),
                                containerColor = if (isPremium) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (isPremium) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
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
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
    val brush = Brush.linearGradient(
        colors = if (isPremium) {
            listOf(Color(0xFFD4AF37), Color(0xFF9A7B1D))
        } else {
            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
        }
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.background(brush).padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Account Tier", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPremium) Icons.Default.Star else Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color.White,
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
                    Text("Coin Balance", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                    Text(
                        text = "$coinBalance",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TestSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SimulatorButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 2.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RealProductCard(product: ProductDetails, isSub: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSub) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSub) Icons.Default.Refresh else Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = if (isSub) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(product.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.outline)
            }
            Text(
                text = product.oneTimePurchaseOfferDetails?.formattedPrice ?: "Detail",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun ApiTestCard(name: String, isLoading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun LogCard(result: String) {
    val isError = result.contains("❌")
    val isSuccess = result.contains("✅")
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = when {
            isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            isSuccess -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            when {
                isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                isSuccess -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
        )
    ) {
        Text(
            text = result,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            lineHeight = 18.sp
        )
    }
}
