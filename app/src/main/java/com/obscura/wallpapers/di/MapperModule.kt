package com.obscura.wallpapers.di

import com.obscura.wallpapers.core.data.mapper.PexelsMapper
import com.obscura.wallpapers.core.data.mapper.PixabayMapper
import com.obscura.wallpapers.core.data.mapper.UnsplashMapper
import com.obscura.wallpapers.core.data.mapper.WallhavenMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapperModule {

    @Provides
    @Singleton
    fun provideUnsplashMapper(): UnsplashMapper {
        return UnsplashMapper()
    }

    @Provides
    @Singleton
    fun providePexelsMapper(): PexelsMapper {
        return PexelsMapper()
    }

    @Provides
    @Singleton
    fun providePixabayMapper(): PixabayMapper {
        return PixabayMapper()
    }

    @Provides
    @Singleton
    fun provideWallhavenMapper(): WallhavenMapper {
        return WallhavenMapper()
    }
} 