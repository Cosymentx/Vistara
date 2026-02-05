package com.obscura.wallpapers.di

import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import com.obscura.wallpapers.manager.LocaleManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 语言管理模块
 * 提供LocaleManager的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object LocaleDiModule {
    
    /**
     * 提供LocaleManager单例
     */
    @Provides
    @Singleton
    fun apiLocaleManager(userPrefsRepository: UserPrefsRepository): LocaleManager {
        println("apiLocaleManager")
        return LocaleManager(userPrefsRepository)
    }
}
