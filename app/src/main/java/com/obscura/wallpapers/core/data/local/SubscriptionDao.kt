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
    
    @Query("SELECT * FROM user_subscription WHERE id = 1")
    fun getSubscriptionStatus(): Flow<UserSubscriptionEntity?>
    
    @Query("SELECT * FROM user_subscription WHERE id = 1")
    suspend fun getSubscriptionStatusOnce(): UserSubscriptionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSubscription(subscription: UserSubscriptionEntity)
    
    @Query("UPDATE user_subscription SET isPremium = :isPremium WHERE id = 1")
    suspend fun updatePremiumStatus(isPremium: Boolean)
    
    @Query("DELETE FROM user_subscription")
    suspend fun clearSubscription()
}
