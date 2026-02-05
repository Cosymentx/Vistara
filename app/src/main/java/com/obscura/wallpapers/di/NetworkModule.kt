package com.obscura.wallpapers.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.obscura.wallpapers.BuildConfig
import com.obscura.wallpapers.core.data.remote.ApiCallHelper
import com.obscura.wallpapers.core.data.remote.ApiKeyManager
import com.obscura.wallpapers.core.data.remote.ApiSource
import com.obscura.wallpapers.core.data.remote.ApiUsageTracker
import com.obscura.wallpapers.core.data.remote.AuthInterceptor
import com.obscura.wallpapers.core.data.remote.service.ApiService
import com.obscura.wallpapers.core.data.remote.service.PexelsApiService
import com.obscura.wallpapers.core.data.remote.service.PixabayApiService
import com.obscura.wallpapers.core.data.remote.service.UnsplashApiService
import com.obscura.wallpapers.core.data.remote.service.WallhavenApiService
import com.obscura.wallpapers.core.data.repository.AuthRepository
import com.obscura.wallpapers.core.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Request
import okhttp3.HttpUrl

/**
 * 网络模块
 * 提供Retrofit和API服务的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkDiModule {

    private const val BASE_URL = "https://api.vistaraai.xyz/"
    private const val TIMEOUT_SECONDS = 30L
    private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    private const val UNSPLASH_AUTH_INTERCEPTOR = "unsplashAuthInterceptor"
    private const val UNSPLASH_HTTP_CLIENT = "unsplashHttpClient"
    private const val UNSPLASH_RETROFIT = "unsplashRetrofit"
    private const val PEXELS_AUTH_INTERCEPTOR = "pexelsAuthInterceptor"
    private const val PEXELS_HTTP_CLIENT = "pexelsHttpClient"
    private const val PEXELS_RETROFIT = "pexelsRetrofit"
    private const val PEXELS_VIDEO_RETROFIT = "pexelsVideoRetrofit"
    private const val PIXABAY_AUTH_INTERCEPTOR = "pixabayAuthInterceptor"
    private const val PIXABAY_HTTP_CLIENT = "pixabayHttpClient"
    private const val PIXABAY_RETROFIT = "pixabayRetrofit"
    private const val WALLHAVEN_AUTH_INTERCEPTOR = "wallhavenAuthInterceptor"
    private const val WALLHAVEN_HTTP_CLIENT = "wallhavenHttpClient"
    private const val WALLHAVEN_RETROFIT = "wallhavenRetrofit"
    private const val CACHE_INTERCEPTOR = "cacheInterceptor"
    private const val OFFLINE_INTERCEPTOR = "offlineInterceptor"

    private fun createApiClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private fun createRetrofit(
        baseUrl: String,
        json: kotlinx.serialization.json.Json,
        client: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
    }

    private fun createHeaderAuthInterceptor(
        source: ApiSource,
        apiUsageTracker: ApiUsageTracker,
        addAuthHeaders: (Request.Builder) -> Unit,
        rateLimitHeaders: Pair<String, String>? = null
    ): Interceptor {
        return Interceptor { chain ->
            if (apiUsageTracker.isApiRateLimited(source)) {
                return@Interceptor okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(403)
                    .message("Rate Limit Exceeded")
                    .body("".toResponseBody(null))
                    .build()
            }

            apiUsageTracker.trackApiCall(source)

            val requestBuilder = chain.request().newBuilder()
            addAuthHeaders(requestBuilder)
            val response = chain.proceed(requestBuilder.build())

            if (response.isSuccessful) {
                rateLimitHeaders?.let { (limitHeader, remainingHeader) ->
                    val rateLimit = response.header(limitHeader)?.toIntOrNull() ?: 0
                    val remaining = response.header(remainingHeader)?.toIntOrNull() ?: 0
                    if (remaining <= 5) {
                        if (remaining <= 0) {
                            apiUsageTracker.setApiRateLimited(source, 600000L)
                        } else {
                            apiUsageTracker.setApiRateLimited(source, 60000L)
                        }
                    }
                }
                apiUsageTracker.trackApiSuccess(source)
            } else {
                if (response.code == 403 || response.code == 429) {
                    apiUsageTracker.trackApiError(source)
                    apiUsageTracker.setApiRateLimited(source, 600000L)
                } else {
                    apiUsageTracker.trackApiError(source)
                }
            }
            response
        }
    }

    private fun createQueryParamAuthInterceptor(
        source: ApiSource,
        apiUsageTracker: ApiUsageTracker,
        applyAuthToUrl: (HttpUrl.Builder) -> Unit
    ): Interceptor {
        return Interceptor { chain ->
            apiUsageTracker.trackApiCall(source)

            val originalRequest = chain.request()
            val urlBuilder = originalRequest.url.newBuilder()
            applyAuthToUrl(urlBuilder)

            val response = chain.proceed(
                originalRequest.newBuilder().url(urlBuilder.build()).build()
            )

            if (response.isSuccessful) {
                apiUsageTracker.trackApiSuccess(source)
            } else {
                apiUsageTracker.trackApiError(source)
            }
            response
        }
    }

    /**
     * 提供Gson实例，用于非网络场景（Room转换等）
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .registerTypeAdapterFactory(com.obscura.wallpapers.core.data.remote.ApiResultAdapterFactory())
            .create()
    }

    /**
     * 提供Kotlinx Serialization的Json实例
     */
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
    }

    /**
     * 提供OkHttp缓存
     */
    @Provides
    @Singleton
    fun provideOkHttpCache(@ApplicationContext context: Context): Cache {
        val cacheSize = 50L * 1024L * 1024L // 50 MB
        return Cache(context.cacheDir, cacheSize)
    }

    /**
     * 提供日志拦截器
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * 提供缓存拦截器
     */
    @Provides
    @Singleton
    @Named(CACHE_INTERCEPTOR)
    fun provideCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()

            // 根据请求类型设置不同的缓存策略
            val cacheControl = when {
                // 搜索结果缓存较短时间
                url.contains("search") -> "public, max-age=300"
                // 详情页可以缓存更长时间
                url.contains("photos/") || url.contains("w/") -> "public, max-age=3600"
                // 默认缓存策略
                else -> "public, max-age=600"
            }

            val response = chain.proceed(request)
            response.newBuilder()
                .header("Cache-Control", cacheControl)
                .build()
        }
    }

    /**
     * 提供离线模式拦截器
     */
    @Provides
    @Singleton
    @Named(OFFLINE_INTERCEPTOR)
    fun provideOfflineInterceptor(@ApplicationContext context: Context): Interceptor {
        fun isNetworkAvailable(ctx: Context): Boolean {
            val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
        }
        return Interceptor { chain ->
            var request = chain.request()

            val isConnected = isNetworkAvailable(context)

            if (!isConnected) {
                // 如果无网络连接，使用缓存
                request = request.newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=2419200") // 4周
                    .build()
            }

            chain.proceed(request)
        }
    }

    /**
     * 提供基础OkHttpClient实例
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named(CACHE_INTERCEPTOR) cacheInterceptor: Interceptor,
        @Named(OFFLINE_INTERCEPTOR) offlineInterceptor: Interceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(offlineInterceptor) // 离线拦截器先执行
            .addInterceptor(authInterceptor) // 认证拦截器
            .addNetworkInterceptor(cacheInterceptor) // 网络拦截器后执行
            .build()
    }

    /**
     * 提供基础Retrofit实例，后续会为各个API服务创建特定的Service实例
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .client(okHttpClient)
            .build()
    }

    /**
     * 以下是各API服务特定的配置
     */

    // Unsplash API
    @Provides
    @Singleton
    @Named(UNSPLASH_AUTH_INTERCEPTOR)
    fun provideUnsplashAuthInterceptor(
        apiKeyManager: ApiKeyManager,
        apiUsageTracker: ApiUsageTracker
    ): Interceptor {
        return createHeaderAuthInterceptor(
            source = ApiSource.UNSPLASH,
            apiUsageTracker = apiUsageTracker,
            addAuthHeaders = { builder ->
                builder.addHeader("Authorization", "Client-ID ${apiKeyManager.getUnsplashApiKey()}")
            },
            rateLimitHeaders = "x-ratelimit-limit" to "x-ratelimit-remaining"
        )
    }

    @Provides
    @Singleton
    @Named(UNSPLASH_HTTP_CLIENT)
    fun provideUnsplashHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named(UNSPLASH_AUTH_INTERCEPTOR) authInterceptor: Interceptor
    ): OkHttpClient {
        return createApiClient(cache, loggingInterceptor, authInterceptor)
    }

    @Provides
    @Singleton
    @Named(UNSPLASH_RETROFIT)
    fun provideUnsplashRetrofit(
        json: Json,
        @Named(UNSPLASH_HTTP_CLIENT) client: OkHttpClient
    ): Retrofit {
        return createRetrofit(UnsplashApiService.BASE_URL, json, client)
    }

    @Provides
    @Singleton
    fun provideUnsplashApiService(
        @Named(UNSPLASH_RETROFIT) retrofit: Retrofit
    ): UnsplashApiService {
        return retrofit.create(UnsplashApiService::class.java)
    }

    // Pexels API
    @Provides
    @Singleton
    @Named(PEXELS_AUTH_INTERCEPTOR)
    fun providePexelsAuthInterceptor(
        apiKeyManager: ApiKeyManager,
        apiUsageTracker: ApiUsageTracker
    ): Interceptor {
        return createHeaderAuthInterceptor(
            source = ApiSource.PEXELS,
            apiUsageTracker = apiUsageTracker,
            addAuthHeaders = { builder ->
                builder.addHeader("Authorization", apiKeyManager.getPexelsApiKey())
            },
            rateLimitHeaders = "X-Ratelimit-Limit" to "X-Ratelimit-Remaining"
        )
    }

    @Provides
    @Singleton
    @Named(PEXELS_HTTP_CLIENT)
    fun providePexelsHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named(PEXELS_AUTH_INTERCEPTOR) authInterceptor: Interceptor
    ): OkHttpClient {
        return createApiClient(cache, loggingInterceptor, authInterceptor)
    }

    @Provides
    @Singleton
    @Named(PEXELS_RETROFIT)
    fun providePexelsRetrofit(
        json: Json,
        @Named(PEXELS_HTTP_CLIENT) client: OkHttpClient
    ): Retrofit {
        return createRetrofit(PexelsApiService.BASE_URL, json, client)
    }

    // 为Pexels视频API提供单独的Retrofit实例
    @Provides
    @Singleton
    @Named(PEXELS_VIDEO_RETROFIT)
    fun providePexelsVideoRetrofit(
        json: Json,
        @Named(PEXELS_HTTP_CLIENT) client: OkHttpClient
    ): Retrofit {
        return createRetrofit(PexelsApiService.VIDEO_BASE_URL, json, client)
    }

    @Provides
    @Singleton
    fun providePexelsApiService(
        @Named(PEXELS_RETROFIT) photoRetrofit: Retrofit,
        @Named(PEXELS_VIDEO_RETROFIT) videoRetrofit: Retrofit
    ): PexelsApiService {
        // 使用动态代理创建PexelsApiService实例
        // 根据方法名判断使用哪个Retrofit实例
        return object : PexelsApiService {
            private val photoService = photoRetrofit.create(PexelsApiService::class.java)
            private val videoService = videoRetrofit.create(PexelsApiService::class.java)

            // 照片相关API使用photoService
            override suspend fun getCuratedPhotos(page: Int, perPage: Int) =
                photoService.getCuratedPhotos(page, perPage)

            override suspend fun searchPhotos(
                query: String,
                page: Int,
                perPage: Int,
                orientation: String?,
                size: String?,
                color: String?
            ) =
                photoService.searchPhotos(query, page, perPage, orientation, size, color)

            override suspend fun getPhoto(id: String) =
                photoService.getPhoto(id)

            override suspend fun getFeaturedCollections(page: Int, perPage: Int) =
                photoService.getFeaturedCollections(page, perPage)

            override suspend fun getCollectionPhotos(id: String, page: Int, perPage: Int) =
                photoService.getCollectionPhotos(id, page, perPage)

            // 视频相关API使用videoService
            override suspend fun getPopularVideos(page: Int, perPage: Int) =
                videoService.getPopularVideos(page, perPage)

            override suspend fun searchVideos(
                query: String,
                page: Int,
                perPage: Int,
                orientation: String?,
                size: String?
            ) =
                videoService.searchVideos(query, page, perPage, orientation, size)

            override suspend fun getVideo(id: String) =
                videoService.getVideo(id)
        }
    }

    // Pixabay API
    @Provides
    @Singleton
    @Named(PIXABAY_AUTH_INTERCEPTOR)
    fun providePixabayAuthInterceptor(
        apiKeyManager: ApiKeyManager,
        apiUsageTracker: ApiUsageTracker
    ): Interceptor {
        return createQueryParamAuthInterceptor(
            source = ApiSource.PIXABAY,
            apiUsageTracker = apiUsageTracker
        ) { urlBuilder ->
            urlBuilder.addQueryParameter("key", apiKeyManager.getPixabayApiKey())
        }
    }

    @Provides
    @Singleton
    @Named(PIXABAY_HTTP_CLIENT)
    fun providePixabayHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named(PIXABAY_AUTH_INTERCEPTOR) authInterceptor: Interceptor
    ): OkHttpClient {
        return createApiClient(cache, loggingInterceptor, authInterceptor)
    }

    @Provides
    @Singleton
    @Named(PIXABAY_RETROFIT)
    fun providePixabayRetrofit(
        json: Json,
        @Named(PIXABAY_HTTP_CLIENT) client: OkHttpClient
    ): Retrofit {
        return createRetrofit(PixabayApiService.BASE_URL, json, client)
    }

    @Provides
    @Singleton
    fun providePixabayApiService(
        @Named(PIXABAY_RETROFIT) retrofit: Retrofit
    ): PixabayApiService {
        return retrofit.create(PixabayApiService::class.java)
    }

    // Wallhaven API
    @Provides
    @Singleton
    @Named(WALLHAVEN_AUTH_INTERCEPTOR)
    fun provideWallhavenAuthInterceptor(
        apiKeyManager: ApiKeyManager,
        apiUsageTracker: ApiUsageTracker
    ): Interceptor {
        return createQueryParamAuthInterceptor(
            source = ApiSource.WALLHAVEN,
            apiUsageTracker = apiUsageTracker
        ) { urlBuilder ->
            urlBuilder.addQueryParameter("apikey", apiKeyManager.getWallhavenApiKey())
        }
    }

    @Provides
    @Singleton
    @Named(WALLHAVEN_HTTP_CLIENT)
    fun provideWallhavenHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named(WALLHAVEN_AUTH_INTERCEPTOR) authInterceptor: Interceptor
    ): OkHttpClient {
        return createApiClient(cache, loggingInterceptor, authInterceptor)
    }

    @Provides
    @Singleton
    @Named(WALLHAVEN_RETROFIT)
    fun provideWallhavenRetrofit(
        json: Json,
        @Named(WALLHAVEN_HTTP_CLIENT) client: OkHttpClient
    ): Retrofit {
        return createRetrofit(WallhavenApiService.BASE_URL, json, client)
    }

    @Provides
    @Singleton
    fun provideWallhavenApiService(
        @Named(WALLHAVEN_RETROFIT) retrofit: Retrofit
    ): WallhavenApiService {
        return retrofit.create(WallhavenApiService::class.java)
    }

    @Provides
    @Singleton
    fun apiAuthInterceptor(
        userRepository: dagger.Lazy<UserRepository>,
        authRepository: dagger.Lazy<AuthRepository>
    ): AuthInterceptor {
        println("apiAuthInterceptor")
        return AuthInterceptor(userRepository, authRepository)
    }

    @Provides
    @Singleton
    fun apiCoreApiService(retrofit: Retrofit): ApiService {
        println("apiCoreApiService")
        return retrofit.create(ApiService::class.java)
    }

    /**
     * 提供API使用跟踪器
     */
    @Provides
    @Singleton
    fun provideApiUsageTracker(): ApiUsageTracker = ApiUsageTracker.getInstance()

    /**
     * 提供API调用帮助器
     */
    @Provides
    @Singleton
    fun provideApiCallHelper(apiUsageTracker: ApiUsageTracker): ApiCallHelper {
        return ApiCallHelper(apiUsageTracker)
    }
}
