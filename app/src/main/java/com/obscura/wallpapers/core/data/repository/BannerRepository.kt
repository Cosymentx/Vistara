package com.obscura.wallpapers.core.data.repository

import com.obscura.wallpapers.core.data.model.Banner
import com.obscura.wallpapers.core.data.remote.ApiResult

interface BannerRepository {
    suspend fun getHomeBanners(): ApiResult<List<Banner>>
}
