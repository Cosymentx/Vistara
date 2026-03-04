package com.obscura.wallpapers.di

import android.content.Context
import androidx.room.Room
import com.obscura.wallpapers.core.data.local.AppDatabase
import com.obscura.wallpapers.core.data.local.DatabaseMigrations
import com.obscura.wallpapers.core.data.local.WallpaperDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseDiModule {

    @Provides
    @Singleton
    fun apiAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        println("apiAppDatabase")
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vistara_db"
        )
        .addMigrations(
            DatabaseMigrations.MIGRATION_1_2, // 添加1到2的迁移策略
            DatabaseMigrations.MIGRATION_2_3, // 添加2到3的迁移策略
            DatabaseMigrations.MIGRATION_3_4, // 添加3到4的迁移策略
            DatabaseMigrations.MIGRATION_4_5, // 添加4到5的迁移策略
            DatabaseMigrations.MIGRATION_5_6, // 添加5到6的迁移策略
            DatabaseMigrations.MIGRATION_6_7  // 添加6到7的迁移策略（订阅表）
        )
        // 如果迁移失败，允许回退到破坏性迁移（会清除数据）
        // 注意：在生产环境中应该移除这一行，否则会导致用户数据丢失
        .fallbackToDestructiveMigration(true)
        .build()
    }

    @Provides
    @Singleton
    fun apiWallpaperDao(
        database: AppDatabase
    ): WallpaperDao {
        println("apiWallpaperDao")
        return database.wallpaperDao()
    }
    
    @Provides
    @Singleton
    fun apiSubscriptionDao(
        database: AppDatabase
    ): com.obscura.wallpapers.core.data.local.SubscriptionDao {
        return database.subscriptionDao()
    }
}
