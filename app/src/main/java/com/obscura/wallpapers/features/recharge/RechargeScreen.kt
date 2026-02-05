package com.obscura.wallpapers.features.recharge

import android.app.Activity
import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.DiamondProduct
import com.obscura.wallpapers.ui.components.PaymentMethodDialog
import com.obscura.wallpapers.ui.icons.AppIcons
import com.obscura.wallpapers.ui.theme.VistaraTheme
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechargeScreen(
    onBackPressed: () -> Unit,
    navController: NavController? = null,
    viewModel: RechargeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val diamondBalance by viewModel.diamondBalance.collectAsState()
    val diamondProducts by viewModel.diamondProducts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val transactionsLoading by viewModel.transactionsLoading.collectAsState()
    val transactionsError by viewModel.transactionsError.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val billingConnectionState by viewModel.billingConnectionState.collectAsState()
    val purchaseState by viewModel.purchaseState.collectAsState()
    val productPrices by viewModel.productPrices.collectAsState()
    val apiProductsLoading by viewModel.apiProductsLoading.collectAsState()
    val apiProductsError by viewModel.apiProductsError.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val showPaymentDialog by viewModel.showPaymentDialog.collectAsState()
    val paymentUrl by viewModel.paymentUrl.collectAsState()
    val orderCreationState by viewModel.orderCreationState.collectAsState()

    var showTransactions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.bindBilling()
    }

    LaunchedEffect(paymentUrl) {
        paymentUrl?.let { url ->
            try {
                val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                val route = "webview?url=$encodedUrl"
                if (navController == null) {
                    Log.e("RechargeScreen", "NavController is null, cannot navigate to WebView")
                } else {
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            } catch (e: Exception) {
                Log.e("RechargeScreen", "Error navigating to WebView: ${e.message}", e)
            }
            viewModel.resetPaymentUrl()
        }
    }

    if (showPaymentDialog && selectedProduct != null) {
        PaymentMethodDialog(
            amount =  "${selectedProduct?.diamondAmount}",
            paymentMethods = paymentMethods,
            isLoading = viewModel.paymentMethodsLoading.collectAsState().value,
            onDismiss = viewModel::dismissPaymentDialog,
            onPaymentSelected = viewModel::applyPaymentMethod
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diamond_recharge)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showTransactions = !showTransactions
                        if (showTransactions) {
                            viewModel.refreshTransactions()
                        }
                    }) {
                        Icon(
                            imageVector = AppIcons.History,
                            contentDescription = stringResource(R.string.transaction_history)
                        )
                    }
                })
        }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = !showTransactions,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                RechargeContent(
                    diamondBalance = diamondBalance,
                    diamondProducts = diamondProducts,
                    selectedProduct = selectedProduct,
                    productPrices = productPrices,
                    isLoading = apiProductsLoading,
                    errorMessage = apiProductsError,
                    onProductSelected = viewModel::chooseProduct,
                    onPurchase = { viewModel.presentPaymentDialog() })
            }

            AnimatedVisibility(
                visible = showTransactions,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                TransactionHistoryContent(
                    transactions = transactions,
                    diamondBalance = diamondBalance,
                    isLoading = transactionsLoading,
                    errorMessage = transactionsError
                )
            }
        }
    }
}

@Composable
fun RechargeContent(
    diamondBalance: Int,
    diamondProducts: List<DiamondProduct>,
    selectedProduct: DiamondProduct?,
    productPrices: Map<String, String>,
    isLoading: Boolean,
    errorMessage: String?,
    onProductSelected: (DiamondProduct) -> Unit,
    onPurchase: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DiamondBalanceCard(diamondBalance)
        }

        item {
            Text(
                text = stringResource(R.string.select_diamond_package),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.loading_products),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else if (errorMessage != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.error_loading_products, errorMessage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (!isLoading && errorMessage == null) {
            itemsIndexed(diamondProducts) { index, product ->
                val googlePriceFromProductId = product.productId?.let { productPrices[it] }
                val apiPrice = "${product.currency} ${product.price}"
                val displayPrice = googlePriceFromProductId ?: apiPrice

                DiamondProductCard(
                    index = index,
                    product = product,
                    isSelected = product == selectedProduct,
                    price = displayPrice,
                    onClick = {
                        onProductSelected(product)
                        onPurchase()
                    })
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=1080px,height=2340px,dpi=440"
)
@Composable
fun RechargeScreenPreview() {
    VistaraTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            DiamondBalanceCard(100)
        }
    }
}
