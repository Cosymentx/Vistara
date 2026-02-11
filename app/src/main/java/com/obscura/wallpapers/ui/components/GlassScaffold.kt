package com.obscura.wallpapers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.skydoves.cloudy.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: String?,
    onBackPressed: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
): Pair<Modifier, @Composable () -> Unit> {
    val contentModifier = Modifier.fillMaxSize()

    val topBar: @Composable () -> Unit = {
        TopAppBar(
            title = { Text(title ?: "") },
            navigationIcon = {
                if (onBackPressed != null) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            },
            actions = { actions?.invoke() },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
        )
    }
    return contentModifier to topBar
}

/**
 * A scaffold that allows content to be drawn behind a blurred (translucent) header.
 */
@Composable
fun GlassHeaderScaffold(
    headerContent: @Composable BoxScope.() -> Unit,
    snackbarHost: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    var headerSize by remember { mutableStateOf(Size.Zero) }
    var headerOffset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        snackbarHost = { snackbarHost?.invoke() },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    // Apply PURE GAUSSIAN BLUR using liquidGlass with 0 distortion
                    if (headerSize.width > 0) {
                        Modifier.liquidGlass(
                            lensCenter = headerOffset,
                            lensSize = headerSize,
                            cornerRadius = 0f,
                            edge = 0f,
                            refraction = 0f,
                            curve = 0f
                        )
                    } else Modifier
                )
        ) {
            // 1. Content Layer (Bottom Layer) - Fills entire screen
            Box(modifier = Modifier.fillMaxSize()) {
                content(paddingValues)
            }

            // 2. High-end Frosted Glass Header Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { layoutCoordinates ->
                        val bounds = layoutCoordinates.boundsInRoot()
                        headerSize = bounds.size
                        headerOffset = bounds.center
                    }
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
            ) {
                headerContent()
                
                // Elegant thin light border at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabScaffold(
    title: String? = null,
    showTopBar: Boolean = false,
    onBackPressed: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    snackbarHost: (@Composable () -> Unit)? = null,
    headerExtra: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable (PaddingValues, Modifier) -> Unit
) {
    if (showTopBar) {
        GlassHeaderScaffold(
            headerContent = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (title != null) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            navigationIcon = {
                                if (onBackPressed != null) {
                                    IconButton(onClick = onBackPressed) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            actions = { actions?.invoke() },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                            modifier = Modifier.statusBarsPadding()
                        )
                    } else {
                        // If no title, still need safe area for headerExtra like Search Bar
                        Box(modifier = Modifier.statusBarsPadding())
                    }
                    headerExtra?.invoke(this)
                }
            },
            snackbarHost = snackbarHost
        ) { paddingValues ->
            content(paddingValues, Modifier.fillMaxSize())
        }
    } else {
        Scaffold(
            snackbarHost = { snackbarHost?.invoke() },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                content(paddingValues, Modifier.fillMaxSize())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassScaffold(
    title: String? = null,
    onBackPressed: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val (contentModifier, topBar) = GlassTopAppBar(
        title = title, onBackPressed = onBackPressed, actions = actions
    )
    Scaffold(
        topBar = { title?.let { topBar() } },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(modifier = contentModifier) {
            content(paddingValues)
        }
    }
}
