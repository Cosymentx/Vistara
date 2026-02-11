package com.obscura.wallpapers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obscura.wallpapers.core.data.model.WallpaperCategory
import com.obscura.wallpapers.ui.theme.stringResource
import com.skydoves.cloudy.liquidGlass

/**
 * 分类选择器组件
 * 显示一个水平滚动的分类列表，支持选中状态
 */
@Composable
fun CategorySelector(
    categories: List<WallpaperCategory>,
    selectedCategory: WallpaperCategory,
    onCategorySelected: (WallpaperCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val rememberedCategories = remember(categories) { categories }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), 
        modifier = modifier.fillMaxWidth()
    ) {
        items(items = rememberedCategories, key = { it.apiValue }) { category ->
            val isSelected = category == selectedCategory
            CategoryChip(
                text = stringResource(category.titleRes),
                isSelected = isSelected,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

/**
 * 兼容旧版本的分类选择器组件
 */
@Composable
fun CategorySelector(
    categories: List<Int>,
    selectedCategory: Int,
    onCategorySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rememberedCategories = remember(categories) { categories }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), 
        modifier = modifier.fillMaxWidth()
    ) {
        items(items = rememberedCategories, key = { it }) { categoryResId ->
            val isSelected = categoryResId == selectedCategory
            CategoryChip(
                text = stringResource(categoryResId),
                isSelected = isSelected,
                onClick = { onCategorySelected(categoryResId) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var size by remember { mutableStateOf(Size.Zero) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val shape = RoundedCornerShape(20.dp)
    
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .onGloballyPositioned {
                val bounds = it.boundsInRoot()
                size = bounds.size
                offset = bounds.center
            }
            .then(
                if (isSelected && size.width > 0) {
                    Modifier.liquidGlass(
                        lensCenter = offset,
                        lensSize = size,
                        cornerRadius = 60f,
                        edge = 0.4f,
                        refraction = 0.2f,
                        curve = 0.25f,
                    )
                } else Modifier
            )
            .clip(shape)
            .background(
                if (isSelected) {
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                }
            )
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.background(
                        Color.White.copy(alpha = 0.05f)
                    )
                } else Modifier
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
            ),
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            }
        )
    }
}
