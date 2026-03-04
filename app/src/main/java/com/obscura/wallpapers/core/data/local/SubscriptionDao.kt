package com.obscura.wallpapers.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.obscura.wallpapers.core.data.local.entity.UserSubscriptionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 订阅数据访问对象
 */
@Dao
interface SubscriptionDao {
    
    /**
     * 获取订阅状态（Flow）
     */
    @Query("SELECT * FROM user_subscription WHERE id = 1")
    fun getSubscriptionStatus(): Flow<UserSubscriptionEntity?>
    
    /**
     * 获取订阅状态（一次性）
     */
    @Query("SELECT * FROM user_subscription WHERE id = 1")
    suspend fun getSubscriptionStatusOnce(): UserSubscriptionEntity?
    
    /**
     * 更新订阅状态
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSubscription(subscription: UserSubscriptionEntity)
    
    /**
     * 更新 Premium 状态
     */
    @Query("UPDATE user_subscription SET isPremium = :isPremium, lastVerified = :timestamp WHERE id = 1")
    suspend fun updatePremiumStatus(isPremium: Boolean, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 清除订阅数据
     */
    @Query("DELETE FROM user_subscription")
    suspend fun clearSubscription()
}
