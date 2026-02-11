package com.obscura.wallpapers.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val lightScheme = lightColorScheme(
    primary = _root_ide_package_.com.obscura.wallpapers.ui.theme.primaryLight,
    onPrimary = _root_ide_package_.com.obscura.wallpapers.ui.theme.onPrimaryLight,
    primaryContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.primaryContainerLight,
    onPrimaryContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.onPrimaryContainerLight,
    secondary = _root_ide_package_.com.obscura.wallpapers.ui.theme.secondaryLight,
    onSecondary = _root_ide_package_.com.obscura.wallpapers.ui.theme.onSecondaryLight,
    secondaryContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.secondaryContainerLight,
    onSecondaryContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.onSecondaryContainerLight,
    tertiary = _root_ide_package_.com.obscura.wallpapers.ui.theme.tertiaryLight,
    onTertiary = _root_ide_package_.com.obscura.wallpapers.ui.theme.onTertiaryLight,
    tertiaryContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.tertiaryContainerLight,
    onTertiaryContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.onTertiaryContainerLight,
    error = _root_ide_package_.com.obscura.wallpapers.ui.theme.errorLight,
    onError = _root_ide_package_.com.obscura.wallpapers.ui.theme.onErrorLight,
    errorContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.errorContainerLight,
    onErrorContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.onErrorContainerLight,
    background = _root_ide_package_.com.obscura.wallpapers.ui.theme.backgroundLight,
    onBackground = _root_ide_package_.com.obscura.wallpapers.ui.theme.onBackgroundLight,
    surface = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceLight,
    onSurface = _root_ide_package_.com.obscura.wallpapers.ui.theme.onSurfaceLight,
    surfaceVariant = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceVariantLight,
    onSurfaceVariant = _root_ide_package_.com.obscura.wallpapers.ui.theme.onSurfaceVariantLight,
    outline = _root_ide_package_.com.obscura.wallpapers.ui.theme.outlineLight,
    outlineVariant = _root_ide_package_.com.obscura.wallpapers.ui.theme.outlineVariantLight,
    scrim = _root_ide_package_.com.obscura.wallpapers.ui.theme.scrimLight,
    inverseSurface = _root_ide_package_.com.obscura.wallpapers.ui.theme.inverseSurfaceLight,
    inverseOnSurface = _root_ide_package_.com.obscura.wallpapers.ui.theme.inverseOnSurfaceLight,
    inversePrimary = _root_ide_package_.com.obscura.wallpapers.ui.theme.inversePrimaryLight,
    surfaceDim = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceDimLight,
    surfaceBright = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceBrightLight,
    surfaceContainerLowest = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceContainerLowestLight,
    surfaceContainerLow = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceContainerLowLight,
    surfaceContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceContainerLight,
    surfaceContainerHigh = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceContainerHighLight,
    surfaceContainerHighest = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = _root_ide_package_.com.obscura.wallpapers.ui.theme.primaryDark,
    onPrimary = _root_ide_package_.com.obscura.wallpapers.ui.theme.onPrimaryDark,
    primaryContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.primaryContainerDark,
    onPrimaryContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.onPrimaryContainerDark,
    secondary = _root_ide_package_.com.obscura.wallpapers.ui.theme.secondaryDark,
    onSecondary = _root_ide_package_.com.obscura.wallpapers.ui.theme.onSecondaryDark,
    secondaryContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.secondaryContainerDark,
    onSecondaryContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.onSecondaryContainerDark,
    tertiary = _root_ide_package_.com.obscura.wallpapers.ui.theme.tertiaryDark,
    onTertiary = _root_ide_package_.com.obscura.wallpapers.ui.theme.onTertiaryDark,
    tertiaryContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.tertiaryContainerDark,
    onTertiaryContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.onTertiaryContainerDark,
    error = _root_ide_package_.com.obscura.wallpapers.ui.theme.errorDark,
    onError = _root_ide_package_.com.obscura.wallpapers.ui.theme.onErrorDark,
    errorContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.errorContainerDark,
    onErrorContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.onErrorContainerDark,
    background = _root_ide_package_.com.obscura.wallpapers.ui.theme.backgroundDark,
    onBackground = _root_ide_package_.com.obscura.wallpapers.ui.theme.onBackgroundDark,
    surface = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceDark,
    onSurface = _root_ide_package_.com.obscura.wallpapers.ui.theme.onSurfaceDark,
    surfaceVariant = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceVariantDark,
    onSurfaceVariant = _root_ide_package_.com.obscura.wallpapers.ui.theme.onSurfaceVariantDark,
    outline = _root_ide_package_.com.obscura.wallpapers.ui.theme.outlineDark,
    outlineVariant = _root_ide_package_.com.obscura.wallpapers.ui.theme.outlineVariantDark,
    scrim = _root_ide_package_.com.obscura.wallpapers.ui.theme.scrimDark,
    inverseSurface = _root_ide_package_.com.obscura.wallpapers.ui.theme.inverseSurfaceDark,
    inverseOnSurface = _root_ide_package_.com.obscura.wallpapers.ui.theme.inverseOnSurfaceDark,
    inversePrimary = _root_ide_package_.com.obscura.wallpapers.ui.theme.inversePrimaryDark,
    surfaceDim = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceDimDark,
    surfaceBright = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceBrightDark,
    surfaceContainerLowest = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceContainerLowestDark,
    surfaceContainerLow = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceContainerLowDark,
    surfaceContainer = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceContainerDark,
    surfaceContainerHigh = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceContainerHighDark,
    surfaceContainerHighest = _root_ide_package_.com.obscura.wallpapers.ui.theme.surfaceContainerHighestDark,
)

/**
 * Vistara应用的主题
 *
 * @param darkTheme 是否使用深色主题
 * @param dynamicColor 是否使用动态颜色（仅Android 12+支持）
 * @param content 主题内容
 */
@Composable
fun ObscuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme
        else -> lightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}