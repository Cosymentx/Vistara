package com.obscura.wallpapers.features.preferences

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obscura.wallpapers.R
import com.obscura.wallpapers.features.preferences.PreferencesViewModel.NotificationType
import com.obscura.wallpapers.ui.components.ConfirmDialog
import com.obscura.wallpapers.ui.components.GlassTopAppBar
import com.obscura.wallpapers.ui.components.LanguageSelector
import com.obscura.wallpapers.ui.components.LocalHazeState
import com.obscura.wallpapers.ui.components.applyVerticalInnerPadding
import com.obscura.wallpapers.ui.components.safeVerticalContentPadding
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.theme.stringResource
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onBackPressed: () -> Unit,
    onNavigateToSubscription: () -> Unit = {},
    viewModel: PreferencesViewModel = hiltViewModel(),
    subscriptionViewModel: SubscriptionStatusViewModel = hiltViewModel()
) {
    val darkTheme by viewModel.darkTheme.collectAsState()
    val dynamicColors by viewModel.dynamicColors.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val showDownloadNotification by viewModel.showDownloadNotification.collectAsState()
    val showWallpaperChangeNotification by viewModel.showWallpaperChangeNotification.collectAsState()
    val downloadOriginalQuality by viewModel.downloadOriginalQuality.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val isClearingCache by viewModel.isClearingCache.collectAsState()
    val appVersion by viewModel.appVersion.collectAsState()
    val needNotificationPermission by viewModel.needNotificationPermission.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isLoggingOut by viewModel.isLoggingOut.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    val isPremium by subscriptionViewModel.isPremiumUser.collectAsState(false)
    val snackbarHostState = remember { SnackbarHostState() }

    val hazeState = remember { HazeState() }

    var currentNotificationType by remember { mutableStateOf<NotificationType?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            currentNotificationType?.let { viewModel.onNotificationPermissionGranted(it) }
        } else {
            viewModel.onNotificationPermissionDenied()
        }
        currentNotificationType = null
    }

    LaunchedEffect(needNotificationPermission) {
        if (needNotificationPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(operationResult) {
        operationResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearOperationResult()
        }
    }

    val (contentModifier, topBar) = GlassTopAppBar(
        title = stringResource(R.string.settings_title), onBackPressed = onBackPressed
    )
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { topBar() }) { innerPadding ->
        val safe = innerPadding.safeVerticalContentPadding(64.dp, 80.dp)
        androidx.compose.runtime.CompositionLocalProvider(LocalHazeState provides hazeState) {
            Column(
                modifier = contentModifier
                    .applyVerticalInnerPadding(innerPadding)
                    .hazeSource(state = hazeState)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = safe.calculateTopPadding(), bottom = safe.calculateBottomPadding()
                    )
            ) {
                SettingsGroup(title = stringResource(R.string.settings_theme_settings)) {
                    SettingsToggleList(
                        items = listOf(
                            SettingsToggleEntry(
                                icon = ObscuraIcons.DarkMode,
                                title = stringResource(R.string.settings_dark_theme),
                                subtitle = stringResource(R.string.settings_dark_theme_desc),
                                checked = darkTheme,
                                onCheckedChange = { viewModel.updateDarkTheme(it) }),
                            SettingsToggleEntry(
                                icon = ObscuraIcons.Palette,
                                title = stringResource(R.string.settings_dynamic_colors),
                                subtitle = stringResource(R.string.settings_dynamic_colors_desc),
                                checked = dynamicColors,
                                onCheckedChange = { viewModel.updateDynamicColors(it) })
                        )
                    )
                }

                SettingsGroup(title = stringResource(R.string.settings_language_settings)) {
                    LanguageSelector(
                        currentLanguage = appLanguage,
                        onLanguageSelected = { viewModel.updateAppLanguage(it) })
                }

                // 订阅状态卡片
                SettingsGroup(title = stringResource(R.string.settings_premium_subscription)) {
                    SettingsActionItem(
                        icon = Icons.Default.Star,
                        title = if (isPremium) stringResource(R.string.settings_premium_member) else stringResource(R.string.settings_upgrade_to_premium),
                        subtitle = if (isPremium) stringResource(R.string.settings_premium_member_desc) else stringResource(R.string.settings_upgrade_to_premium_desc),
                        onClick = onNavigateToSubscription,
                        iconTint = if (isPremium) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary
                    )
                }

                SettingsGroup(title = stringResource(R.string.settings_notification_settings)) {
                    SettingsToggleList(
                        items = listOf(
                            SettingsToggleEntry(
                                icon = ObscuraIcons.Notifications,
                                title = stringResource(R.string.settings_download_notification),
                                subtitle = stringResource(R.string.settings_download_notification_desc),
                                checked = showDownloadNotification,
                                onCheckedChange = {
                                    currentNotificationType = NotificationType.DOWNLOAD
                                    viewModel.updateShowDownloadNotification(it)
                                }), SettingsToggleEntry(
                                icon = ObscuraIcons.Notifications,
                                title = stringResource(R.string.settings_wallpaper_change_notification),
                                subtitle = stringResource(R.string.settings_wallpaper_change_notification_desc),
                                checked = showWallpaperChangeNotification,
                                onCheckedChange = {
                                    currentNotificationType = NotificationType.WALLPAPER_CHANGE
                                    viewModel.updateShowWallpaperChangeNotification(it)
                                })
                        )
                    )
                }

                SettingsGroup(title = stringResource(R.string.settings_download_settings)) {
                    SettingsToggleItem(
                        icon = ObscuraIcons.HighQuality,
                        title = stringResource(R.string.settings_original_quality),
                        subtitle = stringResource(R.string.settings_original_quality_desc),
                        checked = downloadOriginalQuality,
                        onCheckedChange = {
                            viewModel.updateDownloadOriginalQuality(it)
                        })
                    Spacer(Modifier.height(10.dp))
                    SettingsActionItem(
                        icon = ObscuraIcons.Delete,
                        title = stringResource(R.string.settings_clear_cache),
                        subtitle = stringResource(R.string.settings_current_cache_size, cacheSize),
                        onClick = { showClearCacheDialog = true },
                        trailingContent = if (isClearingCache) {
                            {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(24.dp)
                                )
                            }
                        } else null)
                }

                SettingsCategory(title = stringResource(R.string.settings_about_app))
                SettingsActionItem(
                    icon = ObscuraIcons.Version,
                    title = stringResource(R.string.settings_app_version),
                    subtitle = appVersion,
                    onClick = {})

                if (isLoggedIn) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = DividerDefaults.color
                    )

                    SettingsCategory(title = stringResource(R.string.settings_account_settings))
                    SettingsActionItem(
                        icon = ObscuraIcons.ExitToApp,
                        title = stringResource(R.string.settings_sign_out),
                        subtitle = stringResource(R.string.settings_sign_out_desc),
                        onClick = { showLogoutConfirmDialog = true },
                        iconTint = MaterialTheme.colorScheme.error,
                        trailingContent = if (isLoggingOut) {
                            {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(24.dp)
                                )
                            }
                        } else null)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showClearCacheDialog) {
                    ConfirmDialog(
                        onDismiss = { showClearCacheDialog = false },
                        onConfirm = {
                            viewModel.clearCache()
                            showClearCacheDialog = false
                        },
                        title = stringResource(R.string.settings_clear_cache_title),
                        message = stringResource(R.string.settings_clear_cache_message),
                        confirmText = stringResource(R.string.clear),
                        dismissText = stringResource(R.string.cancel),
                        isLoading = isClearingCache
                    )
                }

                if (showLogoutConfirmDialog) {
                    ConfirmDialog(
                        onDismiss = { showLogoutConfirmDialog = false },
                        onConfirm = {
                            viewModel.signOut()
                            showLogoutConfirmDialog = false
                        },
                        title = stringResource(R.string.settings_sign_out_confirm_title),
                        message = stringResource(R.string.settings_sign_out_confirm_message),
                        confirmText = stringResource(R.string.settings_sign_out),
                        dismissText = stringResource(R.string.cancel),
                        isLoading = isLoggingOut
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleList(items: List<SettingsToggleEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { entry ->
            SettingsToggleItem(
                icon = entry.icon,
                title = entry.title,
                subtitle = entry.subtitle,
                checked = entry.checked,
                onCheckedChange = entry.onCheckedChange
            )
        }
    }
}

@Composable
private fun SettingsCategory(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    SettingsCategory(title = title)
    Column(
        content = content,
        modifier = Modifier.padding(vertical = 5.dp),
    )
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = DividerDefaults.Thickness,
        color = DividerDefaults.color
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreferencesScreenPreview() {
    ObscuraTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            PreferencesScreen(onBackPressed = {})
        }
    }
}
