package com.obscura.wallpapers.features.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.components.GlassTopAppBar
import com.obscura.wallpapers.ui.components.TextInputDialog
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    onBackPressed: () -> Unit,
    onNavigateToApiTest: () -> Unit,
    viewModel: TestViewModel = hiltViewModel()
) {
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isCoinTestEnabled by viewModel.isCoinTestEnabled.collectAsState()
    val currentCoinBalance by viewModel.currentCoinBalance.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    val isLoginLoading by viewModel.isLoginLoading.collectAsState()
    val showLoginDialog by viewModel.showLoginDialog.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operationResult) {
        operationResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearOperationResult()
        }
    }

    val (contentModifier, topBar) = GlassTopAppBar(
        title = stringResource(R.string.test_title),
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Summary Status Panel ---
            item {
                TestStatusPanel(isLoggedIn, isPremiumUser, currentCoinBalance)
            }

            // --- Section: Account Simulation ---
            item {
                TestSectionCard(title = "Account Simulation", icon = ObscuraIcons.Person) {
                    // Login Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Login Status", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                if (isLoggedIn) "Authenticated" else "Guest Mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = isLoggedIn,
                            onCheckedChange = { isChecked ->
                                if (isChecked) viewModel.simulateLogin() else viewModel.simulateLogout()
                            }
                        )
                    }

                    // Premium Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Premium Access", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                if (isPremiumUser) "VIP Active" else "Regular User",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = isPremiumUser,
                            onCheckedChange = { isChecked ->
                                if (isChecked) viewModel.enablePremiumUser() else viewModel.disablePremiumUser()
                            }
                        )
                    }
                }
            }

            // --- Section: Economy System ---
            item {
                TestSectionCard(title = "Economy System", icon = ObscuraIcons.Diamond) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Coin Test Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                if (isCoinTestEnabled) "Mocking balance enabled" else "Using real balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = isCoinTestEnabled,
                            onCheckedChange = { viewModel.toggleCoinTest() }
                        )
                    }
                }
            }

            // --- Section: UI & API Tools ---
            item {
                TestSectionCard(title = "External Tools & Dialogs", icon = ObscuraIcons.Build) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onNavigateToApiTest,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Launch Pexels API Tester")
                        }

                        OutlinedButton(
                            onClick = { viewModel.showLoginDialog() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Preview Server Login Dialog")
                        }

                        var showPaymentDialog by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { showPaymentDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Preview Payment Flow")
                        }
                    }
                }
            }
        }
    }

    if (showLoginDialog) {
        TextInputDialog(
            onDismiss = { viewModel.hideLoginDialog() },
            onConfirm = { email -> viewModel.loginWithEmail(email) },
            title = "Server Login Simulation",
            label = "Email Address",
            placeholder = "e.g. tester@obscura.com",
            initialValue = "test@example.com",
            confirmText = "Login",
            dismissText = "Cancel",
            isLoading = isLoginLoading,
            keyboardType = KeyboardType.Email
        )
    }
}

@Composable
private fun TestStatusPanel(isLoggedIn: Boolean, isPremium: Boolean, coins: Int) {
    val primaryBrush = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.background(primaryBrush).padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isLoggedIn) "LOGGED IN" else "GUEST",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPremium) ObscuraIcons.Crown else ObscuraIcons.Person,
                            contentDescription = null,
                            tint = if (isPremium) Color(0xFFFFD700) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPremium) "Premium Member" else "Standard User",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("COINS", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                    Text(
                        text = "$coins",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TestSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                letterSpacing = 0.5.sp
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}
