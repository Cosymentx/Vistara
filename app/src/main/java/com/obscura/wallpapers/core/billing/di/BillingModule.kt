package com.obscura.wallpapers.core.billing.di

import android.content.Context
import com.obscura.wallpapers.core.billing.BillingManager
import com.obscura.wallpapers.core.billing.BillingRepository
import com.obscura.wallpapers.core.billing.FeatureAccessManager
import com.obscura.wallpapers.core.data.local.SubscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 计费模块依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object BillingModule {
    
    @Provides
    @Singleton
    fun provideBillingManager(
        @ApplicationContext context: Context
    ): BillingManager {
        return BillingManager(context)
    }
    
    @Provides
    @Singleton
    fun provideBillingRepository(
        billingManager: BillingManager,
        subscriptionDao: SubscriptionDao
    ): BillingRepository {
        return BillingRepository(billingManager, subscriptionDao)
    }
    
    @Provides
    @Singleton
    fun provideFeatureAccessManager(
        billingRepository: BillingRepository
    ): FeatureAccessManager {
        return FeatureAccessManager(billingRepository)
    }
}
