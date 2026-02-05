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
object MapperDiModule {

    @Provides
    @Singleton
    fun apiUnsplashMapper(): UnsplashMapper {
        println("apiUnsplashMapper")
        return UnsplashMapper()
    }

    @Provides
    @Singleton
    fun apiPexelsMapper(): PexelsMapper {
        println("apiPexelsMapper")
        return PexelsMapper()
    }

    @Provides
    @Singleton
    fun apiPixabayMapper(): PixabayMapper {
        println("apiPixabayMapper")
        return PixabayMapper()
    }

    @Provides
    @Singleton
    fun apiWallhavenMapper(): WallhavenMapper {
        println("apiWallhavenMapper")
        return WallhavenMapper()
    }
} 
