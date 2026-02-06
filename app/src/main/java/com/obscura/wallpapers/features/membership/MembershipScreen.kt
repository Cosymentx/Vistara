package com.obscura.wallpapers.features.membership

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.components.ErrorState
import com.obscura.wallpapers.ui.components.GlassTopAppBar
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.components.PaymentMethodDialog
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipScreen(
    onBackPressed: () -> Unit,
    onUpgradeSuccess: () -> Unit = {},
    viewModel: MembershipViewModel = hiltViewModel(),
    navController: NavController = rememberNavController()
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
    val canPayment by viewModel.canPayment.collectAsState()
    val isUpgrading by viewModel.isUpgrading.collectAsState()
    val upgradeResult by viewModel.upgradeResult.collectAsState()
    val selectedPlan by viewModel.selectedPlan.collectAsState()
    val billingConnectionState by viewModel.billingConnectionState.collectAsState()
    val productPrices by viewModel.productPrices.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val showPaymentDialog by viewModel.showPaymentDialog.collectAsState()
    val paymentUrl by viewModel.paymentUrl.collectAsState()
    val orderCreationState by viewModel.orderCreationState.collectAsState()
    val subscriptionProducts by viewModel.subscriptionProducts.collectAsState()
    val apiProductsLoading by viewModel.apiProductsLoading.collectAsState()
    val apiProductsError by viewModel.apiProductsError.collectAsState()

    viewModel.setNavController(navController)

    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(upgradeResult) {
        upgradeResult?.let {
            when (it) {
                is UpgradeResult.Success -> {
                    snackBarHostState.showSnackbar(it.message)
                    viewModel.clearUpgradeResult()
                    onUpgradeSuccess()
                }

                is UpgradeResult.Error -> {
                    snackBarHostState.showSnackbar(it.message)
                    viewModel.clearUpgradeResult()
                }
            }
        }
    }

    LaunchedEffect(paymentUrl) {
        paymentUrl?.let { url ->
            try {
                val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                val route = "webview?url=$encodedUrl"
                navController.navigate(route) { launchSingleTop = true }
            } catch (_: Exception) {
            }
            viewModel.clearPaymentUrl()
        }
    }

    if (showPaymentDialog && selectedPlan != null) {
        PaymentMethodDialog(
            amount = when (selectedPlan) {
                PremiumPlan.WEEKLY -> stringResource(R.string.subscription_title_week)
                PremiumPlan.MONTHLY -> stringResource(R.string.subscription_title_month)
                PremiumPlan.QUARTERLY -> stringResource(R.string.subscription_title_quarter)
                PremiumPlan.YEARLY -> stringResource(R.string.weekly_plan)
                PremiumPlan.LIFETIME -> stringResource(R.string.monthly_plan)
                null -> stringResource(R.string.subscription_title_month)
            },
            paymentMethods = paymentMethods,
            isLoading = viewModel.paymentMethodsLoading.collectAsState().value,
            onDismiss = viewModel::hidePaymentDialog,
            onPaymentSelected = viewModel::handlePaymentMethodSelected
        )
    }

    val (contentModifier, topBar) = GlassTopAppBar(
        title = stringResource(R.string.premium), onBackPressed = onBackPressed
    )
    Scaffold(
        topBar = { topBar() },
        snackbarHost = { SnackbarHost(snackBarHostState) }) { paddingValues ->
        Box(modifier = contentModifier) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                if (apiProductsLoading) {
                    LoadingState()
                } else if (apiProductsError != null) {
                    ErrorState(
                        message = stringResource(
                            R.string.error_loading_subscription_products, apiProductsError ?: ""
                        ), onRetry = { })
                } else if (subscriptionProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_subscription_products),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        subscriptionProducts.forEachIndexed { index, product ->
                            val googlePriceFromProductId =
                                product.productId?.let { productPrices[it] }
                            val apiPrice = "${product.currency} ${product.price}"
                            val displayPrice = googlePriceFromProductId ?: apiPrice
                            val plan = when {
                                product.productId?.contains(
                                    "vistara_sub_week", ignoreCase = true
                                ) == true -> PremiumPlan.WEEKLY

                                product.productId?.contains(
                                    "vistara_sub_month", ignoreCase = true
                                ) == true -> PremiumPlan.MONTHLY

                                product.productId?.contains(
                                    "vistara_sub_quarter", ignoreCase = true
                                ) == true -> PremiumPlan.QUARTERLY

                                else -> null
                            }

                            if (index > 0) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            SubscriptionCard(
                                title = when (plan) {
                                    PremiumPlan.WEEKLY -> stringResource(R.string.subscription_title_week)
                                    PremiumPlan.MONTHLY -> stringResource(R.string.subscription_title_month)
                                    PremiumPlan.QUARTERLY -> stringResource(R.string.subscription_title_quarter)
                                    else -> product.itemName
                                },
                                price = displayPrice,
                                originalPrice = displayPrice,
                                isSelected = plan != null && plan == selectedPlan,
                                modifier = Modifier.weight(1f),
                                showBonus = false,
                                showDiscount = false,
                                onClick = { plan?.let { viewModel.selectPlan(it) } })
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF3A1B59), Color(0xFFC125E3), Color(0xFF3A1B59)
                                    )
                                )
                            )
                            .padding(vertical = 14.dp), contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .offset(y = (-26).dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF3A1B59).copy(alpha = 0.2f),
                                            Color(0xFFF971F8),
                                            Color(0xFF3A1B59).copy(alpha = 0.2f)
                                        )
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .offset(y = 35.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF3A1B59).copy(alpha = 0.2f),
                                            Color(0xFFF971F8),
                                            Color(0xFF3A1B59).copy(alpha = 0.2f)
                                        )
                                    )
                                )
                        )

                        Text(
                            text = stringResource(R.string.exclusive_privileges),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = com.obscura.wallpapers.ui.theme.AppColors.DarkPremiumFeaturesBackground
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        PremiumFeaturesList()
                    }
                }

                Text(
                    text = stringResource(R.string.payment_terms),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF524C5F),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.cancel_subscription_prefix) + " ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF524C5F)
                    )
                    Text(
                        text = stringResource(R.string.cancel_subscription_link),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF71FE3),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            viewModel.openSubscriptionManagementPage(
                                activity
                            )
                        })
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.more_info_prefix),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF524C5F)
                        )
                        Text(
                            text = stringResource(R.string.terms_of_use),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9F2BEE),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { viewModel.openTermsOfService() })
                        Text(
                            text = " ${stringResource(R.string.and)} ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF524C5F)
                        )
                        Text(
                            text = stringResource(R.string.privacy_policy),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9F2BEE),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { viewModel.openPrivacyPolicy() })
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    title: String,
    price: String,
    originalPrice: String? = null,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    showBonus: Boolean = false,
    showDiscount: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = price,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            if (!originalPrice.isNullOrEmpty() && originalPrice != price) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = originalPrice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showBonus || showDiscount) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showBonus) {
                        AssistChip(
                            onClick = {},
                            label = { Text(text = stringResource(R.string.bonus_diamonds, 0)) })
                    }
                    if (showDiscount) {
                        AssistChip(onClick = {}, label = {
                            Text(
                                text = stringResource(
                                    R.string.discount_percent_off, 0
                                )
                            )
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumFeaturesList(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        FeatureRow(
            title = stringResource(R.string.premium_feature_1),
            desc = stringResource(R.string.premium_feature_1_desc)
        )
        FeatureRow(
            title = stringResource(R.string.premium_feature_2),
            desc = stringResource(R.string.premium_feature_2_desc)
        )
        FeatureRow(
            title = stringResource(R.string.premium_feature_3),
            desc = stringResource(R.string.premium_feature_3_desc)
        )
        FeatureRow(
            title = stringResource(R.string.premium_feature_4),
            desc = stringResource(R.string.premium_feature_4_desc)
        )
        FeatureRow(
            title = stringResource(R.string.premium_feature_5),
            desc = stringResource(R.string.premium_feature_5_desc)
        )
    }
}

@Composable
private fun FeatureRow(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
