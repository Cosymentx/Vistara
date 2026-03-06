package com.obscura.wallpapers.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

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
    val hazeState = remember { HazeState() }

    Scaffold(
        snackbarHost = { snackbarHost?.invoke() },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Content Layer (Bottom) - The source for blur
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
            ) {
                content(paddingValues, Modifier.fillMaxSize())
            }

            // 2. High-end Frosted Glass Header (Top) - The effect child
            if (showTopBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                tint = HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                                blurRadius = 20.dp,
                                noiseFactor = 0.1f
                            )
                        )
                ) {
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
                            Box(modifier = Modifier.statusBarsPadding())
                        }
                        headerExtra?.invoke(this)

                        // Elegant bottom line
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(0.5.dp)
//                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
//                        )
                    }
                }
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
    val hazeState = remember { HazeState() }

    Scaffold(
        topBar = {
            if (title != null) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (onBackPressed != null) {
                            IconButton(onClick = onBackPressed) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            }
                        }
                    },
                    actions = { actions?.invoke() },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .statusBarsPadding()
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = 15.dp,
                                tint = HazeTint(Color.Transparent)
                            )
                        )
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            content(paddingValues)
        }
    }
}
