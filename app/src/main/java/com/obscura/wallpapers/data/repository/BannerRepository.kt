package com.obscura.wallpapers.data.repository

import com.obscura.wallpapers.data.model.Banner
import com.obscura.wallpapers.data.remote.ApiResult

/**
 * Banner 仓库接口
 * 负责获取首页轮播 Banner 数据
 */
interface BannerRepository {
    /**
     * 获取首页轮播 Banner 数据
     * @return Banner 列表
     */
    suspend fun getHomeBanners(): ApiResult<List<Banner>>
}
