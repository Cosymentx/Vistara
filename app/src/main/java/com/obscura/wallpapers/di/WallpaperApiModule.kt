package com.obscura.wallpapers.di

import com.obscura.wallpapers.core.data.mapper.PexelsMapper
import com.obscura.wallpapers.core.data.mapper.UnsplashMapper
import com.obscura.wallpapers.core.data.remote.adapter.PexelsApiAdapter
import com.obscura.wallpapers.core.data.remote.service.PexelsApiService
import com.obscura.wallpapers.core.data.remote.adapter.UnsplashApiAdapter
import com.obscura.wallpapers.core.data.remote.service.UnsplashApiService
import com.obscura.wallpapers.core.data.remote.adapter.WallpaperApiAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * 壁纸API模块
 * 提供壁纸API适配器的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object WallpaperApiDiModule {

    private const val PEXELS_ADAPTER = "pexelsApiAdapter"
    private const val UNSPLASH_ADAPTER = "unsplashApiAdapter"

    /**
     * 提供Pexels API适配器
     */
    @Provides
    @Singleton
    @Named(PEXELS_ADAPTER)
    fun apiPexelsAdapter(
        pexelsApiService: PexelsApiService,
        pexelsMapper: PexelsMapper
    ): WallpaperApiAdapter {
        return PexelsApiAdapter(pexelsApiService, pexelsMapper)
    }

    /**
     * 提供Unsplash API适配器
     */
    @Provides
    @Singleton
    @Named(UNSPLASH_ADAPTER)
    fun apiUnsplashAdapter(
        unsplashApiService: UnsplashApiService,
        unsplashMapper: UnsplashMapper
    ): WallpaperApiAdapter {
        return UnsplashApiAdapter(unsplashApiService, unsplashMapper)
    }

    /**
     * 提供默认的壁纸API适配器
     * 当前使用Pexels作为默认API
     */
    @Provides
    @Singleton
    fun apiDefaultWallpaperAdapter(
        @Named(PEXELS_ADAPTER) pexelsApiAdapter: WallpaperApiAdapter
    ): WallpaperApiAdapter {
        return pexelsApiAdapter
    }
}
