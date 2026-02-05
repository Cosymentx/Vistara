package com.obscura.wallpapers.features.recharge

sealed class RechargePurchaseResult {
    data class Success(val message: String) : RechargePurchaseResult()
    data class Error(val message: String) : RechargePurchaseResult()
}
