package com.obscura.wallpapers.di

import android.content.Context
import com.obscura.wallpapers.core.common.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilsDiModule {

    @Provides
    @Singleton
    fun apiNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        println("apiNetworkMonitor")
        return NetworkMonitor(context)
    }
}
