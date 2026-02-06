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
import com.obscura.wallpapers.core.data.remote.ApiResultAdapterFactory
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
 * 网络依赖注入模块（Network DI Module）
 *
 * 该模块统一管理 App 中所有网络相关的依赖，包括：
 * - Retrofit 实例
 * - OkHttpClient 配置
 * - 各类 API 鉴权拦截器
 * - 网络缓存与离线策略
 * - 接口调用频率与限流状态追踪
 *
 * 当前支持的第三方图片/视频数据源：
 * - Unsplash
 * - Pexels（图片 / 视频）
 * - Pixabay
 * - Wallhaven
 *
 * 每个数据源使用**独立的 OkHttpClient 与 Retrofit 实例**，
 * 防止鉴权、限流状态、请求头相互污染。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkDiModule {

    /** 默认自有后端 API 基础地址 */
    private const val BASE_URL = "https://api.vistaraai.xyz/"

    /** 网络超时时间（秒） */
    private const val TIMEOUT_SECONDS = 30L

    /** JSON 请求/响应的 MediaType */
    private val JSON_MEDIA_TYPE = "application/json".toMediaType()

    // Qualifier 常量（用于区分不同数据源）
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

    /**
     * 创建标准的第三方 API OkHttpClient。
     *
     * @param cache 统一磁盘缓存
     * @param loggingInterceptor 日志拦截器（仅调试环境开启）
     * @param authInterceptor 对应数据源的鉴权拦截器
     */
    private fun createApiClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .cache(cache)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * 创建通用 Retrofit 实例（使用 kotlinx-serialization 解析）。
     *
     * @param baseUrl API 根地址
     * @param json Json 序列化配置
     * @param client OkHttpClient
     */
    private fun createRetrofit(
        baseUrl: String,
        json: Json,
        client: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
        .build()

    /**
     * 创建基于 Header 的鉴权拦截器。
     *
     * 功能：
     * - 自动附加 API Key / Token
     * - 统计接口调用次数
     * - 解析限流响应头
     * - 自动进入限流冷却状态
     */
    private fun createHeaderAuthInterceptor(
        source: ApiSource,
        apiUsageTracker: ApiUsageTracker,
        addAuthHeaders: (Request.Builder) -> Unit,
        rateLimitHeaders: Pair<String, String>? = null
    ): Interceptor = Interceptor { chain ->
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
                val remaining = response.header(remainingHeader)?.toIntOrNull() ?: 0
                if (remaining <= 5) {
                    apiUsageTracker.setApiRateLimited(
                        source,
                        if (remaining <= 0) 600_000L else 60_000L
                    )
                }
            }
            apiUsageTracker.trackApiSuccess(source)
        } else {
            apiUsageTracker.trackApiError(source)
            if (response.code == 403 || response.code == 429) {
                apiUsageTracker.setApiRateLimited(source, 600_000L)
            }
        }
        response
    }

    /**
     * 创建基于 Query 参数的鉴权拦截器（如 Pixabay）。
     */
    private fun createQueryParamAuthInterceptor(
        source: ApiSource,
        apiUsageTracker: ApiUsageTracker,
        applyAuthToUrl: (HttpUrl.Builder) -> Unit
    ): Interceptor = Interceptor { chain ->
        apiUsageTracker.trackApiCall(source)

        val original = chain.request()
        val newUrl = original.url.newBuilder().apply(applyAuthToUrl).build()

        val response = chain.proceed(original.newBuilder().url(newUrl).build())

        if (response.isSuccessful) apiUsageTracker.trackApiSuccess(source)
        else apiUsageTracker.trackApiError(source)

        response
    }

    /** 提供 Gson（用于本地 JSON / Room / 非网络场景） */
    @Provides
    @Singleton
    fun apiGson(): Gson = GsonBuilder()
        .setLenient()
        .registerTypeAdapterFactory(ApiResultAdapterFactory())
        .create()

    /** 提供 kotlinx.serialization Json 实例 */
    @Provides
    @Singleton
    fun apiJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /** 提供 OkHttp 磁盘缓存 */
    @Provides
    @Singleton
    fun apiOkHttpCache(@ApplicationContext context: Context): Cache =
        Cache(context.cacheDir, 50L * 1024 * 1024)

    /** 提供 HttpLoggingInterceptor */
    @Provides
    @Singleton
    fun apiHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
        return interceptor
    }

    /** 提供各数据源的鉴权拦截器 */
    @Provides
    @Singleton
    @Named(UNSPLASH_AUTH_INTERCEPTOR)
    fun apiUnsplashAuthInterceptor(
        apiUsageTracker: ApiUsageTracker,
        apiKeyManager: ApiKeyManager
    ): Interceptor = createHeaderAuthInterceptor(
        source = ApiSource.UNSPLASH,
        apiUsageTracker = apiUsageTracker,
        addAuthHeaders = { builder ->
            builder.addHeader("Authorization", "Client-ID ${apiKeyManager.getUnsplashApiKey()}")
        },
        rateLimitHeaders = "X-Ratelimit-Limit" to "X-Ratelimit-Remaining"
    )

    @Provides
    @Singleton
    @Named(PEXELS_AUTH_INTERCEPTOR)
    fun apiPexelsAuthInterceptor(
        apiUsageTracker: ApiUsageTracker,
        apiKeyManager: ApiKeyManager
    ): Interceptor = createHeaderAuthInterceptor(
        source = ApiSource.PEXELS,
        apiUsageTracker = apiUsageTracker,
        addAuthHeaders = { builder ->
            builder.addHeader("Authorization", apiKeyManager.getPexelsApiKey())
        }
    )

    @Provides
    @Singleton
    @Named(PIXABAY_AUTH_INTERCEPTOR)
    fun apiPixabayAuthInterceptor(
        apiUsageTracker: ApiUsageTracker,
        apiKeyManager: ApiKeyManager
    ): Interceptor = createQueryParamAuthInterceptor(
        source = ApiSource.PIXABAY,
        apiUsageTracker = apiUsageTracker
    ) { urlBuilder ->
        urlBuilder.addQueryParameter("key", apiKeyManager.getPixabayApiKey())
    }

    @Provides
    @Singleton
    @Named(WALLHAVEN_AUTH_INTERCEPTOR)
    fun apiWallhavenAuthInterceptor(
        apiUsageTracker: ApiUsageTracker,
        apiKeyManager: ApiKeyManager
    ): Interceptor = createHeaderAuthInterceptor(
        source = ApiSource.WALLHAVEN,
        apiUsageTracker = apiUsageTracker,
        addAuthHeaders = { builder ->
            builder.addHeader("X-API-Key", apiKeyManager.getWallhavenApiKey())
        }
    )

    /** 提供各数据源的 OkHttpClient */
    @Provides
    @Singleton
    @Named(UNSPLASH_HTTP_CLIENT)
    fun apiUnsplashHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named(UNSPLASH_AUTH_INTERCEPTOR) authInterceptor: Interceptor
    ): OkHttpClient = createApiClient(cache, loggingInterceptor, authInterceptor)

    @Provides
    @Singleton
    @Named(PEXELS_HTTP_CLIENT)
    fun apiPexelsHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named(PEXELS_AUTH_INTERCEPTOR) authInterceptor: Interceptor
    ): OkHttpClient = createApiClient(cache, loggingInterceptor, authInterceptor)

    @Provides
    @Singleton
    @Named(PIXABAY_HTTP_CLIENT)
    fun apiPixabayHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named(PIXABAY_AUTH_INTERCEPTOR) authInterceptor: Interceptor
    ): OkHttpClient = createApiClient(cache, loggingInterceptor, authInterceptor)

    @Provides
    @Singleton
    @Named(WALLHAVEN_HTTP_CLIENT)
    fun apiWallhavenHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named(WALLHAVEN_AUTH_INTERCEPTOR) authInterceptor: Interceptor
    ): OkHttpClient = createApiClient(cache, loggingInterceptor, authInterceptor)

    /** 提供各数据源的 Retrofit */
    @Provides
    @Singleton
    @Named(UNSPLASH_RETROFIT)
    fun apiUnsplashRetrofit(
        json: Json,
        @Named(UNSPLASH_HTTP_CLIENT) client: OkHttpClient
    ): Retrofit = createRetrofit(UnsplashApiService.BASE_URL, json, client)

    @Provides
    @Singleton
    @Named(PEXELS_RETROFIT)
    fun apiPexelsRetrofit(
        json: Json,
        @Named(PEXELS_HTTP_CLIENT) client: OkHttpClient
    ): Retrofit = createRetrofit(PexelsApiService.BASE_URL, json, client)

    @Provides
    @Singleton
    @Named(PEXELS_VIDEO_RETROFIT)
    fun apiPexelsVideoRetrofit(
        json: Json,
        @Named(PEXELS_HTTP_CLIENT) client: OkHttpClient
    ): Retrofit = createRetrofit(PexelsApiService.VIDEO_BASE_URL, json, client)

    @Provides
    @Singleton
    @Named(PIXABAY_RETROFIT)
    fun apiPixabayRetrofit(
        json: Json,
        @Named(PIXABAY_HTTP_CLIENT) client: OkHttpClient
    ): Retrofit = createRetrofit(PixabayApiService.BASE_URL, json, client)

    @Provides
    @Singleton
    @Named(WALLHAVEN_RETROFIT)
    fun apiWallhavenRetrofit(
        json: Json,
        @Named(WALLHAVEN_HTTP_CLIENT) client: OkHttpClient
    ): Retrofit = createRetrofit(WallhavenApiService.BASE_URL, json, client)

    /** 提供各数据源的 ApiService */
    @Provides
    @Singleton
    fun apiUnsplashService(@Named(UNSPLASH_RETROFIT) retrofit: Retrofit): UnsplashApiService =
        retrofit.create(UnsplashApiService::class.java)

    @Provides
    @Singleton
    fun apiPixabayService(@Named(PIXABAY_RETROFIT) retrofit: Retrofit): PixabayApiService =
        retrofit.create(PixabayApiService::class.java)

    @Provides
    @Singleton
    fun apiWallhavenService(@Named(WALLHAVEN_RETROFIT) retrofit: Retrofit): WallhavenApiService =
        retrofit.create(WallhavenApiService::class.java)

    @Provides
    @Singleton
    fun apiPexelsService(
        @Named(PEXELS_RETROFIT) photosRetrofit: Retrofit,
        @Named(PEXELS_VIDEO_RETROFIT) videosRetrofit: Retrofit
    ): PexelsApiService {
        val photosService = photosRetrofit.create(PexelsApiService::class.java)
        val videosService = videosRetrofit.create(PexelsApiService::class.java)
        return object : PexelsApiService {
            override suspend fun getCuratedPhotos(page: Int, perPage: Int) =
                photosService.getCuratedPhotos(page, perPage)
            override suspend fun searchPhotos(
                query: String,
                page: Int,
                perPage: Int,
                orientation: String?,
                size: String?,
                color: String?
            ) = photosService.searchPhotos(query, page, perPage, orientation, size, color)
            override suspend fun getPhoto(id: String) = photosService.getPhoto(id)
            override suspend fun getFeaturedCollections(page: Int, perPage: Int) =
                photosService.getFeaturedCollections(page, perPage)
            override suspend fun getCollectionPhotos(id: String, page: Int, perPage: Int) =
                photosService.getCollectionPhotos(id, page, perPage)
            override suspend fun getPopularVideos(page: Int, perPage: Int) =
                videosService.getPopularVideos(page, perPage)
            override suspend fun searchVideos(
                query: String,
                page: Int,
                perPage: Int,
                orientation: String?,
                size: String?
            ) = videosService.searchVideos(query, page, perPage, orientation, size)
            override suspend fun getVideo(id: String) = videosService.getVideo(id)
        }
    }

    /** 提供自有后端 API 的 OkHttpClient（带认证） */
    @Provides
    @Singleton
    fun apiBaseHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .cache(cache)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /** 提供自有后端 API 的 Retrofit */
    @Provides
    @Singleton
    fun apiBaseRetrofit(
        json: Json,
        client: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
        .build()

    /** 提供 ApiService */
    @Provides
    @Singleton
    fun apiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}

