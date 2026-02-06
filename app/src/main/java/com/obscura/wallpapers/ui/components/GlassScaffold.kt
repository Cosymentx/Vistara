package com.obscura.wallpapers.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: String,
    onBackPressed: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
): Pair<Modifier, @Composable () -> Unit> {
    val hazeState = rememberHazeState()
    val contentModifier = Modifier
        .fillMaxSize()
        .hazeSource(state = hazeState)

    val topBar: @Composable () -> Unit = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                if (onBackPressed != null) {
                    androidx.compose.material3.IconButton(onClick = onBackPressed) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
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
                .hazeEffect(
                    state = hazeState,
                    style = HazeDefaults.style(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        blurRadius = 24.dp
                    )
                )
        )
    }
    return contentModifier to topBar
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassScaffold(
    title: String,
    onBackPressed: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val (contentModifier, topBar) = GlassTopAppBar(
        title = title,
        onBackPressed = onBackPressed,
        actions = actions
    )
    Scaffold(
        topBar = { topBar() },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(modifier = contentModifier) {
            content(paddingValues)
        }
    }
}
