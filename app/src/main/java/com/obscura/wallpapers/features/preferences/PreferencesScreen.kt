package com.obscura.wallpapers.features.preferences

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.components.ConfirmDialog
import com.obscura.wallpapers.ui.components.LanguageSelector
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import com.obscura.wallpapers.features.preferences.PreferencesViewModel.NotificationType
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.theme.stringResource
import com.obscura.wallpapers.ui.components.GlassTopAppBar
import com.obscura.wallpapers.ui.components.applyVerticalInnerPadding
import com.obscura.wallpapers.ui.components.safeVerticalContentPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onBackPressed: () -> Unit, viewModel: PreferencesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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
    val snackbarHostState = remember { SnackbarHostState() }

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
        title = stringResource(R.string.settings),
        onBackPressed = onBackPressed
    )
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, topBar = { topBar() }) { innerPadding ->
        val safe = innerPadding.safeVerticalContentPadding(64.dp, 80.dp)
        Column(
            modifier = contentModifier
                .applyVerticalInnerPadding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    top = safe.calculateTopPadding(),
                    bottom = safe.calculateBottomPadding()
                )
        ) {
            SettingsGroup(title = stringResource(R.string.theme_settings)) {
                SettingsToggleList(
                    items = listOf(
                        SettingsToggleEntry(
                            icon = ObscuraIcons.DarkMode,
                            title = stringResource(R.string.dark_theme),
                            subtitle = stringResource(R.string.dark_theme_desc),
                            checked = darkTheme,
                            onCheckedChange = { viewModel.updateDarkTheme(it) }
                        ),
                        SettingsToggleEntry(
                            icon = ObscuraIcons.Palette,
                            title = stringResource(R.string.dynamic_colors),
                            subtitle = stringResource(R.string.dynamic_colors_desc),
                            checked = dynamicColors,
                            onCheckedChange = { viewModel.updateDynamicColors(it) }
                        )
                    )
                )
            }

            SettingsGroup(title = stringResource(R.string.language_settings)) {
                LanguageSelector(
                    currentLanguage = appLanguage,
                    onLanguageSelected = { viewModel.updateAppLanguage(it) }
                )
            }

            SettingsGroup(title = stringResource(R.string.notification_settings)) {
                SettingsToggleList(
                    items = listOf(
                        SettingsToggleEntry(
                            icon = ObscuraIcons.Notifications,
                            title = stringResource(R.string.download_notification),
                            subtitle = stringResource(R.string.download_notification_desc),
                            checked = showDownloadNotification,
                            onCheckedChange = {
                                currentNotificationType = NotificationType.DOWNLOAD
                                viewModel.updateShowDownloadNotification(it)
                            }
                        ),
                        SettingsToggleEntry(
                            icon = ObscuraIcons.Notifications,
                            title = stringResource(R.string.wallpaper_change_notification),
                            subtitle = stringResource(R.string.wallpaper_change_notification_desc),
                            checked = showWallpaperChangeNotification,
                            onCheckedChange = {
                                currentNotificationType = NotificationType.WALLPAPER_CHANGE
                                viewModel.updateShowWallpaperChangeNotification(it)
                            }
                        )
                    )
                )
            }

            SettingsGroup(title = stringResource(R.string.download_settings)) {
                SettingsToggleItem(
                    icon = ObscuraIcons.HighQuality,
                    title = stringResource(R.string.original_quality),
                    subtitle = stringResource(R.string.original_quality_desc),
                    checked = downloadOriginalQuality,
                    onCheckedChange = {
                        viewModel.updateDownloadOriginalQuality(it)
                    }
                )
                SettingsActionItem(
                    icon = ObscuraIcons.Delete,
                    title = stringResource(R.string.clear_cache),
                    subtitle = stringResource(R.string.current_cache_size, cacheSize),
                    onClick = { showClearCacheDialog = true },
                    trailingContent = if (isClearingCache) {
                        {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(24.dp)
                            )
                        }
                    } else null
                )
            }

            SettingsCategory(title = stringResource(R.string.about_app))
            SettingsActionItem(
                icon = ObscuraIcons.Version,
                title = stringResource(R.string.app_version),
                subtitle = appVersion,
                onClick = {}
            )

            if (isLoggedIn) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )

                SettingsCategory(title = stringResource(R.string.account_settings))
                SettingsActionItem(
                    icon = ObscuraIcons.ExitToApp,
                    title = stringResource(R.string.sign_out),
                    subtitle = stringResource(R.string.sign_out_desc),
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
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showClearCacheDialog) {
                ConfirmDialog(
                    onDismiss = { showClearCacheDialog = false },
                    onConfirm = {
                        viewModel.clearCache()
                        showClearCacheDialog = false
                    },
                    title = stringResource(R.string.clear_cache_title),
                    message = stringResource(R.string.clear_cache_message),
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
                    title = stringResource(R.string.sign_out_confirm_title),
                    message = stringResource(R.string.sign_out_confirm_message),
                    confirmText = stringResource(R.string.sign_out),
                    dismissText = stringResource(R.string.cancel),
                    isLoading = isLoggingOut
                )
            }
        }
    }
}

data class SettingsToggleEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String?,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

@Composable
private fun SettingsToggleList(items: List<SettingsToggleEntry>) {
    Column {
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
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    SettingsCategory(title = title)
    Column(content = content)
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
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
    Surface(modifier = modifier.fillMaxWidth(), onClick = { onCheckedChange(!checked) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
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
    Surface(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            Spacer(modifier = Modifier.width(16.dp))
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
