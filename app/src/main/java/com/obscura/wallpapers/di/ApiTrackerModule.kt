package com.obscura.wallpapers.di

import com.obscura.wallpapers.core.data.remote.ApiUsageTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiTrackerDiModule {
    @Provides
    @Singleton
    fun apiUsageTracker(): ApiUsageTracker {
        println("apiUsageTracker")
        return ApiUsageTracker.getInstance()
    }
}
