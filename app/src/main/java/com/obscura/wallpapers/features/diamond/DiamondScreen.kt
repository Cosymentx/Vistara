package com.obscura.wallpapers.features.diamond

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.components.DiamondBalanceCard
import com.obscura.wallpapers.ui.components.PaymentMethodDialog
import com.obscura.wallpapers.ui.components.RechargeContent
import com.obscura.wallpapers.ui.components.TransactionHistoryContent
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiamondScreen(
    onBackPressed: () -> Unit,
    navController: NavController? = null,
    viewModel: DiamondViewModel = hiltViewModel()
) {
    val diamondBalance by viewModel.diamondBalance.collectAsState()
    val diamondProducts by viewModel.diamondProducts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val transactionsLoading by viewModel.transactionsLoading.collectAsState()
    val transactionsError by viewModel.transactionsError.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val productPrices by viewModel.productPrices.collectAsState()
    val apiProductsLoading by viewModel.apiProductsLoading.collectAsState()
    val apiProductsError by viewModel.apiProductsError.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val showPaymentDialog by viewModel.showPaymentDialog.collectAsState()
    val paymentUrl by viewModel.paymentUrl.collectAsState()

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
                    Log.e("DiamondScreen", "NavController is null, cannot navigate to WebView")
                } else {
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            } catch (e: Exception) {
                Log.e("DiamondScreen", "Error navigating to WebView: ${e.message}", e)
            }
            viewModel.resetPaymentUrl()
        }
    }

    if (showPaymentDialog && selectedProduct != null) {
        PaymentMethodDialog(
            amount = "${selectedProduct?.diamondAmount}",
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
                            imageVector = ObscuraIcons.History,
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

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=1080px,height=2340px,dpi=440"
)
@Composable
fun RechargeScreenPreview() {
    ObscuraTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            DiamondBalanceCard(100)
        }
    }
}
