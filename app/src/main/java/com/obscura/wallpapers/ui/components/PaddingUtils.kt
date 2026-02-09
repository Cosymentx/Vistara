package com.obscura.wallpapers.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun PaddingValues.safeVerticalContentPadding(topFixed: Dp, bottomFixed: Dp): PaddingValues {
    val topInset = calculateTopPadding()
    val bottomInset = calculateBottomPadding()
    val safeTop = if (topFixed > topInset) topFixed - topInset else 0.dp
    val safeBottom = if (bottomFixed > bottomInset) bottomFixed - bottomInset else 0.dp
    return PaddingValues(top = safeTop, bottom = safeBottom)
}

fun Modifier.applyVerticalInnerPadding(inner: PaddingValues): Modifier {
    return padding(
        top = inner.calculateTopPadding(),
        bottom = inner.calculateBottomPadding()
    )
}
