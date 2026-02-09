package com.obscura.wallpapers.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.obscura.wallpapers.billing.BillingManager
import com.obscura.wallpapers.core.data.local.DiamondDao
import com.obscura.wallpapers.core.data.local.WallpaperDao
import com.obscura.wallpapers.core.data.mapper.PexelsMapper
import com.obscura.wallpapers.core.data.mapper.PixabayMapper
import com.obscura.wallpapers.core.data.mapper.UnsplashMapper
import com.obscura.wallpapers.core.data.mapper.WallhavenMapper
import com.obscura.wallpapers.core.data.remote.ApiLoadBalancer
import com.obscura.wallpapers.core.data.remote.ApiUsageTracker
import com.obscura.wallpapers.core.data.remote.service.ApiService
import com.obscura.wallpapers.core.data.remote.service.PexelsApiService
import com.obscura.wallpapers.core.data.remote.service.PixabayApiService
import com.obscura.wallpapers.core.data.remote.service.UnsplashApiService
import com.obscura.wallpapers.core.data.remote.service.WallhavenApiService
import com.obscura.wallpapers.core.data.remote.adapter.WallpaperApiAdapter
import com.obscura.wallpapers.core.data.repository.AuthRepository
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
import com.obscura.wallpapers.settings.ThemeManager
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
object RepositoryDiModule {

    private const val PEXELS_ADAPTER = "pexelsApiAdapter"

    @Provides
    @Singleton
    fun apiWallpaperRepository(
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
        @Named(PEXELS_ADAPTER) pexelsApiAdapter: WallpaperApiAdapter,
        stringProvider: StringProvider
    ): WallpaperRepository {
        println("apiWallpaperRepository")
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
    fun apiUserPrefsRepository(
        dataStore: DataStore<Preferences>
    ): UserPrefsRepository {
        println("apiUserPrefsRepository")
        return UserPrefsRepositoryImpl(dataStore)
    }

    @Provides
    @Singleton
    fun apiUserRepository(
        dataStore: DataStore<Preferences>,
        apiService: ApiService,
        diamondRepository: dagger.Lazy<DiamondRepository>
    ): UserRepository {
        println("apiUserRepository")
        return UserRepositoryImpl(dataStore, apiService, diamondRepository)
    }

    @Provides
    @Singleton
    fun apiBannerRepository(
        networkMonitor: NetworkMonitor, stringProvider: StringProvider
    ): BannerRepository {
        println("apiBannerRepository")
        return BannerRepositoryImpl(networkMonitor, stringProvider)
    }

    @Provides
    @Singleton
    fun apiThemeManager(
        userPrefsRepository: UserPrefsRepository
    ): ThemeManager {
        println("apiThemeManager")
        return ThemeManager(userPrefsRepository)
    }

    @Provides
    @Singleton
    fun apiDiamondRepository(
        diamondDao: DiamondDao,
        authRepository: AuthRepository,
        billingManagerProvider: javax.inject.Provider<BillingManager>,
        stringProvider: StringProvider,
        apiService: ApiService
    ): DiamondRepository {
        println("apiDiamondRepository")
        return DiamondRepositoryImpl(
            diamondDao, authRepository, billingManagerProvider, stringProvider, apiService
        )
    }
}
