package com.obscura.wallpapers.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.obscura.wallpapers.core.data.model.Wallpaper

/**
 * 壁纸网格组件
 * 使用LazyVerticalGrid展示壁纸列表
 *
 * @param wallpapers 壁纸列表
 * @param onWallpaperClick 点击壁纸回调
 * @param columns 网格列数，默认为2
 * @param contentPadding 内容内边距
 * @param modifier 可选的修饰符
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WallpaperGrid(
    wallpapers: List<Wallpaper>,
    onWallpaperClick: (Wallpaper) -> Unit,
    columns: Int = 2,
    isPremiumUser: Boolean = false,
    purchasedIds: Set<String> = emptySet(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(wallpapers) { wallpaper ->
            WallpaperItem(
                wallpaper = wallpaper,
                onClick = { onWallpaperClick(wallpaper) },
                isPremiumUser = isPremiumUser,
                isWallpaperPurchased = purchasedIds.contains(wallpaper.id),
                modifier = Modifier.aspectRatio(0.75f), // 设置宽高比为3:4
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}
