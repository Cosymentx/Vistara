package com.obscura.wallpapers.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object RefreshUtil {
    fun delayedEndRefreshing(
        isRefreshing: MutableStateFlow<Boolean>,
        scope: CoroutineScope,
        delayMillis: Long = 800
    ) {
        if (isRefreshing.value) {
            scope.launch {
                delay(delayMillis)
                isRefreshing.value = false
            }
        }
    }
}
