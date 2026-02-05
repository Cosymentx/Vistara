package com.obscura.wallpapers.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.obscura.wallpapers.billing.BillingManager
import com.obscura.wallpapers.data.local.DiamondDao
import com.obscura.wallpapers.data.local.WallpaperDao
import com.obscura.wallpapers.data.mapper.PexelsMapper
import com.obscura.wallpapers.data.mapper.PixabayMapper
import com.obscura.wallpapers.data.mapper.UnsplashMapper
import com.obscura.wallpapers.data.mapper.WallhavenMapper
import com.obscura.wallpapers.core.data.remote.ApiLoadBalancer
import com.obscura.wallpapers.core.data.remote.ApiUsageTracker
import com.obscura.wallpapers.data.remote.api.ApiService
import com.obscura.wallpapers.core.data.remote.api.PexelsApiService
import com.obscura.wallpapers.core.data.remote.api.PixabayApiService
import com.obscura.wallpapers.core.data.remote.api.UnsplashApiService
import com.obscura.wallpapers.core.data.remote.api.WallhavenApiService
import com.obscura.wallpapers.core.data.remote.api.WallpaperApiAdapter
import com.obscura.wallpapers.data.repository.AuthRepository
import com.obscura.wallpapers.core.data.repository.BannerRepository
import com.obscura.wallpapers.core.data.repository.BannerRepositoryImpl
import com.obscura.wallpapers.core.data.repository.DiamondRepository
import com.obscura.wallpapers.core.data.repository.DiamondRepositoryImpl
import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import com.obscura.wallpapers.core.data.repository.UserPrefsRepositoryImpl
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.core.data.repository.UserRepositoryImpl
import com.obscura.wallpapers.core.data.repository.WallpaperRepository
import com.obscura.wallpapers.core.data.repository.WallpaperRepositoryImpl
import com.obscura.wallpapers.manager.ThemeManager
import com.obscura.wallpapers.core.common.NetworkMonitor
import com.obscura.wallpapers.core.common.StringProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideWallpaperRepository(
        @ApplicationContext context: Context,
        unsplashApiService: UnsplashApiService,
        pexelsApiService: PexelsApiService,
        pixabayApiService: PixabayApiService,
        wallhavenApiService: WallhavenApiService,
        unsplashMapper: UnsplashMapper,
        pexelsMapper: PexelsMapper,
        pixabayMapper: PixabayMapper,
        wallhavenMapper: WallhavenMapper,
        wallpaperDao: WallpaperDao,
        apiLoadBalancer: ApiLoadBalancer,
        apiUsageTracker: ApiUsageTracker,
        networkMonitor: NetworkMonitor,
        wallpaperApiAdapter: WallpaperApiAdapter,
        @Named("pexelsApiAdapter") pexelsApiAdapter: WallpaperApiAdapter,
        stringProvider: StringProvider
    ): WallpaperRepository {
        return WallpaperRepositoryImpl(
            unsplashApiService = unsplashApiService,
            pexelsApiService = pexelsApiService,
            pixabayApiService = pixabayApiService,
            wallhavenApiService = wallhavenApiService,
            unsplashMapper = unsplashMapper,
            pexelsMapper = pexelsMapper,
            pixabayMapper = pixabayMapper,
            wallhavenMapper = wallhavenMapper,
            wallpaperDao = wallpaperDao,
            apiLoadBalancer = apiLoadBalancer,
            apiUsageTracker = apiUsageTracker,
            networkMonitor = networkMonitor,
            wallpaperApiAdapter = wallpaperApiAdapter,
            pexelsApiAdapter = pexelsApiAdapter,
            stringProvider = stringProvider,
            context = context
        )
    }

    @Provides
    @Singleton
    fun provideUserPrefsRepository(
        dataStore: DataStore<Preferences>
    ): UserPrefsRepository {
        return UserPrefsRepositoryImpl(dataStore)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        dataStore: DataStore<Preferences>,
        apiService: ApiService,
        diamondRepository: dagger.Lazy<DiamondRepository>
    ): UserRepository {
        return UserRepositoryImpl(dataStore, apiService, diamondRepository)
    }

    @Provides
    @Singleton
    fun provideBannerRepository(
        networkMonitor: NetworkMonitor, stringProvider: StringProvider
    ): BannerRepository {
        return BannerRepositoryImpl(networkMonitor, stringProvider)
    }

    @Provides
    @Singleton
    fun provideThemeManager(
        userPrefsRepository: UserPrefsRepository
    ): ThemeManager {
        return ThemeManager(userPrefsRepository)
    }

    @Provides
    @Singleton
    fun provideDiamondRepository(
        diamondDao: DiamondDao,
        authRepository: AuthRepository,
        billingManagerProvider: javax.inject.Provider<BillingManager>,
        stringProvider: StringProvider,
        apiService: ApiService
    ): DiamondRepository {
        return DiamondRepositoryImpl(
            diamondDao, authRepository, billingManagerProvider, stringProvider, apiService
        )
    }
}
