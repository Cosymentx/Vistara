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
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * 网络模块
 * 提供Retrofit和API服务的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.vistaraai.xyz/"

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
    @Named("cacheInterceptor")
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
    @Named("offlineInterceptor")
    fun provideOfflineInterceptor(@ApplicationContext context: Context): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()

            // 检查网络连接
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            val isConnected = networkInfo != null && networkInfo.isConnected

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
        @Named("cacheInterceptor") cacheInterceptor: Interceptor,
        @Named("offlineInterceptor") offlineInterceptor: Interceptor,
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
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(okHttpClient)
            .build()
    }

    /**
     * 以下是各API服务特定的配置
     */

    // Unsplash API
    @Provides
    @Singleton
    @Named("unsplashAuthInterceptor")
    fun provideUnsplashAuthInterceptor(
        apiKeyManager: ApiKeyManager,
        apiUsageTracker: ApiUsageTracker
    ): Interceptor {
        return Interceptor { chain ->
            // 检查是否处于速率限制状态
            if (apiUsageTracker.isApiRateLimited(ApiSource.UNSPLASH)) {
                // 如果处于速率限制状态，返回403错误
                return@Interceptor okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(403)
                    .message("Rate Limit Exceeded")
                    .body(okhttp3.ResponseBody.create(null, ""))
                    .build()
            }

            // 跟踪API调用
            apiUsageTracker.trackApiCall(ApiSource.UNSPLASH)

            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Client-ID ${apiKeyManager.getUnsplashApiKey()}")
                .build()

            val response = chain.proceed(request)

            // 跟踪API响应
            if (response.isSuccessful) {
                // 检查响应头中的速率限制信息
                val rateLimit = response.header("x-ratelimit-limit")?.toIntOrNull() ?: 0
                val rateRemaining = response.header("x-ratelimit-remaining")?.toIntOrNull() ?: 0

                if (rateRemaining <= 5) { // 当剩余5个请求时就开始警告
                    // 如果剩余请求数很少，设置速率限制状态
                    android.util.Log.w(
                        "UnsplashInterceptor",
                        "API rate limit almost reached: $rateRemaining/$rateLimit remaining"
                    )

                    if (rateRemaining <= 0) {
                        // 如果剩余请求数为0，设置速率限制状态，时间较长
                        apiUsageTracker.setApiRateLimited(ApiSource.UNSPLASH, 600000L) // 10分钟
                    } else {
                        // 如果剩余请求数很少但不为0，设置短时间的速率限制
                        apiUsageTracker.setApiRateLimited(ApiSource.UNSPLASH, 60000L) // 1分钟
                    }
                }

                apiUsageTracker.trackApiSuccess(ApiSource.UNSPLASH)
            } else {
                // 检查是否是速率限制错误
                if (response.code == 403 || response.code == 429) {
                    android.util.Log.e(
                        "UnsplashInterceptor",
                        "API rate limit exceeded: ${response.code}"
                    )
                    apiUsageTracker.trackApiError(ApiSource.UNSPLASH)
                    apiUsageTracker.setApiRateLimited(ApiSource.UNSPLASH, 600000L)
                } else {
                    apiUsageTracker.trackApiError(ApiSource.UNSPLASH)
                }
            }

            response
        }
    }

    @Provides
    @Singleton
    @Named("unsplashHttpClient")
    fun provideUnsplashHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named("unsplashAuthInterceptor") authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("unsplashRetrofit")
    fun provideUnsplashRetrofit(
        json: Json,
        @Named("unsplashHttpClient") client: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(UnsplashApiService.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideUnsplashApiService(
        @Named("unsplashRetrofit") retrofit: Retrofit
    ): UnsplashApiService {
        return retrofit.create(UnsplashApiService::class.java)
    }

    // Pexels API
    @Provides
    @Singleton
    @Named("pexelsAuthInterceptor")
    fun providePexelsAuthInterceptor(
        apiKeyManager: ApiKeyManager,
        apiUsageTracker: ApiUsageTracker
    ): Interceptor {
        return Interceptor { chain ->
            // 检查是否处于速率限制状态
            if (apiUsageTracker.isApiRateLimited(ApiSource.PEXELS)) {
                // 如果处于速率限制状态，返回403错误
                return@Interceptor okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(403)
                    .message("Rate Limit Exceeded")
                    .body(okhttp3.ResponseBody.create(null, ""))
                    .build()
            }

            // 跟踪API调用
            apiUsageTracker.trackApiCall(ApiSource.PEXELS)

            val request = chain.request().newBuilder()
                .addHeader("Authorization", apiKeyManager.getPexelsApiKey())
                .build()

            val response = chain.proceed(request)

            // 跟踪API响应
            if (response.isSuccessful) {
                // 检查响应头中的速率限制信息
                val rateLimit = response.header("X-Ratelimit-Limit")?.toIntOrNull() ?: 0
                val rateRemaining = response.header("X-Ratelimit-Remaining")?.toIntOrNull() ?: 0

                if (rateRemaining <= 5) { // 当剩余5个请求时就开始警告
                    // 如果剩余请求数很少，设置速率限制状态
                    android.util.Log.w(
                        "PexelsInterceptor",
                        "API rate limit almost reached: $rateRemaining/$rateLimit remaining"
                    )

                    if (rateRemaining <= 0) {
                        // 如果剩余请求数为0，设置速率限制状态，时间较长
                        apiUsageTracker.setApiRateLimited(ApiSource.PEXELS, 600000L) // 10分钟
                    } else {
                        // 如果剩余请求数很少但不为0，设置短时间的速率限制
                        apiUsageTracker.setApiRateLimited(ApiSource.PEXELS, 60000L) // 1分钟
                    }
                }

                apiUsageTracker.trackApiSuccess(ApiSource.PEXELS)
            } else {
                // 检查是否是速率限制错误
                if (response.code == 403 || response.code == 429) {
                    android.util.Log.e(
                        "PexelsInterceptor",
                        "API rate limit exceeded: ${response.code}"
                    )
                    apiUsageTracker.trackApiError(ApiSource.PEXELS)
                    apiUsageTracker.setApiRateLimited(ApiSource.PEXELS, 600000L) // 10分钟
                } else {
                    apiUsageTracker.trackApiError(ApiSource.PEXELS)
                }
            }

            response
        }
    }

    @Provides
    @Singleton
    @Named("pexelsHttpClient")
    fun providePexelsHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named("pexelsAuthInterceptor") authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("pexelsRetrofit")
    fun providePexelsRetrofit(
        json: Json,
        @Named("pexelsHttpClient") client: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(PexelsApiService.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    // 为Pexels视频API提供单独的Retrofit实例
    @Provides
    @Singleton
    @Named("pexelsVideoRetrofit")
    fun providePexelsVideoRetrofit(
        json: Json,
        @Named("pexelsHttpClient") client: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(PexelsApiService.VIDEO_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun providePexelsApiService(
        @Named("pexelsRetrofit") photoRetrofit: Retrofit,
        @Named("pexelsVideoRetrofit") videoRetrofit: Retrofit
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
    @Named("pixabayAuthInterceptor")
    fun providePixabayAuthInterceptor(
        apiKeyManager: ApiKeyManager,
        apiUsageTracker: ApiUsageTracker
    ): Interceptor {
        return Interceptor { chain ->
            // 跟踪API调用
            apiUsageTracker.trackApiCall(ApiSource.PIXABAY)

            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            // 添加API密钥作为查询参数
            val url = originalUrl.newBuilder()
                .addQueryParameter("key", apiKeyManager.getPixabayApiKey())
                .build()

            val request = originalRequest.newBuilder()
                .url(url)
                .build()

            val response = chain.proceed(request)

            // 跟踪API响应
            if (response.isSuccessful) {
                apiUsageTracker.trackApiSuccess(ApiSource.PIXABAY)
            } else {
                apiUsageTracker.trackApiError(ApiSource.PIXABAY)
            }

            response
        }
    }

    @Provides
    @Singleton
    @Named("pixabayHttpClient")
    fun providePixabayHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named("pixabayAuthInterceptor") authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("pixabayRetrofit")
    fun providePixabayRetrofit(
        json: Json,
        @Named("pixabayHttpClient") client: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(PixabayApiService.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun providePixabayApiService(
        @Named("pixabayRetrofit") retrofit: Retrofit
    ): PixabayApiService {
        return retrofit.create(PixabayApiService::class.java)
    }

    // Wallhaven API
    @Provides
    @Singleton
    @Named("wallhavenAuthInterceptor")
    fun provideWallhavenAuthInterceptor(
        apiKeyManager: ApiKeyManager,
        apiUsageTracker: ApiUsageTracker
    ): Interceptor {
        return Interceptor { chain ->
            // 跟踪API调用
            apiUsageTracker.trackApiCall(ApiSource.WALLHAVEN)

            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            // 添加API密钥作为查询参数
            val url = originalUrl.newBuilder()
                .addQueryParameter("apikey", apiKeyManager.getWallhavenApiKey())
                .build()

            val request = originalRequest.newBuilder()
                .url(url)
                .build()

            val response = chain.proceed(request)

            // 跟踪API响应
            if (response.isSuccessful) {
                apiUsageTracker.trackApiSuccess(ApiSource.WALLHAVEN)
            } else {
                apiUsageTracker.trackApiError(ApiSource.WALLHAVEN)
            }

            response
        }
    }

    @Provides
    @Singleton
    @Named("wallhavenHttpClient")
    fun provideWallhavenHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named("wallhavenAuthInterceptor") authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("wallhavenRetrofit")
    fun provideWallhavenRetrofit(
        json: Json,
        @Named("wallhavenHttpClient") client: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(WallhavenApiService.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideWallhavenApiService(
        @Named("wallhavenRetrofit") retrofit: Retrofit
    ): WallhavenApiService {
        return retrofit.create(WallhavenApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        userRepository: dagger.Lazy<UserRepository>,
        authRepository: dagger.Lazy<AuthRepository>
    ): AuthInterceptor {
        return AuthInterceptor(userRepository, authRepository)
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
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
