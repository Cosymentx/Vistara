package com.obscura.wallpapers.core.data.remote.adapter

import com.obscura.wallpapers.core.data.mapper.UnsplashMapper
import com.obscura.wallpapers.core.data.model.Collection
import com.obscura.wallpapers.core.data.model.Wallpaper
import com.obscura.wallpapers.core.data.model.unsplash.UnsplashCollection
import com.obscura.wallpapers.core.data.remote.ApiResult
import com.obscura.wallpapers.core.data.remote.ApiSource
import com.obscura.wallpapers.core.data.remote.safeApiCall
import com.obscura.wallpapers.core.data.remote.service.UnsplashApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnsplashApiAdapter @Inject constructor(
    private val unsplashApiService: UnsplashApiService,
    private val unsplashMapper: UnsplashMapper
) : WallpaperApiAdapter {
    override fun getApiSource(): ApiSource = ApiSource.UNSPLASH
    override suspend fun getFeaturedWallpapers(page: Int, pageSize: Int): ApiResult<List<Wallpaper>> {
        return safeApiCall(ApiSource.UNSPLASH) {
            val response = unsplashApiService.getPhotos(page = page, perPage = pageSize)
            unsplashMapper.toWallpapers(response)
        }
    }
    override suspend fun searchWallpapers(
        query: String,
        page: Int,
        pageSize: Int,
        filters: Map<String, String>
    ): ApiResult<List<Wallpaper>> {
        return safeApiCall(ApiSource.UNSPLASH) {
            val color = filters["color"]
            val orderBy = filters["order_by"] ?: "relevant"
            val response = unsplashApiService.searchPhotos(
                query = query,
                page = page,
                perPage = pageSize,
                orderBy = orderBy,
                color = color
            )
            unsplashMapper.toWallpapers(response.results)
        }
    }
    override suspend fun getWallpaperById(id: String): ApiResult<Wallpaper?> {
        return safeApiCall(ApiSource.UNSPLASH) {
            val photo = unsplashApiService.getPhoto(id)
            unsplashMapper.toWallpaper(photo)
        }
    }
    override suspend fun getRandomWallpapers(count: Int, category: String?): ApiResult<List<Wallpaper>> {
        return safeApiCall(ApiSource.UNSPLASH) {
            val response = unsplashApiService.getRandomPhotos(
                count = count,
                query = category
            )
            unsplashMapper.toWallpapers(response)
        }
    }
    override suspend fun getCollections(page: Int, pageSize: Int): ApiResult<List<Collection>> {
        return safeApiCall(ApiSource.UNSPLASH) {
            val response = unsplashApiService.getFeaturedCollections(page = page, perPage = pageSize)
            val collections = mutableListOf<Collection>()
            for (unsplashCollection in response) {
                collections.add(mapUnsplashCollection(unsplashCollection))
            }
            collections
        }
    }
    override suspend fun getWallpapersByCollection(
        collectionId: String,
        page: Int,
        pageSize: Int
    ): ApiResult<List<Wallpaper>> {
        return safeApiCall(ApiSource.UNSPLASH) {
            val response = unsplashApiService.getCollectionPhotos(
                id = collectionId,
                page = page,
                perPage = pageSize
            )
            unsplashMapper.toWallpapers(response)
        }
    }
    override suspend fun trackDownload(id: String): ApiResult<Unit> {
        return safeApiCall(ApiSource.UNSPLASH) {
            unsplashApiService.trackDownload(id)
            Unit
        }
    }
    private fun mapUnsplashCollection(unsplashCollection: UnsplashCollection): Collection {
        return Collection(
            id = "unsplash_${unsplashCollection.id}",
            title = unsplashCollection.title,
            description = unsplashCollection.description,
            coverUrl = unsplashCollection.coverPhoto.urls.regular,
            wallpaperCount = unsplashCollection.totalPhotos,
            isPremium = false,
            wallpapers = emptyList(),
            tags = unsplashCollection.tags?.map { it.title } ?: emptyList(),
            source = "Unsplash",
            sourceUrl = unsplashCollection.links.html,
            isFeatured = unsplashCollection.featured,
            creator = unsplashCollection.user.name
        )
    }
}
