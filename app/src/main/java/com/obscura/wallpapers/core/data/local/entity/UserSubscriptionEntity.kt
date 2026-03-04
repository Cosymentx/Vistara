package com.obscura.wallpapers.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户订阅状态实体
 */
@Entity(tableName = "user_subscription")
data class UserSubscriptionEntity(
    @PrimaryKey val id: Int = 1,
    val isPremium: Boolean = false,
    val subscriptionType: String? = null, // monthly, yearly, lifetime
    val purchaseToken: String? = null,
    val expiryDate: Long? = null,
    val autoRenewing: Boolean = false,
    val lastVerified: Long = System.currentTimeMillis()
)
